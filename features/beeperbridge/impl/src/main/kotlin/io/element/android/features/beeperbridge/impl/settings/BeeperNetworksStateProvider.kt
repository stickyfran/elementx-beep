/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
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
