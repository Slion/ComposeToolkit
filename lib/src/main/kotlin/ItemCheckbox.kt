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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.CardElevation
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role

/**
 * Adds a checkbox preference item to the lazy list, whose state is remembered by [key].
 *
 * @param key The lazy list key of the item, and the preference state key.
 * @param defaultValue The initial value of the checkbox.
 * @param title The title of the item. Also used as the item's search text.
 * @param modifier Modifier applied to the item.
 * @param rememberState How the item's state is remembered.
 * @param enabled Whether the item is enabled, based on the current value.
 * @param icon The leading icon, based on the current value.
 * @param summary The summary text, based on the current value.
 * @param staticSummary A static summary used in the [buildSearchIndex] index.
 * @param style When non-null, the item is drawn in its own card of this style (a card is a
 * style, not a container); [CardStyle.None] is equivalent to null — plain row.
 * @param cardColor Card background color. Only applies with [style]; if null, the default
 * container color of [style] is used.
 * @param cardElevation Card elevation. Only applies to [CardStyle.Elevated].
 * @param cardBorder Card border. Only applies to [CardStyle.Outlined].
 * @param shape Card shape. Only applies with [style]; if null, `MaterialTheme.shapes.medium`.
 * @param outerPadding Clearance between the card and its container. Only applies with
 * [style]; if null, the theme's `horizontalSpacing` on the start/end and a small vertical
 * gap (2.dp) top/bottom.
 */
public inline fun LazyListScope.itemCheckbox(
    key: String,
    defaultValue: Boolean,
    title: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    crossinline rememberState: @Composable () -> MutableState<Boolean> = {
        rememberValue(key, defaultValue)
    },
    noinline enabled: (Boolean) -> Boolean = { true },
    noinline icon: @Composable ((Boolean) -> Unit)? = null,
    noinline summary: ((Boolean) -> String?)? = null,
    staticSummary: String? = null,
    style: CardStyle? = null,
    cardColor: Color? = null,
    cardElevation: CardElevation? = null,
    cardBorder: BorderStroke? = null,
    shape: Shape? = null,
    outerPadding: PaddingValues? = null,
) {
    // The icon depends on the value, which is only known in composition: the result
    // item re-reads the item's (store-backed) state to render it with the current value.
    SearchIndexer.record(
        key,
        title,
        staticSummary,
        icon?.let { itemIcon -> @Composable { itemIcon(rememberState().value) } },
    )
    item(key = key, contentType = "ItemCheckbox") {
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
            val state = rememberState()
            val value by state
            ItemCheckbox(
                state = state,
                title = title,
                modifier = rowModifier,
                enabled = enabled,
                icon = icon,
                summary = summary,
            )
        }
    }
}

/**
 * Adds a checkbox preference item to the lazy list, whose value is controlled by the caller.
 *
 * @param key The lazy list key of the item.
 * @param value The current value of the checkbox.
 * @param onValueChange Called when the checkbox is toggled.
 * @param title The title of the item. Also used as the item's search text.
 * @param modifier Modifier applied to the item.
 * @param enabled Whether the item is enabled.
 * @param icon The leading icon.
 * @param summary The summary text.
 * @param staticSummary A static summary used in the [buildSearchIndex] index. If null,
 * [summary] is used.
 * @param style When non-null, the item is drawn in its own card of this style (a card is a
 * style, not a container); [CardStyle.None] is equivalent to null — plain row.
 * @param cardColor Card background color. Only applies with [style]; if null, the default
 * container color of [style] is used.
 * @param cardElevation Card elevation. Only applies to [CardStyle.Elevated].
 * @param cardBorder Card border. Only applies to [CardStyle.Outlined].
 * @param shape Card shape. Only applies with [style]; if null, `MaterialTheme.shapes.medium`.
 * @param outerPadding Clearance between the card and its container. Only applies with
 * [style]; if null, the theme's `horizontalSpacing` on the start/end and a small vertical
 * gap (2.dp) top/bottom.
 */
