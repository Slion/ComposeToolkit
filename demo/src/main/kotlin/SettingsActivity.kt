/*
 * Copyright 2026 Stéphane Lenclud
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

package net.slions.compose.toolkit.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import net.slions.compose.toolkit.Catalog
import net.slions.compose.toolkit.CatalogOptions
import net.slions.compose.toolkit.Page
import net.slions.compose.toolkit.ProvidePreferenceLocals
import net.slions.compose.toolkit.ProvidePreferenceTheme
import net.slions.compose.toolkit.item

/**
 * A second settings host: a dedicated activity running its own
 * [net.slions.compose.toolkit.Catalog] that contains only the theme page.
 *
 * This exists to demonstrate that one app can host **several independent Catalogs**. Each
 * [Catalog] builds its own [net.slions.compose.toolkit.SearchIndex] from its own page tree,
 * so the search field here covers only the theme page — a separate search scope from the
 * main activity's Catalog — while both Catalogs read and write the same process-wide
 * [net.slions.compose.toolkit.Store] (see [net.slions.compose.toolkit.createDefaultStore]):
 * a change made here is persisted and picked up by the main activity on return.
 */
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SettingsActivityScreen(onBack = { finish() })
        }
    }
}

@Composable
private fun SettingsActivityScreen(onBack: () -> Unit) {
    // The default flow is process-global, so this Catalog and the main activity's Catalog
    // observe the same store: the theme set here re-themes the main activity when it
    // returns, even though the two Catalogs are otherwise unrelated.
    ProvidePreferenceLocals {
        val (themeValues, writeThemeValues) = rememberSampleThemeValues()
        val darkTheme = effectiveDarkTheme(themeValues.themeMode, isSystemInDarkTheme())
        // Resolved here (in the composable scope) and passed to the plain page builder.
        val defaultAccent = systemDefaultAccentColor(darkTheme)
        CompositionLocalProvider(LocalSampleThemeValues provides themeValues) {
            SampleTheme(darkTheme = darkTheme) {
                ProvidePreferenceTheme {
                    Catalog(
                        title = "Settings",
                        // A wrapper root holding the theme as its only page row: a
                        // childless root would leave the two-pane detail empty.
                        root =
                            Page(id = "settings_root", title = "Settings") {
                                item(
                                    page =
                                        themePage(
                                            values = themeValues,
                                            onValuesChange = writeThemeValues,
                                            dark = darkTheme,
                                            defaultAccent = defaultAccent,
                                        )
                                )
                            },
                        // This is a second surface, not the app's root: the title bar
                        // carries a back chevron and the system back at the root of the
                        // tree both close the activity.
                        onBack = onBack,
                        showBackButton = true,
                        // Same theme-driven pane gutter as the main activity: both
                        // Catalogs read the same store, so the slider in one updates both.
                        options =
                            CatalogOptions(
                                paneGap = (themeValues.paneGapDp ?: DEFAULT_PANE_GAP_DP).dp,
                            ),
                    )
                }
            }
        }
    }
}
