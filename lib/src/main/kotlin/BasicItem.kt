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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

/**
 * The `key` of the preference item that should currently be highlighted (tinted with the theme's
 * `primaryContainer`), or null when no item is highlighted.
 *
 * Hosts (e.g. a search that navigates to a page) provide this for the duration of the highlight;
 * every preference item reads it and highlights itself when its lazy list key matches.
 */
public val LocalHighlightedItemKey: ProvidableCompositionLocal<String?> =
    compositionLocalOf { null }

/**
 * A [Modifier] that highlights the preference item when its lazy list `key` matches
 * [LocalHighlightedItemKey]: the item is tinted with the theme's `primaryContainer` color
 * and flashed with a translucent [androidx.compose.material3.MaterialTheme.colorScheme.primary]
 * overlay that fades smoothly in and out several times (a gentle pulse), so it stands out
 * while the list scrolls to it.
 *
 * Every lazy `*Item` extension applies this to its item, so a host can highlight a single
 * item (e.g. the entry a search navigated to) by providing [LocalHighlightedItemKey] with
 * the item's key for a short duration.
 */
@Composable
public fun highlightedKeyModifier(key: String?): Modifier {
    if (key != LocalHighlightedItemKey.current) return Modifier
    // The highlighted item takes focus: the search field is unfocused, the keyboard
    // closes, and on d-pad and TV the focus ring lands on the item as well.
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    // The static tint alone is easy to miss while the list is still scrolling, so the
    // item also pulses with a smooth fade in and out when the highlight starts. Both the
    // tint and the flash are driven by [Animatable]s and are guaranteed to reach fully
    // transparent before the host clears the highlight, so the highlight always lifts
    // smoothly instead of being cut off mid-animation.
    val tint = remember { Animatable(1f) }
    val flash = remember { Animatable(0f) }
    val tintColor = MaterialTheme.colorScheme.primaryContainer
    val flashColor = MaterialTheme.colorScheme.primary
    // How many times the item pulses (fades in and then out) while it is highlighted.
    val pulseCount = 2
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.hide()
        // One pulse is a full breath: a continuous fade in and then a fade out back to
        // fully transparent. The alpha follows a raised cosine (a raised full sine) so it
        // eases up from rest to a low peak and eases back down to rest with no hard snap
        // at either end. The curve is sampled into keyframes (linear fill) so it plays as
        // one slow, smooth breath rather than two separate tweens. The peak is kept low
        // on purpose: the flash is a subtle glow that tints the item, not a wash that covers
        // the item's content. The pulse repeats [pulseCount] times so the item keeps
        // standing out while the list scrolls to it.
        val pulse =
            keyframes {
                durationMillis = 1400
                0f at 0
                0.037f at 175
                0.125f at 350
                0.213f at 525
                0.25f at 700
                0.213f at 875
                0.125f at 1050
                0.037f at 1225
                0f at 1400
            }
        repeat(pulseCount) { flash.animateTo(targetValue = 0f, animationSpec = pulse) }
        // After the last pulse, ease the item's tint out to fully transparent so the
        // highlight fades away rather than popping the moment the host clears the key.
        tint.animateTo(0f, tween(600, easing = FastOutSlowInEasing))
    }
    return Modifier
        .focusable()
        .focusRequester(focusRequester)
        .drawWithContent {
            // The tint is a *background* for the item: draw it first so the item's own
            // content (text, icon, widget) stays visible on top of it.
            if (tint.value > 0f) drawRect(color = tintColor.copy(alpha = tint.value))
            drawContent()
            // The flash is a subtle glow drawn over the content as a brief pulse.
            if (flash.value > 0f) drawRect(color = flashColor.copy(alpha = flash.value))
        }
}

public fun LazyListScope.basicItem(
    key: String,
    textContainer: @Composable () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    iconContainer: @Composable () -> Unit = {},
    actionIconContainer: @Composable () -> Unit = {},
    widgetContainer: @Composable () -> Unit = {},
    onClick: (() -> Unit)? = null,
) {
    item(key = key, contentType = "BasicItem") {
        BasicItem(
            textContainer = textContainer,
            modifier = modifier.then(highlightedKeyModifier(key)),
            enabled = enabled,
            iconContainer = iconContainer,
            actionIconContainer = actionIconContainer,
            widgetContainer = widgetContainer,
            onClick = onClick,
        )
    }
}

/**
 * A basic preference item: an icon, a text block, an action icon, and a widget, laid out
 * in a single line.
 *
 * @param textContainer The title and (optionally) summary block.
 * @param modifier Modifier applied to the item.
 * @param enabled Whether the preference is enabled.
 * @param iconContainer The leading icon.
 * @param actionIconContainer The action icon, shown just before the widget.
 * @param widgetContainer The trailing widget (e.g. a switch or checkbox).
 * @param onClick Click handler; when null, the item is not clickable.
 */
@Composable
public fun BasicItem(
    textContainer: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconContainer: @Composable () -> Unit = {},
    actionIconContainer: @Composable () -> Unit = {},
    widgetContainer: @Composable () -> Unit = {},
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier =
            modifier.then(
                if (onClick != null) {
                    Modifier.clickable(enabled, onClick = onClick)
                } else {
                    Modifier
                }
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        iconContainer()
        Box(modifier = Modifier.weight(1f)) { textContainer() }
        actionIconContainer()
        widgetContainer()
    }
}
