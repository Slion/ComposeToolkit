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

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import kotlinx.coroutines.flow.MutableStateFlow
import net.slions.compose.toolkit.LocalStore
import net.slions.compose.toolkit.MutableStore
import net.slions.compose.toolkit.PageTransition
import net.slions.compose.toolkit.Store

/**
 * The user-selectable theme properties the sample's "Theme" page can change.
 *
 * All properties are optional: a null value means "use the built-in default".
 *
 * @param accent hex color string (e.g. `"#6750A4"`) used as the color-scheme seed; null =
 *   the platform default (dynamic colors on Android 12+ when [SampleTheme] is asked for them).
 * @param cornerRadiusDp corner-radius scale for cards/rows (the "medium" size of the scale);
 *   null = [DEFAULT_CORNER_RADIUS_DP].
 * @param fontSizePercent font-size scale as a percentage of the default type scale; null = 100.
 * @param tintFactorPercent how strongly the accent tints the neutral roles (background,
 *   surfaces, text), as a percentage of the seed's chroma; null = [DEFAULT_TINT_FACTOR_PERCENT].
 * @param fontFamily one of [FONT_FAMILIES]; null = [DEFAULT_FONT].
 * @param paneGapDp the gutter between the two panes of the adaptive two-pane layout, in dp;
 *   null = [DEFAULT_PANE_GAP_DP].
 * @param pageTransition one of [PAGE_TRANSITION_OPTIONS], the name of the preset the page
 *   transition uses; null = [DEFAULT_PAGE_TRANSITION].
 */
@Immutable
data class SampleThemeValues(
    val themeMode: SampleThemeMode = SampleThemeMode.SYSTEM,
    val accent: String? = null,
    val cornerRadiusDp: Int? = null,
    val fontSizePercent: Int? = null,
    val tintFactorPercent: Int? = null,
    val fontFamily: String? = null,
    val paneGapDp: Int? = null,
    val pageTransition: String? = null,
) {
    companion object {
        /** The platform default font. */
        const val DEFAULT_FONT = "default"

        /** A serif font. */
        const val SERIF_FONT = "serif"

        /** A monospace font. */
        const val MONO_FONT = "monospace"

        /** All selectable font families, in display order. */
        val FONT_FAMILIES: List<String> = listOf(DEFAULT_FONT, SERIF_FONT, MONO_FONT)

        /**
         * The accent presets, in display order (dynamic first). The first entry, whose [hex]
         * is null, means the platform default (dynamic colors on Android 12+). The rest are
         * fixed seeds — darker pastels rather than fully saturated colors, so the derived
         * scheme stays readable in both themes.
         */
        val ACCENT_PRESETS: List<AccentColor> = listOf(
            AccentColor("Default", null),
            AccentColor("Purple", "#6750A4"),
            AccentColor("Blue", "#6288C0"),
            AccentColor("Teal", "#4FA39B"),
            AccentColor("Green", "#7DA86E"),
            AccentColor("Amber", "#C9A24B"),
            AccentColor("Red", "#C2685C"),
            AccentColor("Orange", "#CE8A55"),
            AccentColor("Pink", "#C57FB5"),
        )
    }
}

/**
 * The selectable accent colors, shown as the "Color" row. A null [hex] means the
 * platform default (dynamic colors on Android 12+); otherwise [hex] is the fixed seed.
 */
@Immutable
data class AccentColor(val name: String, val hex: String?)

/** The light/dark theme the app should use. */
enum class SampleThemeMode(val label: String) {
    /** Follow the system setting. */
    SYSTEM("System"),

    /** Always light. */
    LIGHT("Light"),

    /** Always dark. */
    DARK("Dark"),
}

/** The current [SampleThemeValues]. The sample provides this around [SampleTheme]. */
val LocalSampleThemeValues = staticCompositionLocalOf { SampleThemeValues() }

/** Default corner radius (the "medium" size of the shape scale) in dp. */
internal const val DEFAULT_CORNER_RADIUS_DP = 12

/**
 * The default pane gap in dp (the gutter between the two panes of the adaptive layout);
 * the material3 recommended directives carry a 24dp spacer there.
 */
internal const val DEFAULT_PANE_GAP_DP = 24

/**
 * The default neutral-tint factor as a percentage: how strongly the seed hue tints the
 * neutral roles (background, surfaces, text). 5% is the standard M3 dynamic factor; higher
 * values push the accent into the neutrals.
 */
internal const val DEFAULT_TINT_FACTOR_PERCENT = 5

/** The tint-factor range, in percent, as a slider. */
internal val TINT_FACTOR_RANGE: IntRange = 0..50

/** The default page-transition preset name. */
internal const val DEFAULT_PAGE_TRANSITION = "Slide"

/** The selectable page-transition presets, in display order. */
internal val PAGE_TRANSITION_OPTIONS: List<String> =
    listOf("None", "Fade", DEFAULT_PAGE_TRANSITION, "Slide & fade")

