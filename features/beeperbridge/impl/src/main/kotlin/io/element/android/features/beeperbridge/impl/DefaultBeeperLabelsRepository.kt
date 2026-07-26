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
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.element.android.features.beeperbridge.api.BeeperLabel
import io.element.android.features.beeperbridge.api.BeeperLabelsRepository
import io.element.android.libraries.preferences.api.store.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

@ContributesBinding(AppScope::class)
class DefaultBeeperLabelsRepository @Inject constructor(
    preferenceDataStoreFactory: PreferenceDataStoreFactory
) : BeeperLabelsRepository {
    private val dataStore = preferenceDataStoreFactory.create("beeper_labels")
    private val labelsKey = stringPreferencesKey("labels_json")
    private val hiddenNetworksKey = stringPreferencesKey("hidden_networks_json")

    override suspend fun getLabels(): List<BeeperLabel> {
        return getLabelsFlow().first()
    }

    override fun getLabelsFlow(): Flow<List<BeeperLabel>> {
        return dataStore.data.map { prefs ->
            val jsonStr = prefs[labelsKey] ?: "[]"
            parseLabels(jsonStr)
        }
    }

    override suspend fun saveLabel(label: BeeperLabel) {
        dataStore.edit { prefs ->
            val currentStr = prefs[labelsKey] ?: "[]"
            val currentLabels = parseLabels(currentStr).toMutableList()
            val index = currentLabels.indexOfFirst { it.id == label.id }
            if (index >= 0) {
                currentLabels[index] = label
            } else {
                currentLabels.add(label)
            }
            prefs[labelsKey] = serializeLabels(currentLabels)
        }
    }

    override suspend fun deleteLabel(labelId: String) {
        dataStore.edit { prefs ->
            val currentStr = prefs[labelsKey] ?: "[]"
            val currentLabels = parseLabels(currentStr).toMutableList()
            currentLabels.removeAll { it.id == labelId }
            prefs[labelsKey] = serializeLabels(currentLabels)
        }
    }

    override suspend fun getHiddenNetworks(): Set<String> {
        return dataStore.data.map { prefs ->
            val jsonStr = prefs[hiddenNetworksKey] ?: "[]"
            val array = JSONArray(jsonStr)
            val result = mutableSetOf<String>()
            for (i in 0 until array.length()) {
                result.add(array.getString(i))
            }
            result
        }.first()
    }

    override suspend fun setHiddenNetworks(networks: Set<String>) {
        dataStore.edit { prefs ->
            val array = JSONArray()
            networks.forEach { array.put(it) }
            prefs[hiddenNetworksKey] = array.toString()
        }
    }

    private fun parseLabels(jsonStr: String): List<BeeperLabel> {
        val result = mutableListOf<BeeperLabel>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val roomIdsArray = obj.getJSONArray("roomIds")
                val roomIds = mutableListOf<String>()
                for (j in 0 until roomIdsArray.length()) {
                    roomIds.add(roomIdsArray.getString(j))
                }

                result.add(
                    BeeperLabel(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        emoji = if (obj.has("emoji") && !obj.isNull("emoji")) obj.getString("emoji") else null,
                        roomIds = roomIds,
                        isShownInInbox = obj.optBoolean("isShownInInbox", true),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse Beeper labels")
        }
        return result
    }

    private fun serializeLabels(labels: List<BeeperLabel>): String {
        val array = JSONArray()
        for (label in labels) {
            val obj = JSONObject()
            obj.put("id", label.id)
            obj.put("title", label.title)
            obj.put("emoji", label.emoji)
            obj.put("isShownInInbox", label.isShownInInbox)
            obj.put("createdAt", label.createdAt)

            val roomIdsArray = JSONArray()
            label.roomIds.forEach { roomIdsArray.put(it) }
            obj.put("roomIds", roomIdsArray)

            array.put(obj)
        }
        return array.toString()
    }
}
