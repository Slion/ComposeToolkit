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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.CardElevation
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

public inline fun <T> LazyListScope.itemTextField(
    key: String,
    defaultValue: T,
    title: String,
    noinline textToValue: (String) -> T?,
    modifier: Modifier = Modifier.fillMaxWidth(),
    crossinline rememberState: @Composable () -> MutableState<T> = {
        rememberValue(key, defaultValue)
    },
    noinline enabled: (T) -> Boolean = { true },
    noinline icon: @Composable ((T) -> Unit)? = null,
    noinline summary: ((T) -> String?)? = null,
    staticSummary: String? = null,
    noinline valueToText: (T) -> String = { it.toString() },
    noinline textField:
        @Composable
        (value: TextFieldValue, onValueChange: (TextFieldValue) -> Unit, onOk: () -> Unit) -> Unit =
        ItemTextFieldDefaults.TextField,
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
    item(key = key, contentType = "ItemTextField") {
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
            ItemTextField(
                state = state,
                title = title,
                textToValue = textToValue,
                modifier = rowModifier,
                enabled = enabled,
                icon = icon,
                summary = summary,
                valueToText = valueToText,
                textField = textField,
            )
        }
    }
}

public fun <T> LazyListScope.itemTextField(
    key: String,
    value: T,
    onValueChange: (T) -> Unit,
    title: String,
    textToValue: (String) -> T?,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: String? = null,
    staticSummary: String? = null,
    valueToText: (T) -> String = { it.toString() },
    textField:
        @Composable
        (value: TextFieldValue, onValueChange: (TextFieldValue) -> Unit, onOk: () -> Unit) -> Unit =
        ItemTextFieldDefaults.TextField,
    style: CardStyle? = null,
    cardColor: Color? = null,
    cardElevation: CardElevation? = null,
    cardBorder: BorderStroke? = null,
    shape: Shape? = null,
    outerPadding: PaddingValues? = null,
) {
    SearchIndexer.record(key, title, staticSummary ?: summary, icon)
    item(key = key, contentType = "ItemTextField") {
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
            ItemTextField(
                value = value,
                onValueChange = onValueChange,
                title = title,
                textToValue = textToValue,
                modifier = rowModifier,
                enabled = enabled,
                icon = icon,
                summary = summary,
                valueToText = valueToText,
                textField = textField,
            )
        }
    }
}

/**
 * Adds a text-field preference item to a [group] (a [GroupScope]): the row joins the group
 * and is drawn in the group's card — the group's `style`, `shape` and spacing apply, so
 * this overload takes no [modifier] and no card parameters. Parameter semantics match the
 * [itemTextField] ([LazyListScope]) overloads.
 */
public fun <T> GroupScope.itemTextField(
    key: String,
    defaultValue: T,
    title: String,
    textToValue: (String) -> T?,
    rememberState: @Composable () -> MutableState<T> = { rememberValue(key, defaultValue) },
    enabled: (T) -> Boolean = { true },
    icon: @Composable ((T) -> Unit)? = null,
    summary: ((T) -> String?)? = null,
    staticSummary: String? = null,
    valueToText: (T) -> String = { it.toString() },
    textField:
        @Composable
        (value: TextFieldValue, onValueChange: (TextFieldValue) -> Unit, onOk: () -> Unit) -> Unit =
        ItemTextFieldDefaults.TextField,
) {
    items.add(
        GroupItem(
            title = title,
            summary = staticSummary,
            icon = icon?.let { itemIcon -> @Composable { itemIcon(rememberState().value) } },
            content = {
                val state = rememberState()
                val value by state
                ItemTextField(
                    state = state,
                    title = title,
                    textToValue = textToValue,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    icon = icon,
                    summary = summary,
                    valueToText = valueToText,
                    textField = textField,
                )
            },
        )
    )
}

/**
 * Adds a text-field preference item to a [group] (a [GroupScope]); see the [itemTextField]
 * ([GroupScope]) overload for the contract.
 */
public fun <T> GroupScope.itemTextField(
    key: String,
    value: T,
    onValueChange: (T) -> Unit,
    title: String,
    textToValue: (String) -> T?,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: String? = null,
    staticSummary: String? = null,
    valueToText: (T) -> String = { it.toString() },
    textField:
        @Composable
        (value: TextFieldValue, onValueChange: (TextFieldValue) -> Unit, onOk: () -> Unit) -> Unit =
        ItemTextFieldDefaults.TextField,
) {
    items.add(
        GroupItem(
            title = title,
            summary = staticSummary ?: summary,
            icon = icon,
            content = {
                ItemTextField(
                    value = value,
                    onValueChange = onValueChange,
                    title = title,
                    textToValue = textToValue,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    icon = icon,
                    summary = summary,
                    valueToText = valueToText,
                    textField = textField,
                )
            },
        )
    )
}

@Composable
public fun <T> ItemTextField(
    state: MutableState<T>,
    title: String,
    textToValue: (String) -> T?,
    modifier: Modifier = Modifier,
    enabled: (T) -> Boolean = { true },
    icon: @Composable ((T) -> Unit)? = null,
    summary: ((T) -> String?)? = null,
    valueToText: (T) -> String = { it.toString() },
    textField:
        @Composable
        (value: TextFieldValue, onValueChange: (TextFieldValue) -> Unit, onOk: () -> Unit) -> Unit =
        ItemTextFieldDefaults.TextField,
) {
    var value by state
    ItemTextField(
        value = value,
        onValueChange = { value = it },
        title = title,
        textToValue = textToValue,
        modifier = modifier,
        enabled = enabled(value),
        icon = icon?.let { { it(value) } },
        summary = summary?.invoke(value),
        valueToText = valueToText,
        textField = textField,
    )
}

@Composable
public fun <T> ItemTextField(
    value: T,
    onValueChange: (T) -> Unit,
    title: String,
    textToValue: (String) -> T?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: String? = null,
    valueToText: (T) -> String = { it.toString() },
    textField:
        @Composable
        (value: TextFieldValue, onValueChange: (TextFieldValue) -> Unit, onOk: () -> Unit) -> Unit =
        ItemTextFieldDefaults.TextField,
) {
    var openDialog by rememberSaveable { mutableStateOf(false) }
    Item(
        title = title,
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        summary = summary,
    ) {
        openDialog = true
    }
    if (openDialog) {
        var dialogText by
            rememberSaveable(stateSaver = TextFieldValue.Saver) {
                val text = valueToText(value)
                mutableStateOf(TextFieldValue(text, TextRange(text.length)))
            }
        val onOk = {
            val dialogValue = textToValue(dialogText.text)
            if (dialogValue != null) {
                onValueChange(dialogValue)
                openDialog = false
            }
        }
        PreferenceAlertDialog(
            onDismissRequest = { openDialog = false },
            title = { Text(text = title) },
            buttons = {
                TextButton(onClick = { openDialog = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
                TextButton(onClick = onOk) { Text(text = stringResource(R.string.ok)) }
            },
        ) {
            val focusRequester = remember { FocusRequester() }
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .focusRequester(focusRequester)
            ) {
                textField(dialogText, { dialogText = it }, onOk)
            }
            LaunchedEffect(focusRequester) { focusRequester.requestFocus() }
        }
    }
}

@PublishedApi
internal object ItemTextFieldDefaults {
    val TextField:
        @Composable
        (value: TextFieldValue, onValueChange: (TextFieldValue) -> Unit, onOk: () -> Unit) -> Unit =
        { value, onValueChange, onOk ->
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                keyboardActions = KeyboardActions { onOk() },
                singleLine = true,
            )
        }
}
