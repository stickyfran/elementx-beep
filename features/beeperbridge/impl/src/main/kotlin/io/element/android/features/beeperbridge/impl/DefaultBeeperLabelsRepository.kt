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
import io.element.android.features.beeperbridge.api.BeeperLabel
import io.element.android.features.beeperbridge.api.BeeperLabelsRepository
import io.element.android.libraries.core.extensions.runCatchingExceptions
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.di.annotations.SessionCoroutineScope
import io.element.android.libraries.preferences.api.store.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

@ContributesBinding(SessionScope::class)
class DefaultBeeperLabelsRepository @Inject constructor(
    private val matrixAccountDataService: MatrixAccountDataService,
    preferenceDataStoreFactory: PreferenceDataStoreFactory,
    @SessionCoroutineScope private val sessionCoroutineScope: CoroutineScope,
) : BeeperLabelsRepository {
    private val dataStore = preferenceDataStoreFactory.create("beeper_labels")
    private val labelsKey = stringPreferencesKey("labels_json")
    private val hiddenNetworksKey = stringPreferencesKey("hidden_networks_json")

    companion object {
        const val ACCOUNT_DATA_KEY = "com.beeper.labels"
    }

    init {
        sessionCoroutineScope.launch {
            syncFromRemote()
        }
    }

    override suspend fun getLabels(): List<BeeperLabel> {
        return getLabelsFlow().first()
    }

    override fun getLabelsFlow(): Flow<List<BeeperLabel>> {
        return dataStore.data.map { prefs ->
            val jsonStr = prefs[labelsKey] ?: "{}"
            parseLabels(jsonStr)
        }
    }

    override suspend fun saveLabel(label: BeeperLabel) {
        val currentLabels = getLabels().toMutableList()
        val index = currentLabels.indexOfFirst { it.id == label.id }
        if (index >= 0) {
            currentLabels[index] = label
        } else {
            currentLabels.add(label)
        }
        val hiddenNetworks = getHiddenNetworks()
        persistAndPush(currentLabels, hiddenNetworks)
    }

    override suspend fun deleteLabel(labelId: String) {
        val currentLabels = getLabels().toMutableList()
        currentLabels.removeAll { it.id == labelId }
        val hiddenNetworks = getHiddenNetworks()
        persistAndPush(currentLabels, hiddenNetworks)
    }

    override suspend fun getHiddenNetworks(): Set<String> {
        return dataStore.data.map { prefs ->
            val jsonStr = prefs[hiddenNetworksKey] ?: "[]"
            parseHiddenNetworksJson(jsonStr)
        }.first()
    }

    override suspend fun setHiddenNetworks(networks: Set<String>) {
        val currentLabels = getLabels()
        persistAndPush(currentLabels, networks)
    }

    override suspend fun syncFromRemote(): Result<Unit> {
        return runCatchingExceptions {
            val remoteJson = matrixAccountDataService.getAccountData(ACCOUNT_DATA_KEY).getOrNull()
            if (!remoteJson.isNullOrBlank()) {
                val parsedLabels = parseLabels(remoteJson)
                val parsedHidden = parseHiddenNetworks(remoteJson)
                dataStore.edit { prefs ->
                    prefs[labelsKey] = serializeLabels(parsedLabels)
                    val array = JSONArray()
                    parsedHidden.forEach { array.put(it) }
                    prefs[hiddenNetworksKey] = array.toString()
                }
                Timber.d("BeeperLabelsRepository: Synced %d labels from remote", parsedLabels.size)
            }
        }.onFailure {
            Timber.e(it, "BeeperLabelsRepository: Failed to sync labels from remote")
        }
    }

    private suspend fun persistAndPush(labels: List<BeeperLabel>, hiddenNetworks: Set<String>) {
        val labelsJson = serializeLabels(labels)
        val hiddenArray = JSONArray()
        hiddenNetworks.forEach { hiddenArray.put(it) }

        dataStore.edit { prefs ->
            prefs[labelsKey] = labelsJson
            prefs[hiddenNetworksKey] = hiddenArray.toString()
        }

        val fullRemoteJson = serializeFullAccountData(labels, hiddenNetworks)
        matrixAccountDataService.setAccountData(ACCOUNT_DATA_KEY, fullRemoteJson)
    }

    private fun parseLabels(jsonStr: String): List<BeeperLabel> {
        val result = mutableListOf<BeeperLabel>()
        if (jsonStr.isBlank()) return result
        try {
            val trimmed = jsonStr.trim()
            if (trimmed.startsWith("{")) {
                val root = JSONObject(trimmed)
                val keys = root.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (key == "_hidden_networks") continue
                    val obj = root.optJSONObject(key) ?: continue
                    val title = obj.optString("title").ifEmpty { obj.optString("name", "") }
                    val emoji = if (obj.has("emoji") && !obj.isNull("emoji")) obj.optString("emoji") else null
                    val isShown = obj.optBoolean("isShownInInbox", true)
                    val createdAt = obj.optLong("createdAt", System.currentTimeMillis())

                    val roomIds = mutableListOf<String>()
                    val roomsArray = obj.optJSONArray("rooms") ?: obj.optJSONArray("roomIds")
                    if (roomsArray != null) {
                        for (i in 0 until roomsArray.length()) {
                            val rId = roomsArray.optString(i)
                            if (rId.isNotBlank()) roomIds.add(rId)
                        }
                    }

                    result.add(
                        BeeperLabel(
                            id = key,
                            title = title,
                            emoji = emoji,
                            roomIds = roomIds,
                            isShownInInbox = isShown,
                            createdAt = createdAt
                        )
                    )
                }
            } else if (trimmed.startsWith("[")) {
                val array = JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val id = obj.optString("id")
                    if (id.isBlank() || id == "_hidden_networks") continue
                    val title = obj.optString("title").ifEmpty { obj.optString("name", "") }
                    val emoji = if (obj.has("emoji") && !obj.isNull("emoji")) obj.optString("emoji") else null
                    val isShown = obj.optBoolean("isShownInInbox", true)
                    val createdAt = obj.optLong("createdAt", System.currentTimeMillis())

                    val roomIds = mutableListOf<String>()
                    val roomsArray = obj.optJSONArray("rooms") ?: obj.optJSONArray("roomIds")
                    if (roomsArray != null) {
                        for (j in 0 until roomsArray.length()) {
                            val rId = roomsArray.optString(j)
                            if (rId.isNotBlank()) roomIds.add(rId)
                        }
                    }

                    result.add(
                        BeeperLabel(
                            id = id,
                            title = title,
                            emoji = emoji,
                            roomIds = roomIds,
                            isShownInInbox = isShown,
                            createdAt = createdAt
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse Beeper labels")
        }
        return result
    }

    private fun parseHiddenNetworks(jsonStr: String): Set<String> {
        val result = mutableSetOf<String>()
        try {
            val trimmed = jsonStr.trim()
            if (trimmed.startsWith("{")) {
                val root = JSONObject(trimmed)
                val hnObj = root.optJSONObject("_hidden_networks")
                if (hnObj != null) {
                    val rooms = hnObj.optJSONArray("rooms")
                    if (rooms != null) {
                        for (i in 0 until rooms.length()) {
                            val net = rooms.optString(i)
                            if (net.isNotBlank()) result.add(net)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse hidden networks from account data")
        }
        return result
    }

    private fun parseHiddenNetworksJson(jsonStr: String): Set<String> {
        val result = mutableSetOf<String>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                result.add(array.getString(i))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse hidden networks json")
        }
        return result
    }

    private fun serializeLabels(labels: List<BeeperLabel>): String {
        val root = JSONObject()
        for (label in labels) {
            val obj = JSONObject()
            obj.put("id", label.id)
            obj.put("title", label.title)
            if (label.emoji != null) obj.put("emoji", label.emoji)
            obj.put("isShownInInbox", label.isShownInInbox)
            obj.put("createdAt", label.createdAt)

            val roomsArray = JSONArray()
            label.roomIds.forEach { roomsArray.put(it) }
            obj.put("rooms", roomsArray)

            root.put(label.id, obj)
        }
        return root.toString()
    }

    private fun serializeFullAccountData(labels: List<BeeperLabel>, hiddenNetworks: Set<String>): String {
        val root = JSONObject()
        for (label in labels) {
            val obj = JSONObject()
            obj.put("id", label.id)
            obj.put("title", label.title)
            if (label.emoji != null) obj.put("emoji", label.emoji)
            obj.put("isShownInInbox", label.isShownInInbox)
            obj.put("createdAt", label.createdAt)

            val roomsArray = JSONArray()
            label.roomIds.forEach { roomsArray.put(it) }
            obj.put("rooms", roomsArray)

            root.put(label.id, obj)
        }
        if (hiddenNetworks.isNotEmpty()) {
            val hnObj = JSONObject()
            hnObj.put("title", "Hidden Networks")
            val roomsArray = JSONArray()
            hiddenNetworks.forEach { roomsArray.put(it) }
            hnObj.put("rooms", roomsArray)
            hnObj.put("isShownInInbox", false)
            root.put("_hidden_networks", hnObj)
        }
        return root.toString()
    }
}
