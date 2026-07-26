/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
package io.element.android.features.home.impl.spaces

import com.google.common.truth.Truth.assertThat
import io.element.android.features.beeperbridge.api.spaces.VirtualSpaceId
import io.element.android.features.beeperbridge.api.spaces.VirtualSpaceItem
import io.element.android.features.beeperbridge.test.FakeVirtualSpacesProvider
import io.element.android.tests.testutils.test
import kotlinx.coroutines.test.runTest
import org.junit.Test

class HomeSpacesPresenterTest {
    @Test
    fun `present - initial state`() = runTest {
        val virtualSpacesProvider = FakeVirtualSpacesProvider()
        val spaces = listOf(
            VirtualSpaceItem(VirtualSpaceId.AllChats, "All Chats")
        )
        virtualSpacesProvider.emitAvailableSpaces(spaces)

        val presenter = HomeSpacesPresenter(
            virtualSpacesProvider = virtualSpacesProvider
        )
        presenter.test {
            val state = awaitItem()
            assertThat(state.spaces).isEqualTo(spaces)
            assertThat(state.selectedSpaceId).isEqualTo(VirtualSpaceId.AllChats)
        }
    }

    @Test
    fun `present - select space event`() = runTest {
        val virtualSpacesProvider = FakeVirtualSpacesProvider()
        val presenter = HomeSpacesPresenter(
            virtualSpacesProvider = virtualSpacesProvider
        )
        presenter.test {
            val state = awaitItem()
            state.eventSink(HomeSpacesEvents.SelectSpace(VirtualSpaceId.NetworkSpace("whatsapp")))
            val updatedState = awaitItem()
            assertThat(updatedState.selectedSpaceId).isEqualTo(VirtualSpaceId.NetworkSpace("whatsapp"))
        }
    }
}