public fun LazyListScope.itemCheckbox(
    key: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    title: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: String? = null,
    staticSummary: String? = null,
    style: CardStyle? = null,
    cardColor: Color? = null,
    cardElevation: CardElevation? = null,
    cardBorder: BorderStroke? = null,
    shape: Shape? = null,
    outerPadding: PaddingValues? = null,
) {
    SearchIndexer.record(key, title, staticSummary ?: summary, icon)
    item(key = key, contentType = "ItemCheckbox") {
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
            ItemCheckbox(
                value = value,
                onValueChange = onValueChange,
                title = title,
                modifier = rowModifier,
                enabled = enabled,
                icon = icon,
                summary = summary,
            )
        }
    }
}

/**
 * Adds a checkbox preference item to a [group] (a [GroupScope]): the row joins the group
 * and is drawn in the group's card — the group's `style`, `shape` and spacing apply, so
 * this overload takes no [modifier] and no card parameters. Parameter semantics match the
 * [itemCheckbox] ([LazyListScope]) overload.
 */
public fun GroupScope.itemCheckbox(
    key: String,
    defaultValue: Boolean,
    title: String,
    rememberState: @Composable () -> MutableState<Boolean> = { rememberValue(key, defaultValue) },
    enabled: (Boolean) -> Boolean = { true },
    icon: @Composable ((Boolean) -> Unit)? = null,
    summary: ((Boolean) -> String?)? = null,
    staticSummary: String? = null,
) {
    items.add(
        GroupItem(
            title = title,
            summary = staticSummary,
            icon = icon?.let { itemIcon -> @Composable { itemIcon(rememberState().value) } },
            content = {
                val state = rememberState()
                val value by state
                ItemCheckbox(
                    state = state,
                    title = title,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    icon = icon,
                    summary = summary,
                )
            },
        )
    )
}

/**
 * Adds a checkbox preference item to a [group] (a [GroupScope]); see the [itemCheckbox]
 * ([GroupScope]) overload for the contract.
 */
public fun GroupScope.itemCheckbox(
    key: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    title: String,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: String? = null,
    staticSummary: String? = null,
) {
    items.add(
        GroupItem(
            title = title,
            summary = staticSummary ?: summary,
            icon = icon,
            content = {
                ItemCheckbox(
                    value = value,
                    onValueChange = onValueChange,
                    title = title,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    icon = icon,
                    summary = summary,
                )
            },
        )
    )
}

/**
 * A checkbox preference item, whose state is a [MutableState].
 *
 * @param state The state of the checkbox.
 * @param title The title of the item.
 * @param modifier Modifier applied to the item.
 * @param enabled Whether the item is enabled.
 * @param icon The leading icon.
 * @param summary The summary text, based on the current value.
 */
@Composable
public fun ItemCheckbox(
    state: MutableState<Boolean>,
    title: String,
    modifier: Modifier = Modifier,
    enabled: (Boolean) -> Boolean = { true },
    icon: @Composable ((Boolean) -> Unit)? = null,
    summary: ((Boolean) -> String?)? = null,
) {
    var value by state
    ItemCheckbox(
        value = value,
        onValueChange = { value = it },
        title = title,
        modifier = modifier,
        enabled = enabled(value),
        icon = icon?.let { { it(value) } },
        summary = summary?.invoke(value),
    )
}

@Composable
public fun ItemCheckbox(
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: String? = null,
) {
    Item(
        title = title,
        modifier =
            modifier.toggleable(value, enabled, Role.Checkbox, onValueChange = onValueChange),
        enabled = enabled,
        icon = icon,
        summary = summary,
        widgetContainer = {
            val theme = LocalItemTheme.current
            Checkbox(
                checked = value,
                onCheckedChange = null,
                modifier = Modifier.padding(theme.padding.copy(start = theme.horizontalSpacing)),
                enabled = enabled,
            )
        },
    )
}
