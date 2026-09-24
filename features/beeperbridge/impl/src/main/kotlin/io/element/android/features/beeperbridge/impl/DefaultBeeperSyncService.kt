/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
package io.element.android.features.beeperbridge.impl

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.element.android.features.beeperbridge.api.BeeperBridgeService
import io.element.android.features.beeperbridge.api.BeeperLabelsRepository
import io.element.android.features.beeperbridge.api.BeeperMergeRepository
import io.element.android.features.beeperbridge.api.BeeperSyncReport
import io.element.android.features.beeperbridge.api.BeeperSyncService
import io.element.android.features.beeperbridge.api.BeeperSyncState
import io.element.android.libraries.core.extensions.runCatchingExceptions
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.di.annotations.SessionCoroutineScope
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.timeline.Timeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import timber.log.Timber

@ContributesBinding(SessionScope::class)
@SingleIn(SessionScope::class)
class DefaultBeeperSyncService @Inject constructor(
    private val matrixClient: MatrixClient,
    private val beeperBridgeService: BeeperBridgeService,
    private val beeperMergeRepository: BeeperMergeRepository,
    private val beeperLabelsRepository: BeeperLabelsRepository,
    @SessionCoroutineScope private val sessionCoroutineScope: CoroutineScope,
) : BeeperSyncService {

    private val _syncState = MutableStateFlow<BeeperSyncState>(BeeperSyncState.Idle)
    override val syncState: StateFlow<BeeperSyncState> = _syncState.asStateFlow()

    override suspend fun syncAll(backfillDays: Int): Result<BeeperSyncReport> {
        return runCatchingExceptions {
            _syncState.value = BeeperSyncState.Syncing("Sincronizando chats y redes...")
            Timber.d("BeeperSyncService: Starting Smart Sync...")

            // 1. Sync m.direct
            beeperBridgeService.syncDirectChats()

            // 2. Sync merged contacts
            _syncState.value = BeeperSyncState.Syncing("Sincronizando contactos combinados...")
            beeperMergeRepository.syncFromRemote()

            // 3. Sync labels
            _syncState.value = BeeperSyncState.Syncing("Sincronizando etiquetas...")
            beeperLabelsRepository.syncFromRemote()

            val mergedContacts = beeperMergeRepository.getMergedContacts()
            val labels = beeperLabelsRepository.getLabels()

            // 4. Smart backfill: paginate up to backfillDays (default 60 days) for merged contacts
            _syncState.value = BeeperSyncState.Syncing("Verificando historial de 2 meses...")
            var roomsBackfilled = 0
            val targetRoomIds = mergedContacts.values.flatMap { it.roomIds }.distinct()
            val cutoffMillis = System.currentTimeMillis() - (backfillDays.toLong() * 24 * 60 * 60 * 1000)

            for (roomIdStr in targetRoomIds) {
                try {
                    val roomId = RoomId(roomIdStr)
                    val joinedRoom = matrixClient.getJoinedRoom(roomId) ?: continue
                    val timeline = joinedRoom.liveTimeline

                    // Paginate backwards up to 8 batches if we haven't reached the cutoff
                    var batches = 0
                    while (batches < 8) {
                        val status = timeline.backwardPaginationStatus.value
                        if (!status.canPaginate) break

                        // Check oldest event timestamp in current timeline
                        val items = timeline.timelineItems.firstOrNull() ?: emptyList()
                        val oldestEvent = items.filterIsInstance<io.element.android.libraries.matrix.api.timeline.MatrixTimelineItem.Event>().firstOrNull()
                        // If we already have items older than cutoff, stop paginating this room
                        val oldestTs = oldestEvent?.event?.timestamp
                        if (oldestTs != null && oldestTs < cutoffMillis) {
                            break
                        }

                        val result = timeline.paginate(Timeline.PaginationDirection.BACKWARDS)
                        batches++
                        if (result.isFailure || result.getOrNull() == false) break
                        delay(50) // Small yield to prevent saturating the network/CPU
                    }
                    if (batches > 0) {
                        roomsBackfilled++
                    }
                } catch (e: Exception) {
                    Timber.w(e, "BeeperSyncService: Error backfilling history for room $roomIdStr")
                }
            }

            val report = BeeperSyncReport(
                mergedCount = mergedContacts.size,
                labelsCount = labels.size,
                roomsBackfilled = roomsBackfilled,
            )
            Timber.d("BeeperSyncService: Smart Sync finished: $report")
            _syncState.value = BeeperSyncState.Completed(report)

            // Return to Idle after 3 seconds
            sessionCoroutineScope.launch {
                delay(3000)
                _syncState.value = BeeperSyncState.Idle
            }

            report
        }.onFailure {
            Timber.e(it, "BeeperSyncService: Error during Smart Sync")
            _syncState.value = BeeperSyncState.Error(it)
        }
    }
}
