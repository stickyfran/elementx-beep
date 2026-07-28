/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
package io.element.android.features.beeperbridge.impl

import dev.zacsweers.metro.Inject
import io.element.android.features.beeperbridge.api.BeeperNetwork

data class RoomMemberStub(
    val userId: String,
    val isLocalUser: Boolean,
    val avatarUrl: String? = null,
    val displayName: String? = null
)

data class BridgedDmResult(
    val isFakeDm: Boolean,
    val botMxid: String?,
    val contactMxid: String?,
    val network: BeeperNetwork?,
    val isIncomplete: Boolean = false
)

class BridgedDmDetector @Inject constructor() {
    fun analyze(
        roomName: String?,
        members: List<RoomMemberStub>,
    ): BridgedDmResult {
        if (!roomName.isNullOrBlank()) {
            return BridgedDmResult(false, null, null, null)
        }

        if (members.size > 3) {
            return BridgedDmResult(false, null, null, null)
        }

        var botMxid: String? = null
        var contactMxid: String? = null
        var network: BeeperNetwork? = null

        for (member in members) {
            if (member.isLocalUser) continue

            if (BeeperNetworkMap.isBeeperBot(member.userId)) {
                botMxid = member.userId
                if (network == null) {
                    network = BeeperNetworkMap.detectNetwork(member.userId)
                }
            } else {
                contactMxid = member.userId
                if (network == null) {
                    network = BeeperNetworkMap.detectNetwork(member.userId)
                }
            }
        }

        val isIncomplete = botMxid != null && contactMxid == null && members.size <= 2
        val isFakeDm = botMxid != null && contactMxid != null

        return BridgedDmResult(isFakeDm, botMxid, contactMxid, network, isIncomplete)
    }
}
