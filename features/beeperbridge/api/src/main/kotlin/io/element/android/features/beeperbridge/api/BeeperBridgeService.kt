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
