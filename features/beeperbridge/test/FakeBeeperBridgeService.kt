package io.element.android.features.beeperbridge.test

import io.element.android.features.beeperbridge.api.BeeperBridgeService
import io.element.android.features.beeperbridge.api.BeeperRoomData

class FakeBeeperBridgeService : BeeperBridgeService {
    var roomDataToReturn: BeeperRoomData? = null

    override fun getRoomData(roomId: String): BeeperRoomData? {
        return roomDataToReturn
    }
}
