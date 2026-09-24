/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
package io.element.android.features.beeperbridge.impl

import dev.zacsweers.metro.Inject
import io.element.android.libraries.core.extensions.runCatchingExceptions
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.sessionstorage.api.SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.net.URLEncoder

@Inject
open class MatrixAccountDataService(
    private val matrixClient: MatrixClient,
    private val sessionStore: SessionStore,
    private val okHttpClient: OkHttpClient,
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private suspend fun getAuthDetails(): Pair<String, String>? {
        val sessionData = sessionStore.getSession(matrixClient.sessionId.value) ?: return null
        val baseUrl = sessionData.homeserverUrl.trimEnd('/')
        return Pair(baseUrl, sessionData.accessToken)
    }

    open suspend fun getAccountData(type: String): Result<String?> = withContext(Dispatchers.IO) {
        runCatchingExceptions {
            val (baseUrl, token) = getAuthDetails() ?: error("Session details not found")
            val encodedUserId = URLEncoder.encode(matrixClient.sessionId.value, "UTF-8")
            val encodedType = URLEncoder.encode(type, "UTF-8")
            val url = "$baseUrl/_matrix/client/v3/user/$encodedUserId/account_data/$encodedType"

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                when (response.code) {
                    200 -> response.body?.string()
                    404 -> null
                    else -> error("Failed to get account data: HTTP ${response.code}")
                }
            }
        }.onFailure {
            Timber.e(it, "MatrixAccountDataService: Error fetching account data for $type")
        }
    }

    open suspend fun setAccountData(type: String, contentJson: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatchingExceptions {
            val (baseUrl, token) = getAuthDetails() ?: error("Session details not found")
            val encodedUserId = URLEncoder.encode(matrixClient.sessionId.value, "UTF-8")
            val encodedType = URLEncoder.encode(type, "UTF-8")
            val url = "$baseUrl/_matrix/client/v3/user/$encodedUserId/account_data/$encodedType"

            val requestBody = contentJson.toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .put(requestBody)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("Failed to set account data: HTTP ${response.code} - ${response.message}")
                }
            }
        }.onFailure {
            Timber.e(it, "MatrixAccountDataService: Error setting account data for $type")
        }
    }
}
