/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
package io.element.android.features.beeperbridge.api

object DisplayNameSanitizer {
    private val phoneSuffixRegex = Regex("""\s*\(?\+{1,2}[0-9\s.\-]+\)?$""")
    private val hasLettersRegex = Regex("""\p{L}""")

    /**
     * Sanitizes display names by removing trailing phone numbers or artifacts (e.g. "Name (+549...)" -> "Name"),
     * while preserving bare phone numbers (e.g. "+54911...") for unsaved contacts.
     */
    fun sanitize(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val trimmed = raw.trim()
        val cleaned = trimmed.replace(phoneSuffixRegex, "").trim()
        if (cleaned.isNotEmpty() && hasLettersRegex.containsMatchIn(cleaned)) {
            return cleaned.replace("++", "+")
        }
        return trimmed.replace("++", "+")
    }
}
