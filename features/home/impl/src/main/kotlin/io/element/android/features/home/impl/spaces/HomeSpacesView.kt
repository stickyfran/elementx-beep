/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.spaces

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.beeperbridge.api.spaces.VirtualSpaceId
import io.element.android.libraries.designsystem.atomic.molecules.ButtonColumnMolecule
import io.element.android.libraries.designsystem.atomic.pages.HeaderFooterPage
import io.element.android.libraries.designsystem.components.BigIcon
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.TextButton
import io.element.android.libraries.matrix.ui.components.SpaceHeaderRootView
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
fun HomeSpacesView(
    state: HomeSpacesState,
    lazyListState: LazyListState,
    contentPadding: PaddingValues,
    onSpaceClick: (VirtualSpaceId) -> Unit,
    onCreateSpaceClick: () -> Unit,
    onExploreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.spaces.isEmpty()) {
        EmptySpaceHomeView(
            modifier = modifier.padding(contentPadding),
            onCreateSpaceClick = onCreateSpaceClick,
            onExploreClick = onExploreClick,
            canExploreSpaces = false,
        )
    } else {
        LazyColumn(
            modifier = modifier,
            state = lazyListState,
            contentPadding = contentPadding,
        ) {
            item {
                SpaceHeaderRootView(numberOfSpaces = state.spaces.size)
            }

            item {
                HorizontalDivider()
            }

            itemsIndexed(
                items = state.spaces,
                key = { _, space -> space.id.hashCode() }
            ) { index, space ->
                // Render VirtualSpaceItem as a row item
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSpaceClick(space.id) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = space.emoji ?: space.displayName.take(1),
                        style = ElementTheme.typography.fontBodyLgMedium,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    Text(
                        text = space.displayName,
                        style = ElementTheme.typography.fontBodyLgMedium,
                        color = ElementTheme.colors.textPrimary
                    )
                }

                if (index != state.spaces.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
    }
}

/**
 * Ref: https://www.figma.com/design/pDlJZGBsri47FNTXMnEdXB/Compound-Android-Templates?node-id=1763-74215&t=9IGKMXHDfTGAqzQK-4
 */
@Composable
private fun EmptySpaceHomeView(
    onCreateSpaceClick: () -> Unit,
    onExploreClick: () -> Unit,
    canExploreSpaces: Boolean,
    modifier: Modifier = Modifier,
) {
    HeaderFooterPage(
        modifier = modifier,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, bottom = 16.dp, start = 40.dp, end = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                BigIcon(
                    style = BigIcon.Style.Default(CompoundIcons.SpaceSolid())
                )
                Text(
                    text = stringResource(CommonStrings.screen_space_list_empty_state_title),
                    style = ElementTheme.typography.fontHeadingLgBold,
                    color = ElementTheme.colors.textPrimary,
                    textAlign = TextAlign.Center,
                )
            }
        },
        footer = {
            ButtonColumnMolecule {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(CommonStrings.action_create_space),
                    onClick = onCreateSpaceClick,
                )
                if (canExploreSpaces) {
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(CommonStrings.action_explore_public_spaces),
                        onClick = onExploreClick,
                    )
                }
            }
        }
    )
}

@PreviewsDayNight
@Composable
internal fun HomeSpacesViewPreview(
    @PreviewParameter(HomeSpacesStateProvider::class) state: HomeSpacesState,
) = ElementPreview {
    HomeSpacesView(
        state = state,
        lazyListState = rememberLazyListState(),
        onSpaceClick = {},
        onCreateSpaceClick = {},
        onExploreClick = {},
        contentPadding = PaddingValues(bottom = 112.dp),
    )
}
