/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.spaces

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.zacsweers.metro.Inject
import io.element.android.features.beeperbridge.api.spaces.VirtualSpacesProvider
import io.element.android.libraries.architecture.Presenter
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Inject
class HomeSpacesPresenter(
    private val virtualSpacesProvider: VirtualSpacesProvider,
) : Presenter<HomeSpacesState> {
    @Composable
    override fun present(): HomeSpacesState {
        val spaces by remember {
            virtualSpacesProvider.getAvailableSpaces()
        }.collectAsState(persistentListOf())

        val selectedSpaceId by virtualSpacesProvider.getSelectedSpace().collectAsState()

        fun handleEvent(event: HomeSpacesEvents) {
            when (event) {
                is HomeSpacesEvents.SelectSpace -> {
                    virtualSpacesProvider.selectSpace(event.spaceId)
                }
            }
        }

        return HomeSpacesState(
            spaces = spaces.toImmutableList(),
            selectedSpaceId = selectedSpaceId,
            eventSink = ::handleEvent,
        )
    }
}
