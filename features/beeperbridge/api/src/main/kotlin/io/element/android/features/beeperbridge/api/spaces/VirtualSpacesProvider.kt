package io.element.android.features.beeperbridge.api.spaces

import io.element.android.features.home.impl.model.RoomListRoomSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface VirtualSpacesProvider {
    fun getAvailableSpaces(): Flow<List<VirtualSpaceItem>>
    fun getSelectedSpace(): StateFlow<VirtualSpaceId>
    fun selectSpace(spaceId: VirtualSpaceId)
    suspend fun filterRoomsForSpace(
        rooms: List<RoomListRoomSummary>,
        spaceId: VirtualSpaceId
    ): List<RoomListRoomSummary>
}
