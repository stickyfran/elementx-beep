/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
package io.element.android.features.beeperbridge.test

import io.element.android.features.beeperbridge.api.BeeperBridgeService
import io.element.android.features.beeperbridge.api.BeeperLabel
import io.element.android.features.beeperbridge.api.BeeperNetwork
import io.element.android.features.beeperbridge.api.BeeperRoomData

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class FakeBeeperBridgeService : BeeperBridgeService {
    var isEnabledToReturn = true
    var roomDataToReturn: BeeperRoomData? = null
    var networkToReturn: BeeperNetwork? = null
    var isFakeDmToReturn = false
    var labelsToReturn = emptyList<BeeperLabel>()
    var hiddenNetworksToReturn = emptySet<String>()

    override fun isEnabled() = isEnabledToReturn
    override fun getRoomData(roomId: String) = roomDataToReturn
    override fun getNetworkForRoom(roomId: String) = networkToReturn
    override fun isFakeDm(roomId: String) = isFakeDmToReturn
    override fun getLabels() = labelsToReturn
    override fun getHiddenNetworks() = hiddenNetworksToReturn
    override val cacheUpdates: Flow<String> = emptyFlow()
    override suspend fun invalidateCache() {}
    override suspend fun refreshRoomData(roomId: String) {}
}
