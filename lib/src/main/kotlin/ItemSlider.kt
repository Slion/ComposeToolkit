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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Adds a slider preference item to the lazy list, whose state is remembered by [key].
 *
 * @param key The lazy list key of the item, and the preference state key.
 * @param defaultValue The initial value of the slider.
 * @param title The title of the item. Also used as the item's search text.
 * @param modifier Modifier applied to the item.
 * @param rememberState How the item's state is remembered.
 * @param valueRange The range of values the slider can take.
 * @param valueSteps The number of discrete steps; 0 for continuous.
 * @param rememberSliderState How the slider's state is remembered.
 * @param enabled Whether the item is enabled, based on the current value.
 * @param icon The leading icon, based on the current value.
 * @param summary The summary text, based on the current value, shown above the slider.
 * @param staticSummary A static summary used in the [buildSearchIndex] index.
 * @param valueText The text shown next to the slider, based on the current value.
 */
public inline fun LazyListScope.itemSlider(
    key: String,
    defaultValue: Float,
    title: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    crossinline rememberState: @Composable () -> MutableState<Float> = {
        rememberValue(key, defaultValue)
    },
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    valueSteps: Int = 0,
    crossinline rememberSliderState: @Composable (Float) -> MutableFloatState = {
        remember { mutableFloatStateOf(it) }
    },
    noinline enabled: (Float) -> Boolean = { true },
    noinline icon: @Composable ((Float) -> Unit)? = null,
    noinline summary: ((Float) -> String?)? = null,
    staticSummary: String? = null,
    noinline valueText: ((Float) -> String?)? = null,
) {
    // The icon depends on the value, which is only known in composition: the result
    // item re-reads the item's (store-backed) state to render it with the current value.
    SearchIndexer.record(
        key,
        title,
        staticSummary,
        icon?.let { itemIcon -> @Composable { itemIcon(rememberState().value) } },
    )
    item(key = key, contentType = "ItemSlider") {
        val state = rememberState()
        val value by state
        val sliderState = rememberSliderState(value)
        val sliderValue by sliderState
        ItemSlider(
            state = state,
            title = title,
            modifier = modifier.then(highlightedKeyModifier(key)),
            valueRange = valueRange,
            valueSteps = valueSteps,
            sliderState = sliderState,
            enabled = enabled,
            icon = icon,
            summary = summary,
            valueText = valueText,
        )
    }
}

public fun LazyListScope.itemSlider(
    key: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    sliderValue: Float,
    onSliderValueChange: (Float) -> Unit,
    title: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    valueSteps: Int = 0,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: String? = null,
    staticSummary: String? = null,
    valueText: ((Float) -> String?)? = null,
) {
    SearchIndexer.record(key, title, staticSummary ?: summary, icon)
    item(key = key, contentType = "ItemSlider") {
        ItemSlider(
            value = value,
            onValueChange = onValueChange,
            sliderValue = sliderValue,
            onSliderValueChange = onSliderValueChange,
            title = title,
            modifier = modifier.then(highlightedKeyModifier(key)),
            valueRange = valueRange,
            valueSteps = valueSteps,
            enabled = enabled,
            icon = icon,
            summary = summary,
            valueText = valueText,
        )
    }
}

/**
 * Adds a slider preference item to the lazy list, whose value is controlled by the caller.
 * Unlike the overload that takes a [sliderValue], the thumb position is the item's own
 * stable state (not keyed on [value]): keying it on [value] would re-init it on every
 * commit, which — with [live] committing while dragging — makes the thumb fight the finger
 * and the effect never settles.
 *
 * [valueText] (when non-null) is shown as a live **title postfix** (the formatted thumb
 * value, e.g. "Tint        30%"), not next to the slider.
 *
 * @param key The lazy list key of the item.
 * @param value The committed value.
 * @param onValueChange Called with a new value: on release always, and while dragging when
 *   [live] is on.
 * @param title The title of the item. Also used as the item's search text.
 * @param modifier Modifier applied to the item.
 * @param valueRange The range of values the slider can take.
 * @param valueSteps The number of discrete intermediate stops; 0 for continuous.
 * @param enabled Whether the item is enabled.
 * @param icon The leading icon.
 * @param summary The summary text.
 * @param staticSummary A static summary used in the [buildSearchIndex] index. If null,
 *   [summary] is used.
 * @param valueText Formats the live thumb value for the title postfix.
 * @param live Whether to commit while dragging (true) or only on release (false).
 */
