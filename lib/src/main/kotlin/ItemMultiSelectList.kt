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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

public inline fun <T> LazyListScope.itemMultiSelectList(
    key: String,
    defaultValue: Set<T>,
    values: List<T>,
    title: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    crossinline rememberState: @Composable () -> MutableState<Set<T>> = {
        rememberValue(key, defaultValue)
    },
    noinline enabled: (Set<T>) -> Boolean = { true },
    noinline icon: @Composable ((Set<T>) -> Unit)? = null,
    noinline summary: ((Set<T>) -> String?)? = null,
    staticSummary: String? = null,
    noinline valueToText: @Composable (T) -> AnnotatedString = { AnnotatedString(it.toString()) },
    noinline item:
        @Composable
        (value: T, currentValues: Set<T>, onToggle: (Boolean) -> Unit) -> Unit =
        ItemMultiSelectListDefaults.item(valueToText),
) {
    // The icon depends on the value, which is only known in composition: the result
    // item re-reads the item's (store-backed) state to render it with the current value.
    SearchIndexer.record(
        key,
        title,
        staticSummary,
        icon?.let { itemIcon -> @Composable { itemIcon(rememberState().value) } },
    )
    item(key = key, contentType = "ItemMultiSelectList") {
        val state = rememberState()
        val value by state
        ItemMultiSelectList(
            state = state,
            values = values,
            title = title,
            modifier = modifier.then(highlightedKeyModifier(key)),
            enabled = enabled,
            icon = icon,
            summary = summary,
            valueToText = valueToText,
            item = item,
        )
    }
}

public fun <T> LazyListScope.itemMultiSelectList(
    key: String,
    value: Set<T>,
    onValueChange: (Set<T>) -> Unit,
    values: List<T>,
    title: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: String? = null,
    staticSummary: String? = null,
    valueToText: @Composable (T) -> AnnotatedString = { AnnotatedString(it.toString()) },
    item: @Composable (value: T, currentValues: Set<T>, onToggle: (Boolean) -> Unit) -> Unit =
        ItemMultiSelectListDefaults.item(valueToText),
) {
    SearchIndexer.record(key, title, staticSummary ?: summary, icon)
    item(key = key, contentType = "ItemMultiSelectList") {
        ItemMultiSelectList(
            value = value,
            onValueChange = onValueChange,
            values = values,
            title = title,
            modifier = modifier.then(highlightedKeyModifier(key)),
            enabled = enabled,
            icon = icon,
            summary = summary,
            valueToText = valueToText,
            item = item,
        )
    }
}

@Composable
public fun <T> ItemMultiSelectList(
    state: MutableState<Set<T>>,
    values: List<T>,
    title: String,
    modifier: Modifier = Modifier,
    enabled: (Set<T>) -> Boolean = { true },
    icon: @Composable ((Set<T>) -> Unit)? = null,
    summary: ((Set<T>) -> String?)? = null,
    valueToText: @Composable (T) -> AnnotatedString = { AnnotatedString(it.toString()) },
    item: @Composable (value: T, currentValues: Set<T>, onToggle: (Boolean) -> Unit) -> Unit =
        ItemMultiSelectListDefaults.item(valueToText),
) {
    var value by state
    ItemMultiSelectList(
        value = value,
        onValueChange = { value = it },
        values = values,
        title = title,
        modifier = modifier,
        enabled = enabled(value),
        icon = icon?.let { { it(value) } },
        summary = summary?.invoke(value),
        valueToText = valueToText,
        item = item,
    )
}

@Composable
public fun <T> ItemMultiSelectList(
    value: Set<T>,
    onValueChange: (Set<T>) -> Unit,
    values: List<T>,
    title: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: String? = null,
    valueToText: @Composable (T) -> AnnotatedString = { AnnotatedString(it.toString()) },
    item: @Composable (value: T, currentValues: Set<T>, onToggle: (Boolean) -> Unit) -> Unit =
        ItemMultiSelectListDefaults.item(valueToText),
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
        var dialogValue by rememberSaveable { mutableStateOf(value) }
        PreferenceAlertDialog(
            onDismissRequest = { openDialog = false },
            title = { Text(text = title) },
            buttons = {
                TextButton(onClick = { openDialog = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
                TextButton(
                    onClick = {
                        onValueChange(dialogValue)
                        openDialog = false
                    }
                ) {
                    Text(text = stringResource(R.string.ok))
                }
            },
        ) {
            val lazyListState = rememberLazyListState()
            LazyColumn(
                modifier = Modifier.fillMaxWidth().verticalScrollIndicators(lazyListState),
                state = lazyListState,
            ) {
                items(values) { itemValue ->
                    item(itemValue, dialogValue) {
                        dialogValue = if (it) dialogValue + itemValue else dialogValue - itemValue
                    }
                }
            }
        }
    }
}

@PublishedApi
internal object ItemMultiSelectListDefaults {
    fun <T> item(
        valueToText: @Composable (T) -> AnnotatedString
    ): @Composable (value: T, currentValues: Set<T>, onToggle: (Boolean) -> Unit) -> Unit =
        { value, currentValues, onToggle ->
            Item(value, currentValues, valueToText, onToggle)
        }

    @Composable
    private fun <T> Item(
        value: T,
        currentValues: Set<T>,
        valueToText: @Composable (T) -> AnnotatedString,
        onToggle: (Boolean) -> Unit,
    ) {
        val checked = value in currentValues
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .toggleable(checked, true, Role.Checkbox, onValueChange = onToggle)
                    .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = checked, onCheckedChange = null)
            Spacer(modifier = Modifier.width(24.dp))
            Text(
                text = valueToText(value),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
