/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
package io.element.android.features.beeperbridge.api

interface BeeperBridgeService {
    fun isEnabled(): Boolean
    fun getRoomData(roomId: String): BeeperRoomData?
    fun getNetworkForRoom(roomId: String): BeeperNetwork?
    fun isFakeDm(roomId: String): Boolean
    fun getLabels(): List<BeeperLabel>
    fun getHiddenNetworks(): Set<String>
    suspend fun invalidateCache()
    suspend fun refreshRoomData(roomId: String)
}
