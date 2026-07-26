/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
package io.element.android.features.beeperbridge.api

import kotlinx.coroutines.flow.Flow

interface BeeperLabelsRepository {
    suspend fun getLabels(): List<BeeperLabel>
    fun getLabelsFlow(): Flow<List<BeeperLabel>>
    suspend fun saveLabel(label: BeeperLabel)
    suspend fun deleteLabel(labelId: String)
    suspend fun getHiddenNetworks(): Set<String>
    suspend fun setHiddenNetworks(networks: Set<String>)
}
