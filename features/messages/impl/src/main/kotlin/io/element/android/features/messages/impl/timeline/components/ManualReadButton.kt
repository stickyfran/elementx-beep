/*
 * Copyright (c) 2025 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Floating 'Mark as read' button displayed above the message composer
 * when manual read receipts mode is enabled and the room has unread messages.
 * Mirrors FluffyChat's FloatingManualReadButton.
 */
@Composable
internal fun ManualReadButton(
    isVisible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = isVisible,
        enter = scaleIn(animationSpec = tween(220), initialScale = 0.8f) + fadeIn(animationSpec = tween(220)),
        exit = scaleOut(animationSpec = tween(180), targetScale = 0.8f) + fadeOut(animationSpec = tween(180)),
    ) {
        val view = LocalView.current
        val coroutineScope = rememberCoroutineScope()
        var isMarked by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .size(38.dp)
                .shadow(elevation = 2.dp, shape = CircleShape)
                .background(color = ElementTheme.colors.bgCanvasDefault, shape = CircleShape)
                .clip(CircleShape)
                .border(1.dp, ElementTheme.colors.borderInteractiveHovered, CircleShape)
                .clickable {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    isMarked = true
                    onClick()
                    coroutineScope.launch {
                        delay(500)
                        isMarked = false
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = isMarked,
                transitionSpec = {
                    fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
                },
                label = "ManualReadButtonIcon",
            ) { marked ->
                if (marked) {
                    Icon(
                        modifier = Modifier.size(22.dp),
                        imageVector = CompoundIcons.CheckCircle(),
                        contentDescription = stringResource(id = CommonStrings.action_mark_as_read),
                        tint = ElementTheme.colors.iconAccentPrimary,
                    )
                } else {
                    Icon(
                        modifier = Modifier.size(22.dp),
                        imageVector = CompoundIcons.MarkAsRead(),
                        contentDescription = stringResource(id = CommonStrings.action_mark_as_read),
                        tint = ElementTheme.colors.iconAccentPrimary,
                    )
                }
            }
        }
    }
}

@PreviewsDayNight
@Composable
internal fun ManualReadButtonPreview() = ElementPreview {
    ManualReadButton(isVisible = true, onClick = {})
}
