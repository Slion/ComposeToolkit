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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.CardElevation
import androidx.compose.material3.DividerDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

public fun LazyListScope.itemActions(
    key: String,
    title: String,
    secondTarget: @Composable () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: String? = null,
    staticSummary: String? = null,
    onClick: (() -> Unit)? = null,
    style: CardStyle? = null,
    cardColor: Color? = null,
    cardElevation: CardElevation? = null,
    cardBorder: BorderStroke? = null,
    shape: Shape? = null,
    outerPadding: PaddingValues? = null,
) {
    SearchIndexer.record(key, title, staticSummary ?: summary, icon)
    item(key = key, contentType = "ItemActions") {
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
            ItemActions(
                title = title,
                secondTarget = secondTarget,
                modifier = rowModifier,
                enabled = enabled,
                icon = icon,
                summary = summary,
                onClick = onClick,
            )
        }
    }
}

/**
 * Adds an action item to a [group] (a [GroupScope]): the row joins the group and is drawn
 * in the group's card — the group's `style`, `shape` and spacing apply, so this overload
 * takes no [modifier] and no card parameters. Parameter semantics match the [itemActions]
 * ([LazyListScope]) overload.
 */
public fun GroupScope.itemActions(
    key: String,
    title: String,
    secondTarget: @Composable () -> Unit,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: String? = null,
    staticSummary: String? = null,
    onClick: (() -> Unit)? = null,
) {
    items.add(
        GroupItem(
            title = title,
            summary = staticSummary ?: summary,
            icon = icon,
            content = {
                ItemActions(
                    title = title,
                    secondTarget = secondTarget,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    icon = icon,
                    summary = summary,
                    onClick = onClick,
                )
            },
        )
    )
}

@Composable
public fun ItemActions(
    title: String,
    secondTarget: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Item(
        title = title,
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        summary = summary,
        widgetContainer = {
            val theme = LocalItemTheme.current
            Row(
                modifier = Modifier.padding(start = theme.horizontalSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier.size(DividerDefaults.Thickness, theme.dividerHeight)
                            .background(
                                DividerDefaults.color.let {
                                    if (enabled) it else it.copy(alpha = theme.disabledOpacity)
                                }
                            )
                )
                secondTarget()
            }
        },
        onClick = onClick,
    )
}
