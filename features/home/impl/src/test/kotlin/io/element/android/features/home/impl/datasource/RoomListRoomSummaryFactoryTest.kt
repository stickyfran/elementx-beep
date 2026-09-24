/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.datasource

import io.element.android.features.beeperbridge.api.BeeperBridgeService
import io.element.android.features.beeperbridge.test.FakeBeeperBridgeService
import io.element.android.libraries.dateformatter.api.DateFormatter
import io.element.android.libraries.dateformatter.test.FakeDateFormatter
import io.element.android.libraries.eventformatter.api.RoomLatestEventFormatter
import io.element.android.libraries.eventformatter.test.FakeRoomLatestEventFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

fun aRoomListRoomSummaryFactory(
    dateFormatter: DateFormatter = FakeDateFormatter { _, _, _ -> "Today" },
    roomLatestEventFormatter: RoomLatestEventFormatter = FakeRoomLatestEventFormatter(),
    beeperBridgeService: BeeperBridgeService = FakeBeeperBridgeService(),
    sessionCoroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined),
) = RoomListRoomSummaryFactory(
    dateFormatter = dateFormatter,
    roomLatestEventFormatter = roomLatestEventFormatter,
    beeperBridgeService = beeperBridgeService,
    sessionCoroutineScope = sessionCoroutineScope,
)
