/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
package io.element.android.features.beeperbridge.test

import io.element.android.features.beeperbridge.api.BeeperSyncReport
import io.element.android.features.beeperbridge.api.BeeperSyncService
import io.element.android.features.beeperbridge.api.BeeperSyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeBeeperSyncService : BeeperSyncService {
    private val _syncState = MutableStateFlow<BeeperSyncState>(BeeperSyncState.Idle)
    override val syncState: StateFlow<BeeperSyncState> = _syncState.asStateFlow()

    override suspend fun syncAll(backfillDays: Int): Result<BeeperSyncReport> {
        val report = BeeperSyncReport(mergedCount = 0, labelsCount = 0, roomsBackfilled = 0)
        _syncState.value = BeeperSyncState.Completed(report)
        return Result.success(report)
    }
}
