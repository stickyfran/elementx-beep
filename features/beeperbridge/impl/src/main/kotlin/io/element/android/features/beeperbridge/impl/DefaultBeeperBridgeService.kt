/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
package io.element.android.features.beeperbridge.impl

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.element.android.features.beeperbridge.api.BeeperBridgeService
import io.element.android.features.beeperbridge.api.BeeperLabel
import io.element.android.features.beeperbridge.api.BeeperNetwork
import io.element.android.features.beeperbridge.api.BeeperRoomData
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.RoomId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

@ContributesBinding(SessionScope::class)
class DefaultBeeperBridgeService @Inject constructor(
    private val matrixClient: MatrixClient,
    private val bridgedDmDetector: BridgedDmDetector,
) : BeeperBridgeService {
    private val cache = ConcurrentHashMap<String, BeeperRoomData>()
    
    private val _cacheUpdates = MutableSharedFlow<String>(extraBufferCapacity = 64)
    override val cacheUpdates: Flow<String> = _cacheUpdates

    override fun isEnabled(): Boolean {
        return true // Default for now
    }

    override fun getRoomData(roomId: String): BeeperRoomData? {
        return cache[roomId]
    }

    override fun getNetworkForRoom(roomId: String): BeeperNetwork? {
        return cache[roomId]?.network
    }

    override fun isFakeDm(roomId: String): Boolean {
        return cache[roomId]?.isFakeDm == true
    }

    override fun getLabels(): List<BeeperLabel> {
        return emptyList()
    }

    override fun getHiddenNetworks(): Set<String> {
        return emptySet()
    }

    override suspend fun invalidateCache() {
        cache.clear()
    }

    override suspend fun refreshRoomData(roomId: String) {
        if (cache.containsKey(roomId)) return
        
        try {
            val room = matrixClient.getRoom(RoomId(roomId)) ?: return
            val members = room.getMembers(limit = 10).getOrNull() ?: emptyList()
            
            // The roomName here should be the explicit m.room.name state event if it exists
            val roomName = room.name
            
            val membersList = members.map { member ->
                RoomMemberStub(
                    userId = member.userId.value,
                    isLocalUser = member.userId == matrixClient.sessionId,
                    avatarUrl = member.avatarUrl,
                    displayName = member.displayName
                )
            }
            
            val result = bridgedDmDetector.analyze(
                roomName = roomName,
                members = membersList
            )
            
            val beeperData = BeeperRoomData(
                network = result.network ?: BeeperNetwork.UNKNOWN,
                isFakeDm = result.isFakeDm,
                botMxid = result.botMxid,
                realContactMxid = result.contactMxid,
                overrideDisplayName = if (result.isFakeDm) membersList.find { it.userId == result.contactMxid }?.displayName else null,
                overrideAvatarUrl = if (result.isFakeDm) membersList.find { it.userId == result.contactMxid }?.avatarUrl else null,
                networkKey = result.network?.name?.lowercase(),
                fromCache = false
            )
            
            cache[roomId] = beeperData
            _cacheUpdates.tryEmit(roomId)
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh Beeper room data for $roomId")
        }
    }

    // For testing and internal updating
    fun updateCache(roomId: String, data: BeeperRoomData) {
        cache[roomId] = data
        _cacheUpdates.tryEmit(roomId)
    }
}
