/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.slions.compose.toolkit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.CardElevation
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

public fun LazyListScope.itemActionIconButton(
    key: String,
    title: String,
    iconButtonIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: String? = null,
    staticSummary: String? = null,
    onClick: (() -> Unit)? = null,
    iconButtonEnabled: Boolean = enabled,
    onIconButtonClick: () -> Unit,
    style: CardStyle? = null,
    cardColor: Color? = null,
    cardElevation: CardElevation? = null,
    cardBorder: BorderStroke? = null,
    shape: Shape? = null,
    outerPadding: PaddingValues? = null,
) {
    SearchIndexer.record(key, title, staticSummary ?: summary, icon)
    item(key = key, contentType = "ItemActionIconButton") {
        cardedItem(
            modifier = modifier,
            key = key,
            style = style,
            cardColor = cardColor,
            cardElevation = cardElevation,
            cardBorder = cardBorder,
            shape = shape,
            outerPadding = outerPadding,
        ) { rowModifier ->
            ItemActionIconButton(
                title = title,
                iconButtonIcon = iconButtonIcon,
                modifier = rowModifier,
                enabled = enabled,
                icon = icon,
                summary = summary,
                onClick = onClick,
                iconButtonEnabled = iconButtonEnabled,
                onIconButtonClick = onIconButtonClick,
            )
        }
    }
}

/**
 * Adds an action icon button item to a [group] (a [GroupScope]): the row joins the group
 * and is drawn in the group's card — the group's `style`, `shape` and spacing apply, so
 * this overload takes no [modifier] and no card parameters. Parameter semantics match the
 * [itemActionIconButton] ([LazyListScope]) overload.
 */
public fun GroupScope.itemActionIconButton(
    key: String,
    title: String,
    iconButtonIcon: @Composable () -> Unit,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: String? = null,
    staticSummary: String? = null,
    onClick: (() -> Unit)? = null,
    iconButtonEnabled: Boolean = enabled,
    onIconButtonClick: () -> Unit,
) {
    items.add(
        GroupItem(
            title = title,
            summary = staticSummary ?: summary,
            icon = icon,
            content = {
                ItemActionIconButton(
                    title = title,
                    iconButtonIcon = iconButtonIcon,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    icon = icon,
                    summary = summary,
                    onClick = onClick,
                    iconButtonEnabled = iconButtonEnabled,
                    onIconButtonClick = onIconButtonClick,
                )
            },
        )
    )
}

@Composable
public fun ItemActionIconButton(
    title: String,
    iconButtonIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: String? = null,
    onClick: (() -> Unit)? = null,
    iconButtonEnabled: Boolean = enabled,
    onIconButtonClick: () -> Unit,
) {
    ItemActions(
        title = title,
        secondTarget = {
            val theme = LocalItemTheme.current
            IconButton(
                onClick = onIconButtonClick,
                modifier =
                    Modifier.padding(
                        theme.padding.copy(start = theme.horizontalSpacing).offset((-12).dp)
                    ),
                enabled = iconButtonEnabled,
                colors =
                    IconButtonDefaults.iconButtonColors(
                        contentColor = theme.iconColor,
                        disabledContentColor = theme.iconColor.copy(alpha = theme.disabledOpacity),
                    ),
                content = iconButtonIcon,
            )
        },
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        summary = summary,
        onClick = onClick,
    )
}
