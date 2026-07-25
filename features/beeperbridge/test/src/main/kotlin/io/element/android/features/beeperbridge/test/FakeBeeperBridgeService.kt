package io.element.android.features.beeperbridge.test

import io.element.android.features.beeperbridge.api.BeeperBridgeService
import io.element.android.features.beeperbridge.api.BeeperLabel
import io.element.android.features.beeperbridge.api.BeeperNetwork
import io.element.android.features.beeperbridge.api.BeeperRoomData

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
    override suspend fun invalidateCache() {}
    override suspend fun refreshRoomData(roomId: String) {}
}
