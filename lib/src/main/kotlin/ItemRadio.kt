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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.CardElevation
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role

public fun LazyListScope.itemRadio(
    key: String,
    selected: Boolean,
    title: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    summary: String? = null,
    staticSummary: String? = null,
    widgetContainer: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
    style: CardStyle? = null,
    cardColor: Color? = null,
    cardElevation: CardElevation? = null,
    cardBorder: BorderStroke? = null,
    shape: Shape? = null,
    outerPadding: PaddingValues? = null,
) {
    SearchIndexer.record(key, title, staticSummary ?: summary)
    item(key = key, contentType = "ItemRadio") {
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
            ItemRadio(
                selected = selected,
                title = title,
                modifier = rowModifier,
                enabled = enabled,
                summary = summary,
                widgetContainer = widgetContainer,
                onClick = onClick,
            )
        }
    }
}

/**
 * Adds a radio button item to a [group] (a [GroupScope]): the row joins the group and is
 * drawn in the group's card — the group's `style`, `shape` and spacing apply, so this
 * overload takes no [modifier] and no card parameters. Parameter semantics match the
 * [itemRadio] ([LazyListScope]) overload.
 */
public fun GroupScope.itemRadio(
    key: String,
    selected: Boolean,
    title: String,
    enabled: Boolean = true,
    summary: String? = null,
    staticSummary: String? = null,
    widgetContainer: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    items.add(
        GroupItem(
            title = title,
            summary = staticSummary ?: summary,
            widgetContainer = widgetContainer,
            content = {
                ItemRadio(
                    selected = selected,
                    title = title,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    summary = summary,
                    widgetContainer = widgetContainer,
                    onClick = onClick,
                )
            },
        )
    )
}

@Composable
public fun ItemRadio(
    selected: Boolean,
    title: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    summary: String? = null,
    widgetContainer: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Item(
        title = title,
        modifier = modifier.selectable(selected, enabled, Role.RadioButton, onClick = onClick),
        enabled = enabled,
        icon = { RadioButton(selected = selected, onClick = null, enabled = enabled) },
        summary = summary,
        widgetContainer = widgetContainer,
    )
}
