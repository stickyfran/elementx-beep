/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
package io.element.android.features.beeperbridge.test

import io.element.android.features.beeperbridge.api.BeeperLabel
import io.element.android.features.beeperbridge.api.BeeperLabelsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeBeeperLabelsRepository : BeeperLabelsRepository {
    private val labelsFlow = MutableStateFlow<List<BeeperLabel>>(emptyList())
    private var hiddenNetworks = emptySet<String>()

    override suspend fun getLabels(): List<BeeperLabel> = labelsFlow.value

    override fun getLabelsFlow(): Flow<List<BeeperLabel>> = labelsFlow.asStateFlow()

    override suspend fun saveLabel(label: BeeperLabel) {
        labelsFlow.value = labelsFlow.value.filter { it.id != label.id } + label
    }

    override suspend fun deleteLabel(labelId: String) {
        labelsFlow.value = labelsFlow.value.filter { it.id != labelId }
    }

    override suspend fun getHiddenRoomIds(): Set<String> {
        val result = mutableSetOf<String>()
        for (label in labelsFlow.value) {
            if (!label.isShownInInbox) {
                result.addAll(label.roomIds)
            }
        }
        return result
    }

    override suspend fun getHiddenNetworks(): Set<String> = hiddenNetworks

    override suspend fun setHiddenNetworks(networks: Set<String>) {
        hiddenNetworks = networks
    }

    private val spacesTabVisibleFlow = MutableStateFlow(false)

    override suspend fun syncFromRemote(): Result<Unit> = Result.success(Unit)

    override fun isSpacesTabVisibleFlow(): Flow<Boolean> = spacesTabVisibleFlow.asStateFlow()

    override suspend fun setSpacesTabVisible(visible: Boolean) {
        spacesTabVisibleFlow.value = visible
    }

    fun emitLabels(labels: List<BeeperLabel>) {
        labelsFlow.value = labels
    }
}
