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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.CardElevation
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp

public enum class ItemListType {
    ALERT_DIALOG,
    DROPDOWN_MENU,
}

/**
 * Adds a list preference item to the lazy list, whose state is remembered by [key].
 *
 * @param key The lazy list key of the item, and the preference state key.
 * @param defaultValue The initial selected value.
 * @param values The values the user can choose from.
 * @param title The title of the item. Also used as the item's search text.
 * @param modifier Modifier applied to the item.
 * @param rememberState How the item's state is remembered.
 * @param enabled Whether the item is enabled, based on the current value.
 * @param icon The leading icon, based on the current value.
 * @param summary The summary text, based on the current value.
 * @param staticSummary A static summary used in the [buildSearchIndex] index.
 * @param type The type of the list, either an alert dialog or a dropdown menu.
 * @param valueToText How a value is rendered in the list.
 * @param item How a list item is rendered.
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
public inline fun <T> LazyListScope.itemList(
    key: String,
    defaultValue: T,
    values: List<T>,
    title: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    crossinline rememberState: @Composable () -> MutableState<T> = {
        rememberValue(key, defaultValue)
    },
    noinline enabled: (T) -> Boolean = { true },
    noinline icon: @Composable ((T) -> Unit)? = null,
    noinline summary: ((T) -> String?)? = null,
    staticSummary: String? = null,
    type: ItemListType = ItemListType.ALERT_DIALOG,
    noinline valueToText: @Composable (T) -> AnnotatedString = { AnnotatedString(it.toString()) },
    noinline item: @Composable (value: T, currentValue: T, onClick: () -> Unit) -> Unit =
        ItemListDefaults.item(type, valueToText),
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
    item(key = key, contentType = "ItemList") {
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
            ItemList(
                state = state,
                values = values,
                title = title,
                modifier = rowModifier,
                enabled = enabled,
                icon = icon,
                summary = summary,
                type = type,
                valueToText = valueToText,
                item = item,
            )
        }
    }
}

/**
 * Adds a list preference item to the lazy list, whose value is controlled by the caller.
 *
 * @param key The lazy list key of the item.
 * @param value The current selected value.
 * @param onValueChange Called when a new value is selected.
 * @param values The values the user can choose from.
 * @param title The title of the item. Also used as the item's search text.
 * @param modifier Modifier applied to the item.
 * @param enabled Whether the item is enabled.
 * @param icon The leading icon.
 * @param summary The summary text, based on the current value.
 * @param staticSummary A static summary used in the [buildSearchIndex] index.
 * @param type The type of the list, either an alert dialog or a dropdown menu.
 * @param valueToText How a value is rendered in the list.
 * @param item How a list item is rendered.
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
public fun <T> LazyListScope.itemList(
    key: String,
    value: T,
    onValueChange: (T) -> Unit,
    values: List<T>,
    title: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: String? = null,
    staticSummary: String? = null,
    type: ItemListType = ItemListType.ALERT_DIALOG,
    valueToText: @Composable (T) -> AnnotatedString = { AnnotatedString(it.toString()) },
    item: @Composable (value: T, currentValue: T, onClick: () -> Unit) -> Unit =
        ItemListDefaults.item(type, valueToText),
    style: CardStyle? = null,
    cardColor: Color? = null,
    cardElevation: CardElevation? = null,
    cardBorder: BorderStroke? = null,
    shape: Shape? = null,
    outerPadding: PaddingValues? = null,
) {
    SearchIndexer.record(key, title, staticSummary ?: summary, icon)
    item(key = key, contentType = "ItemList") {
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
            ItemList(
                value = value,
                onValueChange = onValueChange,
                values = values,
                title = title,
                modifier = rowModifier,
                enabled = enabled,
                icon = icon,
                summary = summary,
                type = type,
                valueToText = valueToText,
                item = item,
            )
        }
    }
}

/**
 * Adds a list preference item to a [group] (a [GroupScope]): the row joins the group and is
 * drawn in the group's card — the group's `style`, `shape` and spacing apply, so this
 * overload takes no [modifier] and no card parameters. Parameter semantics match the
 * [itemList] ([LazyListScope]) overloads.
 */
public fun <T> GroupScope.itemList(
    key: String,
    defaultValue: T,
    values: List<T>,
    title: String,
    rememberState: @Composable () -> MutableState<T> = { rememberValue(key, defaultValue) },
    enabled: (T) -> Boolean = { true },
    icon: @Composable ((T) -> Unit)? = null,
    summary: ((T) -> String?)? = null,
    staticSummary: String? = null,
    type: ItemListType = ItemListType.ALERT_DIALOG,
    valueToText: @Composable (T) -> AnnotatedString = { AnnotatedString(it.toString()) },
    item: @Composable (value: T, currentValue: T, onClick: () -> Unit) -> Unit =
        ItemListDefaults.item(type, valueToText),
) {
    items.add(
        GroupItem(
            title = title,
            summary = staticSummary,
            icon = icon?.let { itemIcon -> @Composable { itemIcon(rememberState().value) } },
            content = {
                val state = rememberState()
                val value by state
                ItemList(
                    state = state,
                    values = values,
                    title = title,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    icon = icon,
                    summary = summary,
                    type = type,
                    valueToText = valueToText,
                    item = item,
                )
            },
        )
    )
}