/** Maps a [PAGE_TRANSITION_OPTIONS] name to its [PageTransition] preset. */
internal fun pageTransitionOf(name: String?): PageTransition =
    when (name) {
        "None" -> PageTransition.None
        "Fade" -> PageTransition.Fade
        "Slide" -> PageTransition.Slide
        "Slide & fade" -> PageTransition.SlideFade
        else -> PageTransition.Slide
    }

// Item keys the theme values are persisted under. Only primitive types (Int/String) are
// stored, matching what the default preference flow can serialize on every platform.
private const val KEY_THEME_MODE = "sample.theme.mode"
private const val KEY_ACCENT = "sample.theme.accent"
private const val KEY_CORNER_RADIUS = "sample.theme.cornerRadius"
private const val KEY_FONT_SIZE = "sample.theme.fontSize"
private const val KEY_TINT_FACTOR = "sample.theme.tintFactor"
private const val KEY_FONT_FAMILY = "sample.theme.fontFamily"
private const val KEY_PANE_GAP = "sample.theme.paneGap"
private const val KEY_PAGE_TRANSITION = "sample.theme.pageTransition"

/**
 * Rebuilds a [SampleThemeValues] from a [Store] store. Unset or invalid entries fall
 * back to the built-in default for that field.
 */
private fun SampleThemeValues.Companion.fromPrefs(prefs: Store): SampleThemeValues =
    SampleThemeValues(
        themeMode = prefs.getString(KEY_THEME_MODE)?.let { name ->
            SampleThemeMode.entries.firstOrNull { it.name == name }
        } ?: SampleThemeMode.SYSTEM,
        accent = prefs.getString(KEY_ACCENT),
        cornerRadiusDp = prefs.getInt(KEY_CORNER_RADIUS),
        fontSizePercent = prefs.getInt(KEY_FONT_SIZE),
        tintFactorPercent = prefs.getInt(KEY_TINT_FACTOR),
        fontFamily = prefs.getString(KEY_FONT_FAMILY),
        paneGapDp = prefs.getInt(KEY_PANE_GAP),
        pageTransition = prefs.getString(KEY_PAGE_TRANSITION),
    )

private fun Store.getString(key: String): String? = this[key]

private fun Store.getInt(key: String): Int? = this[key]

/** Writes this [SampleThemeValues] into a [MutableStore] store (nulls are removed). */
private fun SampleThemeValues.writeTo(prefs: MutableStore) {
    prefs[KEY_THEME_MODE] = themeMode.name
    prefs[KEY_ACCENT] = accent
    prefs[KEY_CORNER_RADIUS] = cornerRadiusDp
    prefs[KEY_FONT_SIZE] = fontSizePercent
    prefs[KEY_TINT_FACTOR] = tintFactorPercent
    prefs[KEY_FONT_FAMILY] = fontFamily
    prefs[KEY_PANE_GAP] = paneGapDp
    prefs[KEY_PAGE_TRANSITION] = pageTransition
}

/**
 * The current [SampleThemeValues], backed by the shared preference [flow] (the single source of
 * truth). Reads the store reactively into a [SampleThemeValues] and returns a [write] function
 * that applies a new value — updating the UI and persisting it to disk via the flow. The sample
 * provides this around [SampleTheme] so the whole app re-themes live and the settings survive a
 * relaunch.
 */
@Composable
internal fun rememberSampleThemeValues(
    flow: MutableStateFlow<Store> = LocalStore.current,
): Pair<SampleThemeValues, (SampleThemeValues) -> Unit> {
    val store = flow.collectAsState().value
    val values = remember(store) { SampleThemeValues.fromPrefs(store) }
    val write: (SampleThemeValues) -> Unit = { next ->
        flow.value = flow.value.toMutableStore().apply { next.writeTo(this) }
    }
    return values to write
}

/** The default M3 scheme for the given theme, used when no accent is set. */
internal fun sampleDefaultScheme(dark: Boolean): ColorScheme =
    if (dark) darkColorScheme() else lightColorScheme()

/**
 * The sample's color scheme: dynamic colors when [dynamicColor] is on and no [accent] is set
 * (the wallpaper colors on Android 12+); a fixed [accent] (a hex color string) seeds the full
 * scheme via [sampleSeededScheme]; otherwise the default M3 scheme.
 *
 * @param tintFactor the neutral-tint factor as a fraction (e.g. 0.05 = 5%), controlling how
 *   strongly the seed hue tints the neutral roles.
 */
@Composable
internal fun sampleColorScheme(
    accent: String?,
    dark: Boolean,
    dynamicColor: Boolean,
    tintFactor: Float,
): ColorScheme {
    val seedArgb = accent?.let { hex ->
        runCatching {
            val color = Color(android.graphics.Color.parseColor(hex))
            color.toArgb()
        }.getOrNull()
    }
    return when {
        // No fixed accent: the platform dynamic (wallpaper) colors on Android 12+.
        seedArgb == null && dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val dynamic = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            // The "Neutral tint" slider is a no-op on the raw dynamic scheme. When the user
            // moves it off its default, re-derive the whole scheme from the dynamic primary
            // so the neutral roles (background, surfaces, text) respond to the slider. At the
            // default the pure dynamic colors are kept, so "Default" still looks native.
            if (tintFactor == DEFAULT_TINT_FACTOR_PERCENT / 100f) {
                dynamic
            } else {
                sampleSeededScheme(dynamic.primary.toArgb(), dark, tintFactor)
            }
        }
        seedArgb != null -> sampleSeededScheme(seedArgb, dark, tintFactor)
        else -> sampleDefaultScheme(dark)
    }
}

