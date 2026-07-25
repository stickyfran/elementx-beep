package io.element.android.features.beeperbridge.api.spaces

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface VirtualSpacesProvider {
    fun getAvailableSpaces(): Flow<List<VirtualSpaceItem>>
    fun getSelectedSpace(): StateFlow<VirtualSpaceId>
    fun selectSpace(spaceId: VirtualSpaceId)
}
