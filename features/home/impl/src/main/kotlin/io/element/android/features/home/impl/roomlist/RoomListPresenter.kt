/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.roomlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import dev.zacsweers.metro.Inject
import im.vector.app.features.analytics.plan.Interaction
import io.element.android.features.announcement.api.Announcement
import io.element.android.features.announcement.api.AnnouncementService
import io.element.android.features.beeperbridge.api.BeeperLabelsRepository
import io.element.android.features.beeperbridge.api.BeeperMergeRepository
import io.element.android.features.beeperbridge.api.MergedContact
import io.element.android.features.beeperbridge.api.spaces.VirtualSpacesProvider
import io.element.android.features.home.impl.datasource.RoomListDataSource
import io.element.android.features.home.impl.filters.RoomListFiltersState
import io.element.android.features.home.impl.filters.into
import io.element.android.features.home.impl.model.RoomListRoomSummary
import io.element.android.features.home.impl.search.RoomListSearchEvent
import io.element.android.features.home.impl.search.RoomListSearchState
import io.element.android.features.home.impl.spacefilters.SpaceFiltersState
import io.element.android.features.home.impl.spacefilters.into
import io.element.android.features.home.impl.spacefilters.selectedFilter
import io.element.android.features.invite.api.SeenInvitesStore
import io.element.android.features.invite.api.acceptdecline.AcceptDeclineInviteEvents.AcceptInvite
import io.element.android.features.invite.api.acceptdecline.AcceptDeclineInviteEvents.DeclineInvite
import io.element.android.features.invite.api.acceptdecline.AcceptDeclineInviteState
import io.element.android.features.leaveroom.api.LeaveRoomEvent
import io.element.android.features.leaveroom.api.LeaveRoomState
import io.element.android.features.preferences.impl.tasks.MarkRoomAsRead
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.featureflag.api.FeatureFlagService
import io.element.android.libraries.featureflag.api.FeatureFlags
import io.element.android.libraries.fullscreenintent.api.FullScreenIntentPermissionsState
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.encryption.RecoveryState
import io.element.android.libraries.matrix.api.roomlist.RoomList
import io.element.android.libraries.matrix.api.roomlist.RoomListFilter
import io.element.android.libraries.matrix.ui.safety.rememberHideInvitesAvatar
import io.element.android.libraries.push.api.battery.BatteryOptimizationState
import io.element.android.services.analytics.api.AnalyticsService
import io.element.android.services.analytics.api.watchers.AnalyticsColdStartWatcher
import io.element.android.services.analyticsproviders.api.trackers.captureInteraction
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch

