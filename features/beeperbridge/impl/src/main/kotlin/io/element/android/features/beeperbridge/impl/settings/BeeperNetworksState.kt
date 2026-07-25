package io.element.android.features.beeperbridge.impl.settings

import io.element.android.features.beeperbridge.api.BeeperNetwork

data class BeeperNetworksState(
    val networks: List<BeeperNetwork>,
    val eventSink: (BeeperNetworksEvent) -> Unit,
)
