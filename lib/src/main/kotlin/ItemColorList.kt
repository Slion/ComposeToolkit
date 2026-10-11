/*
 * Copyright 2026 Stéphane Lenclud
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CardElevation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * A selectable color option for [ItemColorList], in display order.
 *
 * @property name The option's label (e.g. "Blue").
 * @property hex The option's color as a hex string ("#RRGGBB" or "#AARRGGBB"); `null`
 *   for the "default" option, whose swatch is drawn with [ItemColorList]'s
 *   `defaultOptionColor` (falling back to the theme's primary). The first entry is
 *   typically the default.
 */
public data class ColorOption(
    val name: String,
    val hex: String? = null,
)

/**
 * Adds a color-picker preference item to the lazy list: a row with a swatch of the
 * selected color right-aligned, which opens an alert dialog of radio-button swatch rows.
 *
 * @param key The lazy list key of the item.
 * @param value The index of the selected option in [options].
 * @param onValueChange Called with the index of the selected option.
 * @param options The selectable colors, in display order.
 * @param title The title of the item. Also used as the item's search text.
 * @param modifier Modifier applied to the item.
 * @param enabled Whether the item is enabled.
 * @param icon The leading icon.
 * @param summary The summary text.
 * @param staticSummary A static summary used in the [buildSearchIndex] index. If null,
 *   [summary] is used.
 * @param dialogCancelLabel Text for the dialog's cancel button; if null, the library's
 *   default cancel label is used.
 * @param defaultOptionColor The color to show for the "default" option (a [ColorOption]
 *   with a null [ColorOption.hex]) in both the row swatch and the dialog. When the
 *   default option is selected the app's own theme may be re-derived (e.g. from the
 *   wallpaper), so the *current* theme's primary is not a reliable stand-in for "what the
 *   system will use": pass the platform's actual default accent when it differs. When
 *   null, the current theme's primary is used.
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
public fun LazyListScope.itemColorList(
    key: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    options: List<ColorOption>,
    title: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: String? = null,
    staticSummary: String? = null,
    dialogCancelLabel: String? = null,
    defaultOptionColor: Color? = null,
    style: CardStyle? = null,
    cardColor: Color? = null,
    cardElevation: CardElevation? = null,
    cardBorder: BorderStroke? = null,
    shape: Shape? = null,
    outerPadding: PaddingValues? = null,
) {
    SearchIndexer.record(key, title, staticSummary ?: summary, icon)
    item(key = key, contentType = "ItemColorList") {
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
            ItemColorList(
                value = value,
                onValueChange = onValueChange,
                options = options,
                title = title,
                modifier = rowModifier,
                enabled = enabled,
                icon = icon,
                summary = summary,
                dialogCancelLabel = dialogCancelLabel,
                defaultOptionColor = defaultOptionColor,
            )
        }
    }
}

/**
 * Adds a color-picker preference item to a [group] (a [GroupScope]): the row joins the group
 * and is drawn in the group's card — the group's `style`, `shape` and spacing apply, so this
 * overload takes no [modifier] and no card parameters. Parameter semantics match the
 * [itemColorList] ([LazyListScope]) overload.
 */
public fun GroupScope.itemColorList(
    key: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    options: List<ColorOption>,
    title: String,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: String? = null,
    staticSummary: String? = null,
    dialogCancelLabel: String? = null,
    defaultOptionColor: Color? = null,
) {
    items.add(
        GroupItem(
            title = title,
            summary = staticSummary ?: summary,
            icon = icon,
            content = {
                ItemColorList(
                    value = value,
                    onValueChange = onValueChange,
                    options = options,
                    title = title,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    icon = icon,
                    summary = summary,
                    dialogCancelLabel = dialogCancelLabel,
                    defaultOptionColor = defaultOptionColor,
                )
            },
        )
    )
}

/**
 * A color-picker preference row: shows a swatch right-aligned on the row and opens an
 * alert dialog of radio-button swatch rows when clicked.
 *
 * @param value The index of the selected option in [options].
 * @param onValueChange Called with the index of the selected option.
 * @param options The selectable colors, in display order.
 * @param title The title of the row.
 * @param modifier Modifier applied to the row (e.g. a `testTag`).
 * @param enabled Whether the row is enabled.
 * @param icon Optional leading icon.
 * @param summary Optional summary shown below the title.
 * @param dialogCancelLabel Text for the dialog's cancel button; if null, the library's
 *   default cancel label is used.
 * @param defaultOptionColor See [itemColorList]'s parameter of the same name.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun ItemColorList(
    value: Int,
    onValueChange: (Int) -> Unit,
    options: List<ColorOption>,
    title: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: String? = null,
    dialogCancelLabel: String? = null,
    defaultOptionColor: Color? = null,
) {
    var openSelector by rememberSaveable { mutableStateOf(false) }
    // The color to show for the "default" option (a null [ColorOption.hex]): the caller's
    // platform default when given, otherwise the current theme's primary.
    val defaultSwatch = defaultOptionColor ?: MaterialTheme.colorScheme.primary
    // The row swatch: the selected option's color, or the default's.
    val swatchColor =
        options.getOrNull(value)?.hex?.let { parseHexColor(it) } ?: defaultSwatch

    if (openSelector) {
        BasicAlertDialog(onDismissRequest = { openSelector = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = AlertDialogDefaults.shape,
                color = AlertDialogDefaults.containerColor,
                tonalElevation = AlertDialogDefaults.TonalElevation,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = AlertDialogDefaults.titleContentColor,
                        modifier =
                            Modifier
                                .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 8.dp),
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    ) {
                        items(options.size) { i ->
                            val option = options[i]
                            val selected = i == value
                            val optionColor =
                                option.hex?.let { parseHexColor(it) } ?: defaultSwatch
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .selectable(
                                            selected,
                                            true,
                                            Role.RadioButton,
                                            onClick = {
                                                onValueChange(i)
                                                openSelector = false
                                            },
                                        )
                                        .padding(horizontal = 24.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = null,
                                    colors =
                                        RadioButtonDefaults.colors(
                                            selectedColor = optionColor,
                                        ),
                                )
                                Spacer(modifier = Modifier.width(24.dp))
                                Text(
                                    text = option.name,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Box(
                                    modifier =
                                        Modifier
                                            .size(24.dp)
                                            .background(optionColor, shape = RoundedCornerShape(8.dp)),
                                )
                            }
                        }
                    }
                    TextButton(
                        onClick = { openSelector = false },
                        modifier =
                            Modifier
                                .align(Alignment.End)
                                .padding(end = 16.dp, bottom = 16.dp),
                    ) {
                        Text(text = dialogCancelLabel ?: stringResource(R.string.cancel))
                    }
                }
            }
        }
    }

    Item(
        title = title,
        summary = summary,
        icon = icon,
        enabled = enabled,
        widgetContainer = {
            Box(
                modifier =
                    Modifier
                        .padding(end = 24.dp)
                        .size(24.dp)
                        .background(swatchColor, shape = RoundedCornerShape(8.dp)),
            )
        },
        onClick = { openSelector = true },
        modifier = modifier,
    )
}

/**
 * Parses a `#RRGGBB` / `#AARRGGBB` hex color, or [fallback] when the string is malformed.
 */
internal fun parseHexColor(hex: String, fallback: Color = Color.Unspecified): Color =
    runCatching {
        var h = hex.trim().removePrefix("#")
        if (h.length == 6) h = "FF$h"
        if (h.length == 8) Color(h.toLong(16).toInt()) else Color.Unspecified
    }.getOrDefault(fallback)