@Inject
class RoomListPresenter(
    private val client: MatrixClient,
    private val leaveRoomPresenter: Presenter<LeaveRoomState>,
    private val roomListDataSource: RoomListDataSource,
    private val filtersPresenter: Presenter<RoomListFiltersState>,
    private val searchPresenter: Presenter<RoomListSearchState>,
    private val analyticsService: AnalyticsService,
    private val acceptDeclineInvitePresenter: Presenter<AcceptDeclineInviteState>,
    private val fullScreenIntentPermissionsPresenter: Presenter<FullScreenIntentPermissionsState>,
    private val batteryOptimizationPresenter: Presenter<BatteryOptimizationState>,
    private val markRoomAsRead: MarkRoomAsRead,
    private val seenInvitesStore: SeenInvitesStore,
    private val announcementService: AnnouncementService,
    private val coldStartWatcher: AnalyticsColdStartWatcher,
    private val spaceFiltersPresenter: Presenter<SpaceFiltersState>,
    private val featureFlagService: FeatureFlagService,
    private val virtualSpacesProvider: VirtualSpacesProvider,
    private val beeperLabelsRepository: BeeperLabelsRepository,
    private val beeperMergeRepository: BeeperMergeRepository,
) : Presenter<RoomListState> {
    private val encryptionService = client.encryptionService

    @Composable
    override fun present(): RoomListState {
        val coroutineScope = rememberCoroutineScope()
        val leaveRoomState = leaveRoomPresenter.present()
        val filtersState = filtersPresenter.present()
        val searchState = searchPresenter.present()
        val spaceFiltersState = spaceFiltersPresenter.present()
        val acceptDeclineInviteState = acceptDeclineInvitePresenter.present()

        LaunchedEffect(Unit) {
            roomListDataSource.launchIn(this)
        }

        var securityBannerDismissed by rememberSaveable { mutableStateOf(false) }
        val showNewNotificationSoundBanner by remember {
            announcementService.announcementsToShowFlow().map { announcements ->
                announcements.contains(Announcement.NewNotificationSound)
            }
        }.collectAsState(false)

        // Avatar indicator
        val hideInvitesAvatar by client.rememberHideInvitesAvatar()

        val contextMenu = remember { mutableStateOf<RoomListState.ContextMenu>(RoomListState.ContextMenu.Hidden) }
        val declineInviteMenu = remember { mutableStateOf<RoomListState.DeclineInviteMenu>(RoomListState.DeclineInviteMenu.Hidden) }
        val mergePickerMenu = remember { mutableStateOf<RoomListState.MergePickerMenu>(RoomListState.MergePickerMenu.Hidden) }

        fun handleEvent(event: RoomListEvent) {
            when (event) {
                is RoomListEvent.UpdateVisibleRange -> coroutineScope.launch {
                    roomListDataSource.updateVisibleRange(event.range)
                }
                RoomListEvent.DismissRequestVerificationPrompt -> securityBannerDismissed = true
                RoomListEvent.DismissBanner -> securityBannerDismissed = true
                RoomListEvent.DismissNewNotificationSoundBanner -> coroutineScope.launch {
                    announcementService.onAnnouncementDismissed(Announcement.NewNotificationSound)
                }
                RoomListEvent.ToggleSearchResults -> searchState.eventSink(RoomListSearchEvent.ToggleSearchVisibility)
                is RoomListEvent.ShowContextMenu -> {
                    coroutineScope.showContextMenu(event, contextMenu)
                }
                is RoomListEvent.HideContextMenu -> {
                    contextMenu.value = RoomListState.ContextMenu.Hidden
                }
                is RoomListEvent.LeaveRoom -> {
                    leaveRoomState.eventSink(LeaveRoomEvent.LeaveRoom(event.roomId, needsConfirmation = event.needsConfirmation))
                }
                is RoomListEvent.SetRoomIsFavorite -> coroutineScope.setRoomIsFavorite(event.roomId, event.isFavorite)
                is RoomListEvent.MarkAsRead -> coroutineScope.markAsRead(event.roomId)
                is RoomListEvent.MarkAsUnread -> coroutineScope.markAsUnread(event.roomId)
                is RoomListEvent.MarkAsReadFromBadge -> coroutineScope.launch {
                    markRoomAsRead(event.roomId)
                }
                is RoomListEvent.AcceptInvite -> {
                    acceptDeclineInviteState.eventSink(
                        AcceptInvite(event.roomSummary.toInviteData())
                    )
                }
                is RoomListEvent.DeclineInvite -> {
                    acceptDeclineInviteState.eventSink(
                        DeclineInvite(event.roomSummary.toInviteData(), blockUser = event.blockUser, shouldConfirm = false)
                    )
                }
                is RoomListEvent.ShowDeclineInviteMenu -> declineInviteMenu.value = RoomListState.DeclineInviteMenu.Shown(event.roomSummary)
                RoomListEvent.HideDeclineInviteMenu -> declineInviteMenu.value = RoomListState.DeclineInviteMenu.Hidden
                is RoomListEvent.ShowMergePicker -> {
                    coroutineScope.launch {
                        val currentRooms = roomListDataSource.roomSummariesFlow.first()
                        val primaryRoom = currentRooms.find { it.id == event.roomId.value }
                        val primaryName = primaryRoom?.name ?: "Chat"
                        val existingContact = beeperMergeRepository.getMergeForRoom(event.roomId.value)
                        val candidates = currentRooms.filter { it.isDm && it.id != event.roomId.value }.toImmutableList()
                        mergePickerMenu.value = RoomListState.MergePickerMenu.Shown(
                            primaryRoomId = event.roomId,
                            primaryRoomName = primaryName,
                            existingMergedContact = existingContact,
                            candidateRooms = candidates,
                        )
                    }
                }
                RoomListEvent.HideMergePicker -> {
                    mergePickerMenu.value = RoomListState.MergePickerMenu.Hidden
                }
                is RoomListEvent.PerformMerge -> {
                    coroutineScope.launch {
                        val targetId = event.targetRoomId.value
                        val siblingId = event.siblingRoomId.value
                        val existing = beeperMergeRepository.getMergeForRoom(targetId)
                            ?: beeperMergeRepository.getMergeForRoom(siblingId)
                        if (existing != null) {
                            beeperMergeRepository.addRoomToMerge(existing.id, siblingId)
                            if (!existing.roomIds.contains(targetId)) {
                                beeperMergeRepository.addRoomToMerge(existing.id, targetId)
                            }
                        } else {
                            val newContact = MergedContact(
                                id = java.util.UUID.randomUUID().toString(),
                                displayName = event.displayName.ifEmpty { "Contacto" },
                                roomIds = listOf(targetId, siblingId),
                                createdAt = System.currentTimeMillis()
                            )
                            beeperMergeRepository.saveMergedContact(newContact)
                        }
                        mergePickerMenu.value = RoomListState.MergePickerMenu.Hidden
                    }
                }
                is RoomListEvent.UnmergeRoom -> {
                    coroutineScope.launch {
                        val roomIdStr = event.roomId.value
                        val contact = beeperMergeRepository.getMergeForRoom(roomIdStr)
                        if (contact != null) {
                            if (contact.roomIds.size <= 2) {
                                beeperMergeRepository.deleteMergedContact(contact.id)
                            } else {
                                beeperMergeRepository.removeRoomFromMerge(contact.id, roomIdStr)
                            }
                        }
                        mergePickerMenu.value = RoomListState.MergePickerMenu.Hidden
                    }
                }
            }
        }

        LaunchedEffect(filtersState.filterSelectionStates, spaceFiltersState.selectedFilter()) {
            val selectedFilters = filtersState.selectedFilters().map { filter -> filter.into() }
            val selectedSpaceFilter = spaceFiltersState.selectedFilter().into()
            val allFilters = RoomListFilter.All(selectedFilters + listOfNotNull(selectedSpaceFilter))
            roomListDataSource.updateFilter(allFilters)
        }

        val canReportRoom by produceState(false) { value = client.canReportRoom() }
        val showUnreadCount by produceState(false) {
            value = featureFlagService.isFeatureEnabled(FeatureFlags.UnreadIndicatorCount)
        }

        val contentState = roomListContentState(
            securityBannerDismissed,
            showNewNotificationSoundBanner,
            showUnreadCount,
        )

        return RoomListState(
            contextMenu = contextMenu.value,
            declineInviteMenu = declineInviteMenu.value,
            leaveRoomState = leaveRoomState,
            filtersState = filtersState,
            searchState = searchState,
            spaceFiltersState = spaceFiltersState,
            contentState = contentState,
            acceptDeclineInviteState = acceptDeclineInviteState,
            hideInvitesAvatars = hideInvitesAvatar,
            canReportRoom = canReportRoom,
            mergePickerMenu = mergePickerMenu.value,
            eventSink = ::handleEvent,
        )
    }

    @Composable
    private fun rememberSecurityBannerState(
        securityBannerDismissed: Boolean,
    ): State<SecurityBannerState> {
        val currentSecurityBannerDismissed by rememberUpdatedState(securityBannerDismissed)
        val recoveryState by encryptionService.recoveryStateStateFlow.collectAsState()
        return remember {
            derivedStateOf {
                calculateBannerState(
                    securityBannerDismissed = currentSecurityBannerDismissed,
                    recoveryState = recoveryState,
                )
            }
        }
    }

    private fun calculateBannerState(
        securityBannerDismissed: Boolean,
        recoveryState: RecoveryState,
    ): SecurityBannerState {
        if (securityBannerDismissed) {
            return SecurityBannerState.None
        }

        when (recoveryState) {
            RecoveryState.DISABLED -> return SecurityBannerState.SetUpRecovery
            RecoveryState.INCOMPLETE -> return SecurityBannerState.RecoveryKeyConfirmation
            RecoveryState.UNKNOWN,
            RecoveryState.WAITING_FOR_SYNC,
            RecoveryState.ENABLED -> Unit
        }

        return SecurityBannerState.None
    }

    @Composable
    private fun roomListContentState(
        securityBannerDismissed: Boolean,
        showNewNotificationSoundBanner: Boolean,
        showUnreadCount: Boolean,
    ): RoomListContentState {
        val selectedSpace by virtualSpacesProvider.getSelectedSpace().collectAsState()
        val mergedContacts by beeperMergeRepository.mergedContactsFlow.collectAsState()

        val roomSummaries by produceState(
            initialValue = AsyncData.Loading(),
            key1 = selectedSpace,
            key2 = mergedContacts,
        ) {
            roomListDataSource.roomSummariesFlow.collect { summaries ->
                val filtered = filterRoomsForSpace(summaries, selectedSpace)
                val merged = mergeRoomsForContacts(filtered, mergedContacts)
                value = AsyncData.Success(merged)
            }
        }
        val loadingState by roomListDataSource.loadingState.collectAsState()
        val showEmpty by remember {
            derivedStateOf {
                (loadingState as? RoomList.LoadingState.Loaded)?.numberOfRooms == 0
            }
        }
        val showSkeleton by remember {
            derivedStateOf {
                loadingState == RoomList.LoadingState.NotLoaded || roomSummaries is AsyncData.Loading
            }
        }
        val seenRoomInvites by remember { seenInvitesStore.seenRoomIds() }.collectAsState(emptySet())
        val securityBannerState by rememberSecurityBannerState(securityBannerDismissed)
        return when {
            showEmpty -> RoomListContentState.Empty(
                securityBannerState = securityBannerState,
            )
            showSkeleton -> RoomListContentState.Skeleton(count = 16)
            else -> {
                coldStartWatcher.onRoomListVisible()

                RoomListContentState.Rooms(
                    securityBannerState = securityBannerState,
                    showNewNotificationSoundBanner = showNewNotificationSoundBanner,
                    showUnreadCount = showUnreadCount,
                    fullScreenIntentPermissionsState = fullScreenIntentPermissionsPresenter.present(),
                    batteryOptimizationState = batteryOptimizationPresenter.present(),
                    summaries = roomSummaries.dataOrNull().orEmpty().toImmutableList(),
                    seenRoomInvites = seenRoomInvites.toImmutableSet(),
                )
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun CoroutineScope.showContextMenu(event: RoomListEvent.ShowContextMenu, contextMenuState: MutableState<RoomListState.ContextMenu>) = launch {
        val initialState = RoomListState.ContextMenu.Shown(
            roomId = event.roomSummary.roomId,
            roomName = event.roomSummary.name,
            isDm = event.roomSummary.isDm,
            isFavorite = event.roomSummary.isFavorite,
            hasNewContent = event.roomSummary.hasNewContent,
            isMerged = event.roomSummary.mergedContact != null,
        )
        contextMenuState.value = initialState

        client.getRoom(event.roomSummary.roomId)?.use { room ->

            val isShowingContextMenuFlow = snapshotFlow { contextMenuState.value is RoomListState.ContextMenu.Shown }
                .distinctUntilChanged()

            val isFavoriteFlow = room.roomInfoFlow
                .map { it.isFavorite }
                .distinctUntilChanged()

            isFavoriteFlow
                .onEach { isFavorite ->
                    contextMenuState.value = initialState.copy(isFavorite = isFavorite)
                }
                .flatMapLatest { isShowingContextMenuFlow }
                .takeWhile { isShowingContextMenu -> isShowingContextMenu }
                .collect()
        }
    }

    private fun CoroutineScope.setRoomIsFavorite(roomId: RoomId, isFavorite: Boolean) = launch {
        client.getRoom(roomId)?.use { room ->
            room.setIsFavorite(isFavorite)
                .onSuccess {
                    analyticsService.captureInteraction(name = Interaction.Name.MobileRoomListRoomContextMenuFavouriteToggle)
                }
        }
    }

    private fun CoroutineScope.markAsRead(roomId: RoomId) = launch {
        markRoomAsRead(roomId)
            .onSuccess {
                analyticsService.captureInteraction(name = Interaction.Name.MobileRoomListRoomContextMenuUnreadToggle)
            }
    }

    private fun CoroutineScope.markAsUnread(roomId: RoomId) = launch {
        client.getRoom(roomId)?.use { room ->
            room.setUnreadFlag(isUnread = true)
                .onSuccess {
                    analyticsService.captureInteraction(name = Interaction.Name.MobileRoomListRoomContextMenuUnreadToggle)
                }
        }
    }

    private suspend fun filterRoomsForSpace(
        rooms: List<RoomListRoomSummary>,
        spaceId: io.element.android.features.beeperbridge.api.spaces.VirtualSpaceId
    ): List<RoomListRoomSummary> {
        timber.log.Timber.d("BeeperBridge: filterRoomsForSpace IN with ${rooms.size} rooms, spaceId: ${spaceId.javaClass.simpleName}")
        val result = when (spaceId) {
            is io.element.android.features.beeperbridge.api.spaces.VirtualSpaceId.AllChats -> {
                val hiddenNetworks = beeperLabelsRepository.getHiddenNetworks()
                rooms.filter { room ->
                    val network = room.beeperData?.network
                    network == null || !hiddenNetworks.contains(network.name)
                }
            }
            is io.element.android.features.beeperbridge.api.spaces.VirtualSpaceId.NetworkSpace -> {
                rooms.filter { room ->
                    room.beeperData?.network?.name == spaceId.networkKey
                }
            }
            is io.element.android.features.beeperbridge.api.spaces.VirtualSpaceId.LabelSpace -> {
                val labels = beeperLabelsRepository.getLabels()
                val label = labels.find { it.id == spaceId.labelId }
                if (label != null) {
                    rooms.filter { label.roomIds.contains(it.id) }
                } else {
                    emptyList()
                }
            }
            is io.element.android.features.beeperbridge.api.spaces.VirtualSpaceId.RealSpace -> {
                rooms
            }
            is io.element.android.features.beeperbridge.api.spaces.VirtualSpaceId.TagSpace -> {
                rooms
            }
        }
        timber.log.Timber.d("BeeperBridge: filterRoomsForSpace OUT with ${result.size} rooms")
        return result
    }

    private fun mergeRoomsForContacts(
        rooms: List<RoomListRoomSummary>,
        mergedContacts: Map<String, MergedContact>,
    ): List<RoomListRoomSummary> {
        if (mergedContacts.isEmpty()) return rooms

        val roomIdToContact = mutableMapOf<String, MergedContact>()
        for (contact in mergedContacts.values) {
            for (roomId in contact.roomIds) {
                roomIdToContact[roomId] = contact
            }
        }

        val processedContacts = mutableSetOf<String>()
        val result = mutableListOf<RoomListRoomSummary>()

        for (room in rooms) {
            val contact = roomIdToContact[room.id]
            if (contact == null) {
                result.add(room)
            } else {
                if (processedContacts.add(contact.id)) {
                    val siblingRooms = rooms.filter { contact.roomIds.contains(it.id) }
                    val primaryRoom = siblingRooms.firstOrNull() ?: room
                    val totalUnreadMessages = siblingRooms.sumOf { it.numberOfUnreadMessages }
                    val totalUnreadMentions = siblingRooms.sumOf { it.numberOfUnreadMentions }
                    val totalUnreadNotifications = siblingRooms.sumOf { it.numberOfUnreadNotifications }
                    val hasMarkedUnread = siblingRooms.any { it.isMarkedUnread }
                    val allNetworks = siblingRooms.mapNotNull { it.beeperData?.network }.distinct().toImmutableList()

                    val mergedSummary = primaryRoom.copy(
                        name = contact.displayName.ifEmpty { primaryRoom.name },
                        numberOfUnreadMessages = totalUnreadMessages,
                        numberOfUnreadMentions = totalUnreadMentions,
                        numberOfUnreadNotifications = totalUnreadNotifications,
                        isMarkedUnread = hasMarkedUnread,
                        mergedContact = contact,
                        siblingRoomIds = contact.roomIds.filter { it != primaryRoom.id }.toImmutableList(),
                        mergedNetworks = allNetworks,
                    )
                    result.add(mergedSummary)
                }
            }
        }

        return result
    }
}
