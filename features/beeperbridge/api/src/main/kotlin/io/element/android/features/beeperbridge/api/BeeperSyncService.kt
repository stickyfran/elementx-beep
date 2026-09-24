/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
package io.element.android.features.beeperbridge.api

import kotlinx.coroutines.flow.StateFlow

data class BeeperSyncReport(
    val mergedCount: Int,
    val labelsCount: Int,
    val roomsBackfilled: Int,
)

sealed interface BeeperSyncState {
    data object Idle : BeeperSyncState
    data class Syncing(val message: String) : BeeperSyncState
    data class Completed(val report: BeeperSyncReport) : BeeperSyncState
    data class Error(val error: Throwable) : BeeperSyncState
}

interface BeeperSyncService {
    val syncState: StateFlow<BeeperSyncState>
    suspend fun syncAll(backfillDays: Int = 60): Result<BeeperSyncReport>
}
