package io.element.android.features.beeperbridge.impl

import io.element.android.features.beeperbridge.api.BeeperNetwork

data class RoomMemberStub(
    val userId: String,
    val isLocalUser: Boolean
)

data class FakeDmResult(
    val isFakeDm: Boolean,
    val botMxid: String?,
    val contactMxid: String?,
    val network: BeeperNetwork?,
    val isIncomplete: Boolean = false
)

class FakeDmDetector {
    fun analyze(
        roomName: String?,
        members: List<RoomMemberStub>,
    ): FakeDmResult {
        if (!roomName.isNullOrBlank()) {
            return FakeDmResult(false, null, null, null)
        }

        if (members.size > 3) {
            return FakeDmResult(false, null, null, null)
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

        return FakeDmResult(isFakeDm, botMxid, contactMxid, network, isIncomplete)
    }
}
