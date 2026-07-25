/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.spaces

import io.element.android.features.beeperbridge.api.spaces.VirtualSpaceId

sealed interface HomeSpacesEvents {
    data class SelectSpace(val spaceId: VirtualSpaceId) : HomeSpacesEvents
}
