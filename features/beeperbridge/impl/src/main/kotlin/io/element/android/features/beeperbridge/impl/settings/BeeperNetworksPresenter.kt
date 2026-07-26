/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
package io.element.android.features.beeperbridge.impl.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.zacsweers.metro.Inject
import io.element.android.features.beeperbridge.api.BeeperNetwork
import io.element.android.libraries.architecture.Presenter

class BeeperNetworksPresenter @Inject constructor() : Presenter<BeeperNetworksState> {
    @Composable
    override fun present(): BeeperNetworksState {
        // Return a dummy list of all available Beeper networks
        val networks = remember { BeeperNetwork.entries.toList() }

        fun handleEvent(event: BeeperNetworksEvent) {
            when (event) {
                // Handle events here
            }
        }

        return BeeperNetworksState(
            networks = networks,
            eventSink = ::handleEvent,
        )
    }
}
