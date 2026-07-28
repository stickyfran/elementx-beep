/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.roomlist

import io.element.android.libraries.matrix.api.roomlist.RoomListFilter
import org.matrix.rustcomponents.sdk.RoomListEntriesDynamicFilterKind
import org.matrix.rustcomponents.sdk.RoomListEntriesDynamicFilterKind.All
import org.matrix.rustcomponents.sdk.RoomListEntriesDynamicFilterKind.Any
import org.matrix.rustcomponents.sdk.RoomListEntriesDynamicFilterKind.Category
import org.matrix.rustcomponents.sdk.RoomListEntriesDynamicFilterKind.DeduplicateVersions
import org.matrix.rustcomponents.sdk.RoomListEntriesDynamicFilterKind.Favourite
import org.matrix.rustcomponents.sdk.RoomListEntriesDynamicFilterKind.Identifiers
import org.matrix.rustcomponents.sdk.RoomListEntriesDynamicFilterKind.Invite
import org.matrix.rustcomponents.sdk.RoomListEntriesDynamicFilterKind.NonLeft
import org.matrix.rustcomponents.sdk.RoomListEntriesDynamicFilterKind.NonSpace
import org.matrix.rustcomponents.sdk.RoomListEntriesDynamicFilterKind.None
import org.matrix.rustcomponents.sdk.RoomListEntriesDynamicFilterKind.NormalizedMatchRoomName
import org.matrix.rustcomponents.sdk.RoomListEntriesDynamicFilterKind.Space
import org.matrix.rustcomponents.sdk.RoomListEntriesDynamicFilterKind.Unread
import org.matrix.rustcomponents.sdk.RoomListFilterCategory

/**
 * Mapper for converting RoomListFilter to Rust SDK filter kinds.
 */
internal object RoomListFilterMapper {
    /**
     * Base rust filters to always apply across all room lists.
     * With version deduplication enabled.
     */
    private val RUST_BASE_FILTERS = listOf<RoomListEntriesDynamicFilterKind>(
        DeduplicateVersions
    )

    /**
     * Converts a RoomListFilter to a Rust SDK RoomListEntriesDynamicFilterKind.
     * Applies base filters along with the provided filter.
     */
    fun toRustFilter(filter: RoomListFilter): RoomListEntriesDynamicFilterKind {
        val mapped = mapFilter(filter)
        val allFilters = if (mapped != null) RUST_BASE_FILTERS + mapped else RUST_BASE_FILTERS
        return if (allFilters.size == 1) allFilters.first() else All(allFilters)
    }

    /**
     * Maps a RoomListFilter to its Rust SDK equivalent.
     * Returns null for empty All/Any filters to avoid empty filter lists matching no rooms in Rust SDK.
     */
    private fun mapFilter(filter: RoomListFilter): RoomListEntriesDynamicFilterKind? {
        return when (filter) {
            is RoomListFilter.All -> {
                val mapped = filter.filters.mapNotNull { mapFilter(it) }
                if (mapped.isEmpty()) null else All(filters = mapped)
            }
            is RoomListFilter.Any -> {
                val mapped = filter.filters.mapNotNull { mapFilter(it) }
                if (mapped.isEmpty()) null else Any(filters = mapped)
            }
            is RoomListFilter.Identifiers -> Identifiers(identifiers = filter.values.map { it.value })
            RoomListFilter.None -> None
            RoomListFilter.Category.Group -> Category(RoomListFilterCategory.GROUP)
            RoomListFilter.Category.People -> Category(RoomListFilterCategory.PEOPLE)
            RoomListFilter.Category.Space -> Space
            RoomListFilter.Favorite -> Favourite
            RoomListFilter.Unread -> Unread
            is RoomListFilter.NormalizedMatchRoomName -> NormalizedMatchRoomName(
                pattern = filter.pattern
            )
            RoomListFilter.Invite -> Invite
        }
    }
}
