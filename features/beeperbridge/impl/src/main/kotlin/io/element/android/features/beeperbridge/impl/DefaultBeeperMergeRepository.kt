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
        private const val MILLIS_IN_SECOND = 1000L
    }

    init {
        sessionCoroutineScope.launch {
            try {
                val cachedJson = dataStore.data.first()[contactsKey] ?: "{}"
                val initialMap = parseContacts(cachedJson)
                updateState(initialMap)
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
        return runCatching {
            val sanitized = contact.copy(
                displayName = DisplayNameSanitizer.sanitize(contact.displayName).ifEmpty { contact.displayName }
            )
            val updated = _mergedContactsFlow.value.toMutableMap()
            updated[sanitized.id] = sanitized
            persistAndSync(updated)
        }
    }

    override suspend fun deleteMergedContact(mergeId: String): Result<Unit> {
        return runCatching {
            val updated = _mergedContactsFlow.value.toMutableMap()
            if (updated.remove(mergeId) != null) {
                persistAndSync(updated)
            }
        }
    }

    override suspend fun addRoomToMerge(mergeId: String, roomId: String): Result<Unit> {
        return runCatching {
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
        return runCatching {
            val contact = _mergedContactsFlow.value[mergeId] ?: return@runCatching
            val updatedRoomIds = contact.roomIds.filter { it != roomId }
            val updated = _mergedContactsFlow.value.toMutableMap()
            if (updatedRoomIds.isEmpty()) {
                updated.remove(mergeId)
            } else {
                updated[mergeId] = contact.copy(roomIds = updatedRoomIds)
            }
            persistAndSync(updated)
        }
    }

    override suspend fun syncFromRemote(): Result<Unit> {
        return runCatching {
            val remoteJson = matrixAccountDataService.getAccountData(ACCOUNT_DATA_KEY).getOrNull()
            if (remoteJson != null) {
                val remoteContacts = parseContacts(remoteJson)
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

    private suspend fun persistAndSync(contacts: Map<String, MergedContact>) {
        updateState(contacts)
        val jsonStr = serializeContacts(contacts)
        dataStore.edit { prefs ->
            prefs[contactsKey] = jsonStr
        }
        sessionCoroutineScope.launch {
            matrixAccountDataService.setAccountData(ACCOUNT_DATA_KEY, jsonStr)
        }
    }

    private fun parseContacts(jsonStr: String): Map<String, MergedContact> {
        val result = mutableMapOf<String, MergedContact>()
        if (jsonStr.isBlank() || jsonStr.trim() == "{}") return result
        try {
            val root = JSONObject(jsonStr)
            val keys = root.keys()
            while (keys.hasNext()) {
                val mergeId = keys.next()
                val obj = root.optJSONObject(mergeId) ?: continue

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

                val phoneContactId = obj.optString("phoneContactId").takeIf { it.isNotBlank() }
                val customWhatsAppPhone = obj.optString("customWhatsAppPhone").takeIf { it.isNotBlank() }
                val customInstagramHandle = obj.optString("customInstagramHandle").takeIf { it.isNotBlank() }
                val createdAt = obj.optLong("createdAt", System.currentTimeMillis() / MILLIS_IN_SECOND)

                result[mergeId] = MergedContact(
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
        } catch (e: Exception) {
            Timber.e(e, "BeeperMergeRepository: Error parsing merged contacts JSON")
        }
        return result
    }

    private fun serializeContacts(contacts: Map<String, MergedContact>): String {
        val root = JSONObject()
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
            root.put(mergeId, obj)
        }
        return root.toString()
    }
}
