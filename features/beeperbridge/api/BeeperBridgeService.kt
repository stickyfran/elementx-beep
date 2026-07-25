package io.element.android.features.beeperbridge.api

interface BeeperBridgeService {
    fun getRoomData(roomId: String): BeeperRoomData?
}
