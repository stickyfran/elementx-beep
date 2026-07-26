/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
package io.element.android.features.beeperbridge.impl.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.element.android.libraries.designsystem.components.preferences.PreferencePage
import io.element.android.libraries.designsystem.theme.components.ListItem
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.components.list.ListItemContent
import io.element.android.libraries.designsystem.theme.components.IconSource
import io.element.android.compound.tokens.generated.CompoundIcons

@Composable
fun BeeperNetworksView(
    state: BeeperNetworksState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PreferencePage(
        modifier = modifier,
        onBackClick = onBackClick,
        title = "Beeper Networks",
    ) {
        state.networks.forEach { network ->
            ListItem(
                headlineContent = { Text(network.displayName) },
                leadingContent = ListItemContent.Icon(IconSource.Vector(CompoundIcons.Chat())), // Placeholder icon
                trailingContent = ListItemContent.Text("Connect"), // We'll update this later based on actual status
                onClick = {
                    // TODO: trigger login flow
                }
            )
        }
    }
}

@io.element.android.libraries.designsystem.preview.PreviewWithLargeHeight
@Composable
internal fun BeeperNetworksViewLightPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(BeeperNetworksStateProvider::class) state: BeeperNetworksState
) = io.element.android.libraries.designsystem.preview.ElementPreviewLight {
    BeeperNetworksView(
        state = state,
        onBackClick = {},
    )
}

@io.element.android.libraries.designsystem.preview.PreviewWithLargeHeight
@Composable
internal fun BeeperNetworksViewDarkPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(BeeperNetworksStateProvider::class) state: BeeperNetworksState
) = io.element.android.libraries.designsystem.preview.ElementPreviewDark {
    BeeperNetworksView(
        state = state,
        onBackClick = {},
    )
}
