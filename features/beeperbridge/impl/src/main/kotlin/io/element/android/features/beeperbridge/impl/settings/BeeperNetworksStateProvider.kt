package io.element.android.features.beeperbridge.impl.settings

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.element.android.features.beeperbridge.api.BeeperNetwork

class BeeperNetworksStateProvider : PreviewParameterProvider<BeeperNetworksState> {
    override val values: Sequence<BeeperNetworksState>
        get() = sequenceOf(
            BeeperNetworksState(
                networks = BeeperNetwork.entries.toList(),
                eventSink = {}
            )
        )
}
