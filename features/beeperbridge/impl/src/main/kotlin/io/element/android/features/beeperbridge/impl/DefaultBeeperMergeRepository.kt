/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
package io.element.android.features.beeperbridge.impl

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.element.android.features.beeperbridge.api.BeeperMergeRepository
import io.element.android.features.beeperbridge.api.DisplayNameSanitizer
import io.element.android.features.beeperbridge.api.MergedContact
import io.element.android.libraries.core.extensions.runCatchingExceptions
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.di.annotations.SessionCoroutineScope
import io.element.android.libraries.preferences.api.store.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

@ContributesBinding(SessionScope::class)
class DefaultBeeperMergeRepository @Inject constructor(
    private val matrixAccountDataService: MatrixAccountDataService,
    preferenceDataStoreFactory: PreferenceDataStoreFactory,
    @SessionCoroutineScope private val sessionCoroutineScope: CoroutineScope,
) : BeeperMergeRepository {
    private val dataStore = preferenceDataStoreFactory.create("beeper_merged_contacts")
    private val contactsKey = stringPreferencesKey("merged_contacts_json")

    private val _mergedContactsFlow = MutableStateFlow<Map<String, MergedContact>>(emptyMap())
    override val mergedContactsFlow: StateFlow<Map<String, MergedContact>> = _mergedContactsFlow.asStateFlow()

    private val roomToMergeId = ConcurrentHashMap<String, String>()

    companion object {
        const val ACCOUNT_DATA_KEY = "com.beeper.merged_contacts"
        const val FALLBACK_ACCOUNT_DATA_KEY = "m.fluffybeep.merges"
        private const val MILLIS_IN_SECOND = 1000L
    }

    init {
        sessionCoroutineScope.launch {
            try {
                val cachedJson = dataStore.data.first()[contactsKey] ?: "{}"
                val initialMap = parseContacts(cachedJson)
                if (initialMap.isNotEmpty()) {
                    updateState(initialMap)
                }
            } catch (e: Exception) {
                Timber.e(e, "BeeperMergeRepository: Failed to load cached merged contacts")
            }

            // Sync from remote Matrix Account Data
            syncFromRemote()
        }
    }

    private fun updateState(contacts: Map<String, MergedContact>) {
        _mergedContactsFlow.value = contacts
        roomToMergeId.clear()
        for ((mergeId, contact) in contacts) {
            for (roomId in contact.roomIds) {
                roomToMergeId[roomId] = mergeId
            }
        }
    }

    override suspend fun getMergedContacts(): Map<String, MergedContact> {
        return _mergedContactsFlow.value
    }

    override suspend fun getMergeForRoom(roomId: String): MergedContact? {
        val mergeId = roomToMergeId[roomId] ?: return null
        return _mergedContactsFlow.value[mergeId]
    }

    override suspend fun getSiblingRoomIds(roomId: String): List<String> {
        val contact = getMergeForRoom(roomId) ?: return emptyList()
        return contact.roomIds.filter { it != roomId }
    }

    override suspend fun saveMergedContact(contact: MergedContact): Result<Unit> {
        return runCatchingExceptions {
            val sanitized = contact.copy(
                displayName = DisplayNameSanitizer.sanitize(contact.displayName).ifEmpty { contact.displayName }
            )
            val updated = _mergedContactsFlow.value.toMutableMap()
            updated[sanitized.id] = sanitized
            persistAndSync(updated)
        }
    }

    override suspend fun deleteMergedContact(mergeId: String): Result<Unit> {
        return runCatchingExceptions {
            val updated = _mergedContactsFlow.value.toMutableMap()
            if (updated.remove(mergeId) != null) {
                persistAndSync(updated, deletedMergeId = mergeId)
            }
        }
    }

    override suspend fun addRoomToMerge(mergeId: String, roomId: String): Result<Unit> {
        return runCatchingExceptions {
            val contact = _mergedContactsFlow.value[mergeId] ?: error("Merge contact not found: $mergeId")
            if (!contact.roomIds.contains(roomId)) {
                val updatedContact = contact.copy(roomIds = contact.roomIds + roomId)
                val updated = _mergedContactsFlow.value.toMutableMap()
                updated[mergeId] = updatedContact
                persistAndSync(updated)
            }
        }
    }

    override suspend fun removeRoomFromMerge(mergeId: String, roomId: String): Result<Unit> {
        return runCatchingExceptions {
            val contact = _mergedContactsFlow.value[mergeId] ?: return@runCatchingExceptions
            val updatedRoomIds = contact.roomIds.filter { it != roomId }
            val updated = _mergedContactsFlow.value.toMutableMap()
            val deletedId = if (updatedRoomIds.isEmpty()) {
                updated.remove(mergeId)
                mergeId
            } else {
                updated[mergeId] = contact.copy(roomIds = updatedRoomIds)
                null
            }
            persistAndSync(updated, deletedMergeId = deletedId)
        }
    }

    override suspend fun syncFromRemote(): Result<Unit> {
        return runCatchingExceptions {
            var remoteJson = matrixAccountDataService.getAccountData(ACCOUNT_DATA_KEY).getOrNull()
            var remoteContacts = remoteJson?.let { parseContacts(it) }.orEmpty()
            if (remoteContacts.isEmpty()) {
                val fallbackJson = matrixAccountDataService.getAccountData(FALLBACK_ACCOUNT_DATA_KEY).getOrNull()
                if (fallbackJson != null) {
                    val fallbackContacts = parseContacts(fallbackJson)
                    if (fallbackContacts.isNotEmpty()) {
                        remoteContacts = fallbackContacts
                        remoteJson = fallbackJson
                    }
                }
            }
            if (remoteContacts.isNotEmpty()) {
                Timber.d("BeeperMergeRepository: Synced %d merged contacts from remote", remoteContacts.size)
                dataStore.edit { prefs ->
                    prefs[contactsKey] = serializeContacts(remoteContacts)
                }
                updateState(remoteContacts)
            }
        }.onFailure {
            Timber.e(it, "BeeperMergeRepository: Failed to sync merged contacts from remote")
        }
    }

    private suspend fun persistAndSync(contacts: Map<String, MergedContact>, deletedMergeId: String? = null) {
        var remoteContacts = matrixAccountDataService.getAccountData(ACCOUNT_DATA_KEY).getOrNull()
            ?.let { parseContacts(it) }
            .orEmpty()
        if (remoteContacts.isEmpty()) {
            val fallbackJson = matrixAccountDataService.getAccountData(FALLBACK_ACCOUNT_DATA_KEY).getOrNull()
            if (fallbackJson != null) {
                val fallbackContacts = parseContacts(fallbackJson)
                if (fallbackContacts.isNotEmpty()) {
                    remoteContacts = fallbackContacts
                }
            }
        }

        val merged = remoteContacts.toMutableMap()
        if (deletedMergeId != null) {
            merged.remove(deletedMergeId)
        }
        merged.putAll(contacts)

        updateState(merged)
        val jsonStr = serializeContacts(merged)
        dataStore.edit { prefs ->
            prefs[contactsKey] = jsonStr
        }
        matrixAccountDataService.setAccountData(ACCOUNT_DATA_KEY, jsonStr)
    }

    private fun parseContacts(jsonStr: String): Map<String, MergedContact> {
        val result = mutableMapOf<String, MergedContact>()
        if (jsonStr.isBlank() || jsonStr.trim() == "{}") return result
        try {
            val root = JSONObject(jsonStr)

            // Format 1: Beeper / FluffyBeep standard: { "contacts": { "<id>": { ... } } }
            val contactsObj = root.optJSONObject("contacts")
            if (contactsObj != null) {
                parseContactsFromObject(contactsObj, result)
            }

            // Format 2: Fallback array: { "merges": [ { "id": "...", ... } ] }
            val mergesArray = root.optJSONArray("merges")
            if (mergesArray != null && result.isEmpty()) {
                parseContactsFromArray(mergesArray, result)
            }

            // Format 3: Flat dictionary: { "<id>": { "displayName": "...", "roomIds": [...] } }
            if (result.isEmpty()) {
                parseContactsFromObject(root, result)
            }
        } catch (e: Exception) {
            Timber.e(e, "BeeperMergeRepository: Error parsing merged contacts JSON")
        }
        return result
    }

    private fun parseContactsFromObject(objMap: JSONObject, result: MutableMap<String, MergedContact>) {
        val keys = objMap.keys()
        while (keys.hasNext()) {
            val mergeId = keys.next()
            if (mergeId == "contacts" || mergeId == "merges") continue
            val obj = objMap.optJSONObject(mergeId) ?: continue
            val contact = parseSingleContact(mergeId, obj) ?: continue
            result[mergeId] = contact
        }
    }

    private fun parseContactsFromArray(arr: JSONArray, result: MutableMap<String, MergedContact>) {
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val mergeId = obj.optString("id").takeIf { it.isNotBlank() } ?: continue
            val contact = parseSingleContact(mergeId, obj) ?: continue
            result[mergeId] = contact
        }
    }

    private fun parseSingleContact(mergeId: String, obj: JSONObject): MergedContact? {
        val rawName = obj.optString("displayName", "")
        val displayName = DisplayNameSanitizer.sanitize(rawName).ifEmpty { rawName }
        val avatarMxc = obj.optString("avatarMxc").takeIf { it.isNotBlank() }

        val roomIdsList = mutableListOf<String>()
        val roomIdsArray = obj.optJSONArray("roomIds")
        if (roomIdsArray != null) {
            for (i in 0 until roomIdsArray.length()) {
                val rId = roomIdsArray.optString(i)
                if (rId.isNotBlank()) roomIdsList.add(rId)
            }
        }

        if (roomIdsList.isEmpty() && displayName.isBlank()) {
            return null
        }

        val phoneContactId = obj.optString("phoneContactId").takeIf { it.isNotBlank() }
        val customWhatsAppPhone = obj.optString("customWhatsAppPhone").takeIf { it.isNotBlank() }
        val customInstagramHandle = obj.optString("customInstagramHandle").takeIf { it.isNotBlank() }
        val createdAt = obj.optLong("createdAt", System.currentTimeMillis() / MILLIS_IN_SECOND)

        return MergedContact(
            id = mergeId,
            displayName = displayName,
            avatarMxc = avatarMxc,
            roomIds = roomIdsList,
            phoneContactId = phoneContactId,
            customWhatsAppPhone = customWhatsAppPhone,
            customInstagramHandle = customInstagramHandle,
            createdAt = createdAt,
        )
    }

    private fun serializeContacts(contacts: Map<String, MergedContact>): String {
        val root = JSONObject()
        val contactsObj = JSONObject()
        for ((mergeId, contact) in contacts) {
            val obj = JSONObject()
            obj.put("displayName", contact.displayName)
            contact.avatarMxc?.let { obj.put("avatarMxc", it) }
            val roomIdsArray = JSONArray()
            contact.roomIds.forEach { roomIdsArray.put(it) }
            obj.put("roomIds", roomIdsArray)
            contact.phoneContactId?.let { obj.put("phoneContactId", it) }
            contact.customWhatsAppPhone?.let { obj.put("customWhatsAppPhone", it) }
            contact.customInstagramHandle?.let { obj.put("customInstagramHandle", it) }
            obj.put("createdAt", contact.createdAt)
            contactsObj.put(mergeId, obj)
        }
        root.put("contacts", contactsObj)
        return root.toString()
    }
}
