/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
package io.element.android.features.beeperbridge.impl.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import dev.zacsweers.metro.Inject
import io.element.android.features.beeperbridge.api.BeeperLabelsRepository
import io.element.android.features.beeperbridge.api.BeeperNetwork
import io.element.android.libraries.architecture.Presenter
import kotlinx.coroutines.launch

class BeeperNetworksPresenter @Inject constructor(
    private val beeperLabelsRepository: BeeperLabelsRepository,
) : Presenter<BeeperNetworksState> {
    @Composable
    override fun present(): BeeperNetworksState {
        val networks = remember { BeeperNetwork.entries.toList() }
        val showSpacesTab by beeperLabelsRepository.isSpacesTabVisibleFlow().collectAsState(initial = false)
        val coroutineScope = rememberCoroutineScope()

        fun handleEvent(event: BeeperNetworksEvent) {
            when (event) {
                is BeeperNetworksEvent.ToggleShowSpacesTab -> {
                    coroutineScope.launch {
                        beeperLabelsRepository.setSpacesTabVisible(event.show)
                    }
                }
            }
        }

        return BeeperNetworksState(
            networks = networks,
            showSpacesTab = showSpacesTab,
            eventSink = ::handleEvent,
        )
    }
}
