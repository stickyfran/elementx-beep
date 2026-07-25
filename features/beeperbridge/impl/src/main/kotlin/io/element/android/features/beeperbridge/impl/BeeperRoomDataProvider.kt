package io.element.android.features.beeperbridge.impl

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.element.android.libraries.di.AppScope
import io.element.android.features.beeperbridge.api.BeeperBridgeService
import io.element.android.features.beeperbridge.api.BeeperLabel
import io.element.android.features.beeperbridge.api.BeeperNetwork
import io.element.android.features.beeperbridge.api.BeeperRoomData
import java.util.concurrent.ConcurrentHashMap

@ContributesBinding(AppScope::class)
class BeeperRoomDataProvider @Inject constructor(
    private val fakeDmDetector: FakeDmDetector
) : BeeperBridgeService {

    private val cache = ConcurrentHashMap<String, BeeperRoomData>()

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
        // Will be implemented when MatrixClient is injected
    }
    
    // For testing and internal updating
    fun updateCache(roomId: String, data: BeeperRoomData) {
        cache[roomId] = data
    }
}