public fun LazyListScope.itemSlider(
    key: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    title: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    valueSteps: Int = 0,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: String? = null,
    staticSummary: String? = null,
    valueText: ((Float) -> String?)? = null,
    live: Boolean = false,
) {
    SearchIndexer.record(key, title, staticSummary ?: summary, icon)
    item(key = key, contentType = "ItemSlider") {
        // The thumb position, its own stable state; see the KDoc.
        val thumb = remember { mutableFloatStateOf(value) }
        ItemSlider(
            value = value,
            onValueChange = onValueChange,
            sliderValue = thumb.floatValue,
            onSliderValueChange = {
                thumb.floatValue = it
                if (live) onValueChange(it)
            },
            title = title,
            modifier = modifier.then(highlightedKeyModifier(key)),
            valueRange = valueRange,
            valueSteps = valueSteps,
            enabled = enabled,
            icon = icon,
            summary = summary,
            titlePostfix =
                valueText?.let { text ->
                    @Composable { text(thumb.floatValue)?.let { Text(it) } }
                },
        )
    }
}

/**
 * A slider preference item, whose state is a [MutableState].
 */
@Composable
public fun ItemSlider(
    state: MutableState<Float>,
    title: String,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    valueSteps: Int = 0,
    sliderState: MutableFloatState = remember { mutableFloatStateOf(state.value) },
    enabled: (Float) -> Boolean = { true },
    icon: @Composable ((Float) -> Unit)? = null,
    summary: ((Float) -> String?)? = null,
    valueText: ((Float) -> String?)? = null,
) {
    var value by state
    var sliderValue by sliderState
    ItemSlider(
        value = value,
        onValueChange = { value = it },
        sliderValue = sliderValue,
        onSliderValueChange = { sliderValue = it },
        title = title,
        modifier = modifier,
        valueRange = valueRange,
        valueSteps = valueSteps,
        enabled = enabled(value),
        icon = icon?.let { { it(value) } },
        summary = summary?.invoke(value),
        valueText = valueText,
    )
}

@Composable
public fun ItemSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    sliderValue: Float,
    onSliderValueChange: (Float) -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    valueSteps: Int = 0,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: String? = null,
    valueText: ((Float) -> String?)? = null,
    titlePostfix: @Composable (() -> Unit)? = null,
) {
    // The live position of the thumb. It follows the finger while dragging and is synced
    // from [sliderValue] when the caller commits a new value and the slider is idle.
    val liveValue = remember { mutableFloatStateOf(sliderValue) }
    val isDragging = remember { mutableStateOf(false) }
    SideEffect {
        if (!isDragging.value && sliderValue != liveValue.floatValue) {
            liveValue.floatValue = sliderValue
        }
    }
    val theme = LocalItemTheme.current
    BasicItem(
        textContainer = {
            Column(
                modifier =
                    Modifier.padding(
                        theme.padding.copy(
                            start = if (icon != null) 0.dp else Dp.Unspecified,
                        )
                    )
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides
                        theme.titleColor.let {
                            if (enabled) it else it.copy(alpha = theme.disabledOpacity)
                        }
                ) {
                    ProvideTextStyle(value = theme.titleTextStyle) {
                        if (titlePostfix != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = title, modifier = Modifier.weight(1f))
                                Spacer(modifier = Modifier.width(theme.horizontalSpacing))
                                titlePostfix()
                            }
                        } else {
                            Text(text = title)
                        }
                    }
                }
                if (summary != null) {
                    CompositionLocalProvider(
                        LocalContentColor provides
                            theme.summaryColor.let {
                                if (enabled) it else it.copy(alpha = theme.disabledOpacity)
                            }
                    ) {
                        ProvideTextStyle(value = theme.summaryTextStyle) {
                            Text(text = summary)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // The thumb is driven by the live value, so it follows the finger while
                    // dragging; onValueChangeFinished() reads it back, which also covers the
                    // click case where no onValueChange() recomposition happens in between.
                    Slider(
                        value = liveValue.floatValue,
                        onValueChange = {
                            isDragging.value = true
                            liveValue.floatValue = it
                            onSliderValueChange(it)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = enabled,
                        valueRange = valueRange,
                        steps = valueSteps,
                        onValueChangeFinished = {
                            isDragging.value = false
                            onValueChange(liveValue.floatValue)
                        },
                    )
                    valueText?.let { text ->
                        val text = text(liveValue.floatValue)
                        if (text != null) {
                            Box(modifier = Modifier.padding(start = theme.horizontalSpacing)) {
                                Text(text = text)
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier,
        enabled = enabled,
        iconContainer = {
            if (icon != null) {
                Row(
                    modifier =
                        Modifier.widthIn(min = theme.iconContainerMinWidth)
                            .padding(theme.padding.copy(end = 0.dp)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides
                            theme.iconColor.let {
                                if (enabled) it else it.copy(alpha = theme.disabledOpacity)
                            },
                        content = icon,
                    )
                    Spacer(modifier = Modifier.width(theme.horizontalSpacing))
                }
            }
        },
    )
}
