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
