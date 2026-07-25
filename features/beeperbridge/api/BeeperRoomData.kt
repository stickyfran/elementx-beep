package io.element.android.features.beeperbridge.api

data class BeeperRoomData(
    val network: BeeperNetwork,
    val isFakeDm: Boolean,
    val botMxid: String? = null,
    val realContactMxid: String? = null
)
