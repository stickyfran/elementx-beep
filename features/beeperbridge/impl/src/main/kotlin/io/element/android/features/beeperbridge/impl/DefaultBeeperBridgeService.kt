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
import dev.zacsweers.metro.SingleIn
import io.element.android.features.beeperbridge.api.BeeperBridgeService
import io.element.android.features.beeperbridge.api.BeeperLabel
import io.element.android.features.beeperbridge.api.BeeperNetwork
import io.element.android.features.beeperbridge.api.BeeperRoomData
import io.element.android.features.beeperbridge.api.DisplayNameSanitizer
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.di.annotations.SessionCoroutineScope
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.RoomId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

@ContributesBinding(SessionScope::class)
@SingleIn(SessionScope::class)
class DefaultBeeperBridgeService @Inject constructor(
    private val matrixClient: MatrixClient,
    private val bridgedDmDetector: BridgedDmDetector,
    private val matrixAccountDataService: MatrixAccountDataService,
    @SessionCoroutineScope private val sessionCoroutineScope: CoroutineScope,
) : BeeperBridgeService {
    private val cache = ConcurrentHashMap<String, BeeperRoomData>()
    private val directRoomToNetwork = ConcurrentHashMap<String, BeeperNetwork>()
    private val directRoomToUserId = ConcurrentHashMap<String, String>()

    private val _cacheUpdates = MutableSharedFlow<String>(extraBufferCapacity = 512)
    override val cacheUpdates: Flow<String> = _cacheUpdates

    init {
        sessionCoroutineScope.launch {
            loadDirectChatsMap()
        }
    }

    private suspend fun loadDirectChatsMap() {
        val result = matrixAccountDataService.getAccountData("m.direct")
        val jsonStr = result.getOrNull() ?: return
        try {
            val json = JSONObject(jsonStr)
            val keys = json.keys()
            var count = 0
            while (keys.hasNext()) {
                val userId = keys.next()
                val detectedNetwork = BeeperNetworkMap.detectNetwork(userId)
                val roomArray = json.optJSONArray(userId) ?: continue
                for (i in 0 until roomArray.length()) {
                    val roomId = roomArray.optString(i)
                    if (roomId.isNotBlank()) {
                        directRoomToUserId[roomId] = userId
                        if (detectedNetwork != null && detectedNetwork != BeeperNetwork.UNKNOWN) {
                            directRoomToNetwork[roomId] = detectedNetwork
                            cache.computeIfAbsent(roomId) {
                                BeeperRoomData(
                                    network = detectedNetwork,
                                    isFakeDm = true,
                                    networkKey = detectedNetwork.name.lowercase(),
                                    fromCache = true,
                                )
                            }
                            count++
                        }
                    }
                }
            }
            Timber.d("BeeperBridge: Loaded m.direct map with $count bridged DMs")
            _cacheUpdates.tryEmit("m.direct")
        } catch (e: Exception) {
            Timber.e(e, "BeeperBridge: Failed to parse m.direct account data")
        }
    }

    override fun isEnabled(): Boolean {
        return true // Default for now
    }

    override fun getRoomData(roomId: String): BeeperRoomData? {
        val cached = cache[roomId]
        if (cached != null) return cached
        val network = directRoomToNetwork[roomId] ?: directRoomToUserId[roomId]?.let { BeeperNetworkMap.detectNetwork(it) }
        if (network != null) {
            val roomData = BeeperRoomData(
                network = network,
                isFakeDm = true,
                networkKey = network.name.lowercase(),
                fromCache = true,
            )
            cache[roomId] = roomData
            return roomData
        }
        return null
    }

    override fun getNetworkForRoom(roomId: String): BeeperNetwork? {
        val cached = cache[roomId]?.network
        if (cached != null && cached != BeeperNetwork.UNKNOWN) {
            return cached
        }
        val fromDirect = directRoomToNetwork[roomId]
        if (fromDirect != null) {
            return fromDirect
        }
        val userId = directRoomToUserId[roomId]
        if (userId != null) {
            val net = BeeperNetworkMap.detectNetwork(userId)
            if (net != null) {
                directRoomToNetwork[roomId] = net
                return net
            }
        }
        val heuristic = BeeperNetworkMap.detectNetworkFromIdentifier(roomId)
        if (heuristic != null) return heuristic
        return cached
    }

    override fun isFakeDm(roomId: String): Boolean {
        return cache[roomId]?.isFakeDm == true || directRoomToNetwork.containsKey(roomId)
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
        val existing = cache[roomId]
        if (existing != null && existing.network != BeeperNetwork.UNKNOWN) return

        try {
            matrixClient.getRoom(RoomId(roomId))?.use { room ->
                val members = room.getMembers(limit = 10).getOrNull() ?: emptyList()
                val roomInfo = room.info()
                val roomName = roomInfo.rawName

                val membersList = if (members.isNotEmpty()) {
                    members.map { member ->
                        RoomMemberStub(
                            userId = member.userId.value,
                            isLocalUser = member.userId == matrixClient.sessionId,
                            avatarUrl = member.avatarUrl,
                            displayName = member.displayName
                        )
                    }
                } else {
                    roomInfo.heroes.map { hero ->
                        RoomMemberStub(
                            userId = hero.userId.value,
                            isLocalUser = hero.userId == matrixClient.sessionId,
                            avatarUrl = hero.avatarUrl,
                            displayName = hero.displayName
                        )
                    }
                }

                val result = bridgedDmDetector.analyze(
                    roomName = roomName ?: roomInfo.name,
                    members = membersList
                )

                Timber.d(
                    "BeeperBridge: refreshRoomData for %s - members: %d, rawName: '%s', isFakeDm: %b, network: %s",
                    roomId,
                    membersList.size,
                    roomName,
                    result.isFakeDm,
                    result.network
                )

                val rawDisplayName = if (result.isFakeDm) {
                    membersList.find { it.userId == result.contactMxid }?.displayName
                        ?: roomName
                        ?: roomInfo.name
                } else {
                    roomName ?: roomInfo.name
                }
                val contactDisplayName = DisplayNameSanitizer.sanitize(rawDisplayName).takeIf { it.isNotBlank() }
                val contactAvatarUrl = if (result.isFakeDm) membersList.find { it.userId == result.contactMxid }?.avatarUrl else null

                var detectedNetwork = result.network
                if (detectedNetwork == null) {
                    val directMember = room.getDirectRoomMember()
                    if (directMember != null) {
                        detectedNetwork = BeeperNetworkMap.detectNetwork(directMember.userId.value)
                    }
                }
                val alias = roomInfo.canonicalAlias
                if (detectedNetwork == null && alias != null) {
                    detectedNetwork = BeeperNetworkMap.detectNetworkFromIdentifier(alias.value)
                }
                val infoName = roomInfo.name
                if (detectedNetwork == null && infoName != null) {
                    detectedNetwork = BeeperNetworkMap.detectNetworkFromIdentifier(infoName)
                }
                if (detectedNetwork == null) {
                    detectedNetwork = BeeperNetworkMap.detectNetworkFromIdentifier(roomId)
                }

                val beeperData = BeeperRoomData(
                    network = detectedNetwork ?: BeeperNetwork.UNKNOWN,
                    isFakeDm = result.isFakeDm,
                    botMxid = result.botMxid,
                    realContactMxid = result.contactMxid,
                    overrideDisplayName = contactDisplayName,
                    overrideAvatarUrl = contactAvatarUrl,
                    networkKey = detectedNetwork?.name?.lowercase(),
                    fromCache = false
                )

                cache[roomId] = beeperData
                _cacheUpdates.tryEmit(roomId)
            }
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
