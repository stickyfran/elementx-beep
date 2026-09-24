/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.appconfig

object PushConfig {
    /**
     * Note: pusher_app_id cannot exceed 64 chars.
     * We use FluffyChat's data_message app ID because Beeper's server
     * trusts it to send full decrypted payloads instead of empty tickles.
     */
    const val PUSHER_APP_ID: String = "chat.fluffy.fluffychat.data_message"
}
