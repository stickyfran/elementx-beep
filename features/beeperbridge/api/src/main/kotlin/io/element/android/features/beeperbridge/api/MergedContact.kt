/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
package io.element.android.features.beeperbridge.api

import androidx.compose.runtime.Immutable

@Immutable
data class MergedContact(
    val id: String,
    val displayName: String,
    val avatarMxc: String? = null,
    val roomIds: List<String> = emptyList(),
    val phoneContactId: String? = null,
    val customWhatsAppPhone: String? = null,
    val customInstagramHandle: String? = null,
    val createdAt: Long = System.currentTimeMillis() / 1000,
)
