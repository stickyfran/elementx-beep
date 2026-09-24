/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
package io.element.android.features.beeperbridge.impl

import com.google.common.truth.Truth.assertThat
import io.element.android.features.beeperbridge.api.DisplayNameSanitizer
import org.junit.Test

class DisplayNameSanitizerTest {

    @Test
    fun `strips phone number in parentheses from contact name`() {
        val result = DisplayNameSanitizer.sanitize("Bruno Musco Nuevo (+5491127536793)")
        assertThat(result).isEqualTo("Bruno Musco Nuevo")
    }

    @Test
    fun `strips phone number without parentheses from contact name`() {
        val result = DisplayNameSanitizer.sanitize("Juan Perez +5491112345678")
        assertThat(result).isEqualTo("Juan Perez")
    }

    @Test
    fun `preserves bare phone number for unsaved contacts`() {
        val result = DisplayNameSanitizer.sanitize("+5491127536793")
        assertThat(result).isEqualTo("+5491127536793")
    }

    @Test
    fun `handles empty and blank strings gracefully`() {
        assertThat(DisplayNameSanitizer.sanitize("")).isEmpty()
        assertThat(DisplayNameSanitizer.sanitize(null)).isEmpty()
        assertThat(DisplayNameSanitizer.sanitize("   ")).isEmpty()
    }
}
