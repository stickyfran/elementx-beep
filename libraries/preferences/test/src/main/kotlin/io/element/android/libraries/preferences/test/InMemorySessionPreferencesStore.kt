/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.preferences.test

import io.element.android.libraries.preferences.api.store.SessionPreferencesStore
import io.element.android.libraries.preferences.api.store.VideoCompressionPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class InMemorySessionPreferencesStore(
    isSharePresenceEnabled: Boolean = true,
    isSendPublicReadReceiptsEnabled: Boolean = true,
    isRenderReadReceiptsEnabled: Boolean = true,
    isSendTypingNotificationsEnabled: Boolean = true,
    isRenderTypingNotificationsEnabled: Boolean = true,
    isSessionVerificationSkipped: Boolean = false,
    doesCompressMedia: Boolean = true,
    videoCompressionPreset: VideoCompressionPreset = VideoCompressionPreset.STANDARD,
    isManualReadReceiptsEnabled: Boolean = false,
    isShowManualReadBannerEnabled: Boolean = true,
    isShowManualReadBottomEnabled: Boolean = true,
    isShowManualReadInputBarEnabled: Boolean = true,
) : SessionPreferencesStore {
    private val isSharePresenceEnabled = MutableStateFlow(isSharePresenceEnabled)
    private val isSendPublicReadReceiptsEnabled = MutableStateFlow(isSendPublicReadReceiptsEnabled)
    private val isRenderReadReceiptsEnabled = MutableStateFlow(isRenderReadReceiptsEnabled)
    private val isSendTypingNotificationsEnabled = MutableStateFlow(isSendTypingNotificationsEnabled)
    private val isRenderTypingNotificationsEnabled = MutableStateFlow(isRenderTypingNotificationsEnabled)
    private val isSessionVerificationSkipped = MutableStateFlow(isSessionVerificationSkipped)
    private val doesCompressMedia = MutableStateFlow(doesCompressMedia)
    private val videoCompressionPreset = MutableStateFlow(videoCompressionPreset)
    private val isManualReadReceiptsEnabled = MutableStateFlow(isManualReadReceiptsEnabled)
    private val isShowManualReadBannerEnabled = MutableStateFlow(isShowManualReadBannerEnabled)
    private val isShowManualReadBottomEnabled = MutableStateFlow(isShowManualReadBottomEnabled)
    private val isShowManualReadInputBarEnabled = MutableStateFlow(isShowManualReadInputBarEnabled)
    var clearCallCount = 0
        private set

    override suspend fun setSharePresence(enabled: Boolean) {
        isSharePresenceEnabled.tryEmit(enabled)
    }

    override fun isSharePresenceEnabled(): Flow<Boolean> = isSharePresenceEnabled

    override suspend fun setSendPublicReadReceipts(enabled: Boolean) {
        isSendPublicReadReceiptsEnabled.tryEmit(enabled)
    }

    override fun isSendPublicReadReceiptsEnabled(): Flow<Boolean> = isSendPublicReadReceiptsEnabled

    override suspend fun setRenderReadReceipts(enabled: Boolean) {
        isRenderReadReceiptsEnabled.tryEmit(enabled)
    }

    override fun isRenderReadReceiptsEnabled(): Flow<Boolean> = isRenderReadReceiptsEnabled

    override suspend fun setSendTypingNotifications(enabled: Boolean) {
        isSendTypingNotificationsEnabled.tryEmit(enabled)
    }

    override fun isSendTypingNotificationsEnabled(): Flow<Boolean> = isSendTypingNotificationsEnabled

    override suspend fun setRenderTypingNotifications(enabled: Boolean) {
        isRenderTypingNotificationsEnabled.tryEmit(enabled)
    }

    override fun isRenderTypingNotificationsEnabled(): Flow<Boolean> = isRenderTypingNotificationsEnabled

    override suspend fun setSkipSessionVerification(skip: Boolean) {
        isSessionVerificationSkipped.tryEmit(skip)
    }

    override fun isSessionVerificationSkipped(): Flow<Boolean> {
        return isSessionVerificationSkipped
    }

    override suspend fun setOptimizeImages(compress: Boolean) = doesCompressMedia.emit(compress)

    override fun doesOptimizeImages(): Flow<Boolean> = doesCompressMedia

    override suspend fun setVideoCompressionPreset(preset: VideoCompressionPreset) {
        videoCompressionPreset.value = preset
    }

    override fun getVideoCompressionPreset(): Flow<VideoCompressionPreset> {
        return videoCompressionPreset
    }

    override suspend fun setManualReadReceiptsEnabled(enabled: Boolean) {
        isManualReadReceiptsEnabled.tryEmit(enabled)
    }

    override fun isManualReadReceiptsEnabled(): Flow<Boolean> = isManualReadReceiptsEnabled

    override suspend fun setShowManualReadBanner(enabled: Boolean) {
        isShowManualReadBannerEnabled.tryEmit(enabled)
    }

    override fun isShowManualReadBannerEnabled(): Flow<Boolean> = isShowManualReadBannerEnabled

    override suspend fun setShowManualReadBottom(enabled: Boolean) {
        isShowManualReadBottomEnabled.tryEmit(enabled)
    }

    override fun isShowManualReadBottomEnabled(): Flow<Boolean> = isShowManualReadBottomEnabled

    override suspend fun setShowManualReadInputBar(enabled: Boolean) {
        isShowManualReadInputBarEnabled.tryEmit(enabled)
    }

    override fun isShowManualReadInputBarEnabled(): Flow<Boolean> = isShowManualReadInputBarEnabled

    override suspend fun clear() {
        clearCallCount++
        isSendPublicReadReceiptsEnabled.tryEmit(true)
    }
}