/**
 * Derives a full Material 3 [ColorScheme] from a fixed accent seed ([seed], an ARGB int).
 *
 * Seeding only `primary` (e.g. `darkColorScheme(primary = seed)`) leaves every other role on
 * the baseline palette, so components that read multiple roles (the switch knob is
 * `onPrimary`, the track `primary`) don't follow the accent. This maps the seed's HCT hue +
 * chroma to the standard M3 tones for the given theme, and tints the neutral roles
 * (background, surfaces, text) with the seed hue at [tintFactor] of its chroma, matching how
 * the dynamic (wallpaper-seeded) scheme looks.
 */
internal fun sampleSeededScheme(
    seed: Int,
    dark: Boolean,
    tintFactor: Float,
): ColorScheme {
    val base = sampleDefaultScheme(dark)
    val hct = FloatArray(3)
    ColorUtils.colorToM3HCT(seed, hct)
    val hue = hct[0]
    val chroma = hct[1]
    fun t(tone: Int) = Color(ColorUtils.M3HCTToColor(hue, chroma, tone.toFloat()))
    // Neutral roles: the seed's hue at [tintFactor] of its chroma (Material tints its dynamic
    // neutral palette at 0.05; a higher factor pushes the accent into the neutrals).
    fun n(tone: Int) = Color(ColorUtils.M3HCTToColor(hue, chroma * tintFactor, tone.toFloat()))
    // The M3 tonal palette assumes a vivid seed. With a *dark* seed (e.g. #386A20, lightness
    // ~0.4) the dark-mode tone 80 lands near-black and vanishes against the dark surface — the
    // slider's active track disappears. Floor the primary's lightness (hue + chroma kept).
    val primary = if (dark) {
        val hct2 = FloatArray(3)
        ColorUtils.colorToM3HCT(ColorUtils.M3HCTToColor(hue, chroma, 80f), hct2)
        Color(ColorUtils.M3HCTToColor(hct2[0], hct2[1], hct2[2].coerceAtLeast(0.45f)))
    } else {
        t(40)
    }
    return base.copy(
        primary = primary,
        onPrimary = if (dark) t(20) else t(100),
        primaryContainer = if (dark) t(30) else t(90),
        onPrimaryContainer = if (dark) t(90) else t(10),
        inversePrimary = t(40),
        secondary = if (dark) t(80) else t(40),
        onSecondary = if (dark) t(20) else t(100),
        secondaryContainer = if (dark) t(30) else t(90),
        onSecondaryContainer = if (dark) t(90) else t(10),
        tertiary = if (dark) t(80) else t(40),
        onTertiary = if (dark) t(20) else t(100),
        tertiaryContainer = if (dark) t(30) else t(90),
        onTertiaryContainer = if (dark) t(90) else t(10),
        surfaceTint = primary,
        // Neutral family, at the standard M3 tones for the given theme.
        background = if (dark) n(10) else n(98),
        onBackground = if (dark) n(90) else n(10),
        surface = if (dark) n(6) else n(98),
        onSurface = if (dark) n(90) else n(10),
        surfaceVariant = if (dark) n(30) else n(90),
        onSurfaceVariant = if (dark) n(80) else n(30),
        outline = if (dark) n(60) else n(50),
        outlineVariant = if (dark) n(30) else n(80),
        inverseSurface = if (dark) n(90) else n(20),
        inverseOnSurface = if (dark) n(20) else n(95),
        surfaceContainerLowest = if (dark) n(4) else n(100),
        surfaceContainerLow = if (dark) n(10) else n(96),
        surfaceContainer = if (dark) n(12) else n(94),
        surfaceContainerHigh = if (dark) n(17) else n(92),
        surfaceContainerHighest = if (dark) n(22) else n(90),
    )
}

/**
 * The platform's *actual* default accent (what "Default" resolves to), as a [Color].
 *
 * On Android 12+ this is the system dynamic (wallpaper) accent, resolved independently of
 * which option is currently selected — so the Color row and dialog can show the true
 * "what Default will look like" swatch. Returns null below Android 12, letting the caller
 * fall back to the current theme's primary.
 */
@Composable
internal fun systemDefaultAccentColor(dark: Boolean): Color? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    // The system dynamic accent, resolved from the context independently of the current
    // selection — this is what "Default" actually resolves to on this device.
    val context = LocalContext.current
    return if (dark) {
        dynamicDarkColorScheme(context).primary
    } else {
        dynamicLightColorScheme(context).primary
    }
}
