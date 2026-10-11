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
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

public inline fun LazyListScope.itemActionsSwitch(
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
    noinline switchEnabled: (Boolean) -> Boolean = enabled,
    noinline onClick: ((Boolean) -> Unit)? = null,
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
    item(key = key, contentType = "ItemActionsSwitch") {
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
            ItemActionsSwitch(
                state = state,
                title = title,
                modifier = rowModifier,
                enabled = enabled,
                icon = icon,
                summary = summary,
                switchEnabled = switchEnabled,
                onClick = onClick,
            )
        }
    }
}

public fun LazyListScope.itemActionsSwitch(
    key: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    title: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: String? = null,
    staticSummary: String? = null,
    switchEnabled: Boolean = enabled,
    onClick: (() -> Unit)? = null,
    style: CardStyle? = null,
    cardColor: Color? = null,
    cardElevation: CardElevation? = null,
    cardBorder: BorderStroke? = null,
    shape: Shape? = null,
    outerPadding: PaddingValues? = null,
) {
    SearchIndexer.record(key, title, staticSummary ?: summary, icon)
    item(key = key, contentType = "ItemActionsSwitch") {
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
            ItemActionsSwitch(
                value = value,
                onValueChange = onValueChange,
                title = title,
                modifier = rowModifier,
                enabled = enabled,
                icon = icon,
                summary = summary,
                switchEnabled = switchEnabled,
                onClick = onClick,
            )
        }
    }
}

/**
 * Adds an actions switch item to a [group] (a [GroupScope]): the row joins the group and is
 * drawn in the group's card — the group's `style`, `shape` and spacing apply, so this
 * overload takes no [modifier] and no card parameters. Parameter semantics match the
 * [itemActionsSwitch] ([LazyListScope]) overloads.
 */
public fun GroupScope.itemActionsSwitch(
    key: String,
    defaultValue: Boolean,
    title: String,
    rememberState: @Composable () -> MutableState<Boolean> = { rememberValue(key, defaultValue) },
    enabled: (Boolean) -> Boolean = { true },
    icon: @Composable ((Boolean) -> Unit)? = null,
    summary: ((Boolean) -> String?)? = null,
    staticSummary: String? = null,
    switchEnabled: (Boolean) -> Boolean = enabled,
    onClick: ((Boolean) -> Unit)? = null,
) {
    items.add(
        GroupItem(
            title = title,
            summary = staticSummary,
            icon = icon?.let { itemIcon -> @Composable { itemIcon(rememberState().value) } },
            content = {
                val state = rememberState()
                val value by state
                ItemActionsSwitch(
                    state = state,
                    title = title,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    icon = icon,
                    summary = summary,
                    switchEnabled = switchEnabled,
                    onClick = onClick,
                )
            },
        )
    )
}

/**
 * Adds an actions switch item to a [group] (a [GroupScope]); see the [itemActionsSwitch]
 * ([GroupScope]) overload for the contract.
 */
public fun GroupScope.itemActionsSwitch(
    key: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    title: String,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: String? = null,
    staticSummary: String? = null,
    switchEnabled: Boolean = enabled,
    onClick: (() -> Unit)? = null,
) {
    items.add(
        GroupItem(
            title = title,
            summary = staticSummary ?: summary,
            icon = icon,
            content = {
                ItemActionsSwitch(
                    value = value,
                    onValueChange = onValueChange,
                    title = title,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    icon = icon,
                    summary = summary,
                    switchEnabled = switchEnabled,
                    onClick = onClick,
                )
            },
        )
    )
}

@Composable
public fun ItemActionsSwitch(
    state: MutableState<Boolean>,
    title: String,
    modifier: Modifier = Modifier,
    enabled: (Boolean) -> Boolean = { true },
    icon: @Composable ((Boolean) -> Unit)? = null,
    summary: ((Boolean) -> String?)? = null,
    switchEnabled: (Boolean) -> Boolean = { true },
    onClick: ((Boolean) -> Unit)? = null,
) {
    var value by state
    ItemActionsSwitch(
        value = value,
        onValueChange = { value = it },
        title = title,
        modifier = modifier,
        enabled = enabled(value),
        icon = icon?.let { { it(value) } },
        summary = summary?.invoke(value),
        switchEnabled = switchEnabled(value),
        onClick = onClick?.let { { it(value) } },
    )
}

@Composable
public fun ItemActionsSwitch(
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: String? = null,
    switchEnabled: Boolean = enabled,
    onClick: (() -> Unit)? = null,
) {
    ItemActions(
        title = title,
        secondTarget = {
            val theme = LocalItemTheme.current
            Switch(
                checked = value,
                onCheckedChange = onValueChange,
                modifier =
                    Modifier.padding(
                        theme.padding
                            .copy(start = theme.horizontalSpacing)
                            .offset(vertical = (-8).dp)
                    ),
                enabled = switchEnabled,
            )
        },
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        summary = summary,
        onClick = onClick,
    )
}