/**
 * Adds a list preference item to a [group] (a [GroupScope]); see the [itemList]
 * ([GroupScope]) overload for the contract.
 */
public fun <T> GroupScope.itemList(
    key: String,
    value: T,
    onValueChange: (T) -> Unit,
    values: List<T>,
    title: String,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: String? = null,
    staticSummary: String? = null,
    type: ItemListType = ItemListType.ALERT_DIALOG,
    valueToText: @Composable (T) -> AnnotatedString = { AnnotatedString(it.toString()) },
    item: @Composable (value: T, currentValue: T, onClick: () -> Unit) -> Unit =
        ItemListDefaults.item(type, valueToText),
) {
    items.add(
        GroupItem(
            title = title,
            summary = staticSummary ?: summary,
            icon = icon,
            content = {
                ItemList(
                    value = value,
                    onValueChange = onValueChange,
                    values = values,
                    title = title,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    icon = icon,
                    summary = summary,
                    type = type,
                    valueToText = valueToText,
                    item = item,
                )
            },
        )
    )
}

@Composable
public fun <T> ItemList(
    state: MutableState<T>,
    values: List<T>,
    title: String,
    modifier: Modifier = Modifier,
    enabled: (T) -> Boolean = { true },
    icon: @Composable ((T) -> Unit)? = null,
    summary: ((T) -> String?)? = null,
    type: ItemListType = ItemListType.ALERT_DIALOG,
    valueToText: @Composable (T) -> AnnotatedString = { AnnotatedString(it.toString()) },
    item: @Composable (value: T, currentValue: T, onClick: () -> Unit) -> Unit =
        ItemListDefaults.item(type, valueToText),
) {
    var value by state
    ItemList(
        value = value,
        onValueChange = { value = it },
        values = values,
        title = title,
        modifier = modifier,
        enabled = enabled(value),
        icon = icon?.let { { it(value) } },
        summary = summary?.invoke(value),
        type = type,
        valueToText = valueToText,
        item = item,
    )
}

@Composable
public fun <T> ItemList(
    value: T,
    onValueChange: (T) -> Unit,
    values: List<T>,
    title: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: String? = null,
    type: ItemListType = ItemListType.ALERT_DIALOG,
    valueToText: @Composable (T) -> AnnotatedString = { AnnotatedString(it.toString()) },
    item: @Composable (value: T, currentValue: T, onClick: () -> Unit) -> Unit =
        ItemListDefaults.item(type, valueToText),
) {
    var openSelector by rememberSaveable { mutableStateOf(false) }
    // Put DropdownMenu before Item so that it can anchor to the right position.
    if (openSelector) {
        when (type) {
            ItemListType.ALERT_DIALOG -> {
                PreferenceAlertDialog(
                    onDismissRequest = { openSelector = false },
                    title = { Text(text = title) },
                    buttons = {
                        TextButton(onClick = { openSelector = false }) {
                            Text(text = stringResource(R.string.cancel))
                        }
                    },
                ) {
                    val lazyListState = rememberLazyListState()
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().verticalScrollIndicators(lazyListState),
                        state = lazyListState,
                    ) {
                        items(values) { itemValue ->
                            item(itemValue, value) {
                                onValueChange(itemValue)
                                openSelector = false
                            }
                        }
                    }
                }
            }
            ItemListType.DROPDOWN_MENU -> {
                val theme = LocalItemTheme.current
                Box(
                    modifier = Modifier.fillMaxWidth().padding(theme.padding.copy(vertical = 0.dp))
                ) {
                    DropdownMenu(
                        expanded = openSelector,
                        onDismissRequest = { openSelector = false },
                    ) {
                        for (itemValue in values) {
                            item(itemValue, value) {
                                onValueChange(itemValue)
                                openSelector = false
                            }
                        }
                    }
                }
            }
        }
    }
    Item(
        title = title,
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        summary = summary,
    ) {
        openSelector = true
    }
}

@PublishedApi
internal object ItemListDefaults {
    fun <T> item(
        type: ItemListType,
        valueToText: @Composable (T) -> AnnotatedString,
    ): @Composable (value: T, currentValue: T, onClick: () -> Unit) -> Unit =
        when (type) {
            ItemListType.ALERT_DIALOG -> {
                { value, currentValue, onClick ->
                    DialogItem(value, currentValue, valueToText, onClick)
                }
            }
            ItemListType.DROPDOWN_MENU -> {
                { value, currentValue, onClick ->
                    DropdownMenuItem(value, currentValue, valueToText, onClick)
                }
            }
        }

    @Composable
    private fun <T> DialogItem(
        value: T,
        currentValue: T,
        valueToText: @Composable (T) -> AnnotatedString,
        onClick: () -> Unit,
    ) {
        val selected = value == currentValue
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .selectable(selected, true, Role.RadioButton, onClick = onClick)
                    .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = null)
            Spacer(modifier = Modifier.width(24.dp))
            Text(
                text = valueToText(value),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }

    @Composable
    private fun <T> DropdownMenuItem(
        value: T,
        currentValue: T,
        valueToText: @Composable (T) -> AnnotatedString,
        onClick: () -> Unit,
    ) {
        DropdownMenuItem(
            text = { Text(text = valueToText(value)) },
            onClick = onClick,
            modifier =
                if (value == currentValue) {
                    Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest)
                } else {
                    Modifier
                },
            colors = MenuDefaults.itemColors(),
        )
    }
}
