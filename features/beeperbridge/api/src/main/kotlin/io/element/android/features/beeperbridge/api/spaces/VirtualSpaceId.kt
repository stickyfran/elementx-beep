package io.element.android.features.beeperbridge.api.spaces

sealed class VirtualSpaceId {
    data object AllChats : VirtualSpaceId()
    data class NetworkSpace(val networkKey: String) : VirtualSpaceId()
    data class LabelSpace(val labelId: String) : VirtualSpaceId()
    data class TagSpace(val tag: String) : VirtualSpaceId()
    data class RealSpace(val roomId: String) : VirtualSpaceId()
}
