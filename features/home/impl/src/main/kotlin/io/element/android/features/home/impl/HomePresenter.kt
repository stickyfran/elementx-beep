/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.zacsweers.metro.Inject
import io.element.android.features.home.impl.roomlist.RoomListState
import io.element.android.features.home.impl.spaces.HomeSpacesState
import io.element.android.features.logout.api.direct.DirectLogoutState
import io.element.android.features.rageshake.api.RageshakeFeatureAvailability
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarDispatcher
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarMessage
import io.element.android.libraries.designsystem.utils.snackbar.collectSnackbarMessageAsState
import io.element.android.libraries.indicator.api.IndicatorService
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.sync.SyncService
import io.element.android.libraries.sessionstorage.api.SessionStore
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@Inject
class HomePresenter(
    private val client: MatrixClient,
    private val syncService: SyncService,
    private val snackbarDispatcher: SnackbarDispatcher,
    private val indicatorService: IndicatorService,
    private val roomListPresenter: Presenter<RoomListState>,
    private val homeSpacesPresenter: Presenter<HomeSpacesState>,
    private val logoutPresenter: Presenter<DirectLogoutState>,
    private val rageshakeFeatureAvailability: RageshakeFeatureAvailability,
    private val sessionStore: SessionStore,
    private val virtualSpacesProvider: io.element.android.features.beeperbridge.api.spaces.VirtualSpacesProvider,
    private val beeperLabelsRepository: io.element.android.features.beeperbridge.api.BeeperLabelsRepository,
    private val beeperSyncService: io.element.android.features.beeperbridge.api.BeeperSyncService,
    private val roomListDataSource: io.element.android.features.home.impl.datasource.RoomListDataSource? = null,
) : Presenter<HomeState> {
    private val currentUserWithNeighborsBuilder = CurrentUserWithNeighborsBuilder()

    @Composable
    override fun present(): HomeState {
        val coroutineState = rememberCoroutineScope()
        val matrixUser by client.userProfile.collectAsState()
        val currentUserAndNeighbors by remember {
            combine(
                client.userProfile,
                sessionStore.sessionsFlow(),
                currentUserWithNeighborsBuilder::build,
            )
        }.collectAsState(initial = persistentListOf(matrixUser))
        val isOnline by syncService.isOnline.collectAsState()
        val canReportBug by remember { rageshakeFeatureAvailability.isAvailable() }.collectAsState(false)
        val roomListState = roomListPresenter.present()
        val homeSpacesState = homeSpacesPresenter.present()
        var currentHomeNavigationBarItemOrdinal by rememberSaveable { mutableIntStateOf(HomeNavigationBarItem.Chats.ordinal) }
        val currentHomeNavigationBarItem by remember {
            derivedStateOf {
                HomeNavigationBarItem.from(currentHomeNavigationBarItemOrdinal)
            }
        }
        val selectedVirtualSpaceId by virtualSpacesProvider.getSelectedSpace().collectAsState()
        val beeperLabelsList by beeperLabelsRepository.getLabelsFlow().collectAsState(initial = emptyList())
        val beeperLabels = remember(beeperLabelsList) {
            beeperLabelsList.filter { it.isShownInInbox }.toImmutableList()
        }
        val showSpacesTab by beeperLabelsRepository.isSpacesTabVisibleFlow().collectAsState(initial = false)

        LaunchedEffect(Unit) {
            // Force a refresh of the profile
            client.getUserProfile()
        }
        // Avatar indicator
        val showAvatarIndicator by indicatorService.showRoomListTopBarIndicator()
        val directLogoutState = logoutPresenter.present()

        fun handleEvent(event: HomeEvent) {
            when (event) {
                is HomeEvent.SelectHomeNavigationBarItem -> {
                    currentHomeNavigationBarItemOrdinal = event.item.ordinal
                }
                is HomeEvent.SelectVirtualSpace -> {
                    virtualSpacesProvider.selectSpace(event.spaceId)
                    if (event.spaceId is io.element.android.features.beeperbridge.api.spaces.VirtualSpaceId.LabelSpace ||
                        event.spaceId is io.element.android.features.beeperbridge.api.spaces.VirtualSpaceId.AllChats) {
                        currentHomeNavigationBarItemOrdinal = HomeNavigationBarItem.Chats.ordinal
                    }
                }
                is HomeEvent.SetShowSpacesTab -> coroutineState.launch {
                    beeperLabelsRepository.setSpacesTabVisible(event.show)
                }
                is HomeEvent.SwitchToAccount -> coroutineState.launch {
                    sessionStore.setLatestSession(event.sessionId.value)
                }
                HomeEvent.TriggerSmartSync -> coroutineState.launch {
                    val result = beeperSyncService.syncAll(backfillDays = 60)
                    roomListDataSource?.loadAllRooms()
                    result.onSuccess {
                        snackbarDispatcher.post(
                            SnackbarMessage(io.element.android.libraries.ui.strings.CommonStrings.common_success)
                        )
                    }.onFailure {
                        snackbarDispatcher.post(
                            SnackbarMessage(io.element.android.libraries.ui.strings.CommonStrings.common_error)
                        )
                    }
                }
            }
        }

        val beeperSyncState by beeperSyncService.syncState.collectAsState()
        val snackbarMessage by snackbarDispatcher.collectSnackbarMessageAsState()
        return HomeState(
            currentUserAndNeighbors = currentUserAndNeighbors,
            showAvatarIndicator = showAvatarIndicator,
            hasNetworkConnection = isOnline,
            currentHomeNavigationBarItem = currentHomeNavigationBarItem,
            selectedVirtualSpaceId = selectedVirtualSpaceId,
            beeperLabels = beeperLabels,
            showSpacesTab = showSpacesTab,
            roomListState = roomListState,
            homeSpacesState = homeSpacesState,
            snackbarMessage = snackbarMessage,
            canReportBug = canReportBug,
            directLogoutState = directLogoutState,
            beeperSyncState = beeperSyncState,
            eventSink = ::handleEvent,
        )
    }
}
