/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
package io.element.android.features.beeperbridge.api

import kotlinx.coroutines.flow.StateFlow

interface BeeperMergeRepository {
    /**
     * Observable state flow of all merged contacts, keyed by mergeId.
     */
    val mergedContactsFlow: StateFlow<Map<String, MergedContact>>

    /**
     * Get snapshot map of all merged contacts.
     */
    suspend fun getMergedContacts(): Map<String, MergedContact>

    /**
     * Find if a roomId belongs to a merged contact.
     */
    suspend fun getMergeForRoom(roomId: String): MergedContact?

    /**
     * Get sibling roomIds for a given roomId (other rooms belonging to the same merge, excluding self).
     */
    suspend fun getSiblingRoomIds(roomId: String): List<String>

    /**
     * Create or update a merged contact.
     */
    suspend fun saveMergedContact(contact: MergedContact): Result<Unit>

    /**
     * Delete a merged contact by its mergeId.
     */
    suspend fun deleteMergedContact(mergeId: String): Result<Unit>

    /**
     * Add a room to an existing merge.
     */
    suspend fun addRoomToMerge(mergeId: String, roomId: String): Result<Unit>

    /**
     * Remove a room from a merge.
     */
    suspend fun removeRoomFromMerge(mergeId: String, roomId: String): Result<Unit>

    /**
     * Force synchronization with remote Matrix Account Data `com.beeper.merged_contacts`.
     */
    suspend fun syncFromRemote(): Result<Unit>
}
