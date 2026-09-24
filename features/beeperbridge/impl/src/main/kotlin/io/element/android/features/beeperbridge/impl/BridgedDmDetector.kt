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
        var botMxid: String? = null
        var contactMxid: String? = null
        var network: BeeperNetwork? = null
        var nonBotOtherMembers = 0

        for (member in members) {
            if (member.isLocalUser) continue

            if (BeeperNetworkMap.isBeeperBot(member.userId)) {
                botMxid = member.userId
                if (network == null) {
                    network = BeeperNetworkMap.detectNetwork(member.userId)
                }
            } else {
                contactMxid = member.userId
                nonBotOtherMembers++
                if (network == null) {
                    network = BeeperNetworkMap.detectNetwork(member.userId)
                }
            }
        }

        // If network wasn't detected from non-bot members, try bot MXID
        if (network == null && botMxid != null) {
            network = BeeperNetworkMap.detectNetwork(botMxid)
        }

        // If still null, try from roomName
        if (network == null && !roomName.isNullOrBlank()) {
            network = BeeperNetworkMap.detectNetworkFromIdentifier(roomName)
        }

        val isIncomplete = botMxid != null && contactMxid == null && members.size <= 2

        // A bridged 1-to-1 DM has:
        // - exactly 1 non-bot contact
        // - active non-local members <= 2 (i.e. total members <= 3 including local user)
        // - at least one contact identified
        val isBridged1to1 = (nonBotOtherMembers == 1 && members.size <= 3 && contactMxid != null)
        val isFakeDm = (botMxid != null && contactMxid != null && members.size <= 3) ||
                       (isBridged1to1 && network != null)

        return BridgedDmResult(
            isFakeDm = isFakeDm,
            botMxid = botMxid,
            contactMxid = if (isFakeDm) contactMxid else null,
            network = network,
            isIncomplete = isIncomplete
        )
    }
}
