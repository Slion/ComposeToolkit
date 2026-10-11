/*
 * Copyright 2026 Stéphane Lenclud
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.slions.compose.toolkit.demo

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Icon
import androidx.compose.ui.text.AnnotatedString
import dev.vicart.compose.material.symbols.MaterialSymbol
import net.slions.compose.toolkit.ItemList
import net.slions.compose.toolkit.Page
import net.slions.compose.toolkit.group
import net.slions.compose.toolkit.section

/**
 * The "Theme" page: live theme controls. It is hosted by [SettingsActivity] in its own
 * Catalog; its state is hoisted there as a [SampleThemeValues] and applied by [SampleTheme],
 * so changing a row here re-themes the whole app — a working example of the controlled
 * preference forms.
 */
fun themePage(
    values: SampleThemeValues,
    onValuesChange: (SampleThemeValues) -> Unit,
    dark: Boolean,
    defaultAccent: Color?,
): Page {
    return Page(
        id = "theme",
        title = "Theme",
        summary = "Contrast, colors, and text. Settings are persisted.",
        icon = { MaterialSymbol.Outlined(icon = "palette") },
    ) {
        section(key = "theme_colors_category", title = "Colors")
        group(key = "theme_colors_group") {
            item(title = "Contrast", summary = values.themeMode.label) {
                ItemList(
                    value = values.themeMode,
                    onValueChange = { onValuesChange(values.copy(themeMode = it)) },
                    values = SampleThemeMode.entries,
                    title = "Contrast",
                    summary = values.themeMode.label,
                    icon = { Icon(imageVector = Icons.Outlined.Contrast, contentDescription = null) },
                    valueToText = { AnnotatedString(it.label) },
                )
            }
            item(title = "Color", summary = accentNameOf(values.accent)) {
                ColorPreference(
                    value = values.accent ?: "",
                    onValueChange = { onValuesChange(values.copy(accent = it.ifEmpty { null })) },
                    options = SampleThemeValues.ACCENT_PRESETS.map { AccentColorOption(it.name, it.hex) },
                    title = "Color",
                    summary = accentNameOf(values.accent),
                    icon = { MaterialSymbol.Outlined(icon = "colors") },
                    defaultOptionColor = defaultAccent,
                )
            }
            item(title = "Tint") {
                LiveItemSlider(
                    title = "Tint",
                    value = (values.tintFactorPercent ?: DEFAULT_TINT_FACTOR_PERCENT).toFloat(),
                    onValueChange = { onValuesChange(values.copy(tintFactorPercent = it.toInt())) },
                    valueRange = TINT_FACTOR_RANGE.first.toFloat()..TINT_FACTOR_RANGE.last.toFloat(),
                    // M3 'steps' counts intermediate stops (segments = steps + 1), so for 5% steps
                    // over 0..50 (10 segments) we pass 9, not 10.
                    valueSteps = (TINT_FACTOR_RANGE.last - TINT_FACTOR_RANGE.first) / 5 - 1,
                    valueText = { "${it.toInt()}%" },
                    live = true,
                    icon = { MaterialSymbol.Outlined(icon = "imagesearch_roller") },
                )
            }
        }

        section(key = "theme_text_category", title = "Texts")
        group(key = "theme_text_group") {
            item(title = "Font", summary = fontLabel(values.fontFamily)) {
                ItemList(
                    value = values.fontFamily ?: SampleThemeValues.DEFAULT_FONT,
                    onValueChange = { onValuesChange(values.copy(fontFamily = it)) },
                    values = SampleThemeValues.FONT_FAMILIES,
                    title = "Font",
                    summary = fontLabel(values.fontFamily),
                    icon = { Icon(imageVector = Icons.Outlined.TextFields, contentDescription = null) },
                    valueToText = { AnnotatedString(fontLabel(it)) },
                )
            }
            item(title = "Size") {
                LiveItemSlider(
                    title = "Size",
                    value = (values.fontSizePercent ?: 100).toFloat(),
                    onValueChange = { onValuesChange(values.copy(fontSizePercent = it.toInt())) },
                    valueRange = 80f..140f,
                    // 5% steps over 80..140 = 12 segments, so 11 intermediate stops.
                    valueSteps = (140 - 80) / 5 - 1,
                    valueText = { "${it.toInt()}%" },
                    live = true,
                    icon = { MaterialSymbol.Outlined(icon = "format_size") },
                )
            }
        }

        section(key = "theme_shapes_category", title = "Shapes")
        group(key = "theme_shapes_group") {
            item(title = "Corner") {
                LiveItemSlider(
                    title = "Corner",
                    value = (values.cornerRadiusDp ?: DEFAULT_CORNER_RADIUS_DP).toFloat(),
                    onValueChange = { onValuesChange(values.copy(cornerRadiusDp = it.toInt())) },
                    valueRange = 0f..32f,
                    // 2 dp steps over 0..32 = 16 segments, so 15 intermediate stops.
                    valueSteps = (32 - 0) / 2 - 1,
                    valueText = { "${it.toInt()} dp" },
                    live = true,
                    icon = { MaterialSymbol.Outlined(icon = "rounded_corner") },
                )
            }
        }

        section(key = "theme_spacing_category", title = "Spacing")
        group(key = "theme_spacing_group") {
            item(title = "Pane gap") {
                LiveItemSlider(
                    title = "Pane gap",
                    value = (values.paneGapDp ?: DEFAULT_PANE_GAP_DP).toFloat(),
                    onValueChange = { onValuesChange(values.copy(paneGapDp = it.toInt())) },
                    valueRange = 0f..48f,
                    // 4 dp steps over 0..48 = 12 segments, so 11 intermediate stops.
                    valueSteps = (48 - 0) / 4 - 1,
                    valueText = { "${it.toInt()} dp" },
                    live = true,
                    icon = { MaterialSymbol.Outlined(icon = "splitscreen") },
                )
            }
        }
    }
}

private fun accentNameOf(hex: String?): String =
    SampleThemeValues.ACCENT_PRESETS.firstOrNull { it.hex == hex }?.name ?: "Default"

private fun fontLabel(font: String?): String = when (font) {
    SampleThemeValues.SERIF_FONT -> "Serif"
    SampleThemeValues.MONO_FONT -> "Monospace"
    else -> "Default"
}
