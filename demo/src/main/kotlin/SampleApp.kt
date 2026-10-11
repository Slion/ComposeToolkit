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

package net.slions.compose.toolkit.demo

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.slions.compose.toolkit.Catalog
import net.slions.compose.toolkit.CatalogOptions
import net.slions.compose.toolkit.ProvidePreferenceLocals
import net.slions.compose.toolkit.ProvidePreferenceTheme

/**
 * The root of the sample app: the pages hosted by
 * [net.slions.compose.toolkit.Catalog] (adaptive two-pane layout).
 */
@Composable
fun SampleApp() {
    // The theme flow is the single source of truth; the default flow persists it to disk.
    ProvidePreferenceLocals {
        val (themeValues, _) = rememberSampleThemeValues()
        val darkTheme = effectiveDarkTheme(themeValues.themeMode, isSystemInDarkTheme())
        var sheetOpen by rememberSaveable { mutableStateOf(false) }
        CompositionLocalProvider(LocalSampleThemeValues provides themeValues) {
            SampleTheme(darkTheme = darkTheme) {
                // Re-provide the preference theme *inside* SampleTheme: its default colors
                // (title/summary/icon, drawn from MaterialTheme.colorScheme) are otherwise
                // captured once, against the outer light scheme, and would not follow the
                // live light/dark switch.
                ProvidePreferenceTheme {
                    val context = LocalContext.current
                    Catalog(
                        title = SampleTitle,
                        root =
                            sampleRootPage(
                                onOpenSettings = {
                                    context.startActivity(
                                        Intent(context, SettingsActivity::class.java)
                                    )
                                },
                                onOpenSheet = { sheetOpen = true },
                            ),
                        // This is the app's root surface, but the title bar still carries
                        // a back chevron: at the root of the tree it closes the activity.
                        onBack = { (context as? Activity)?.finish() },
                        showBackButton = true,
                        // The pane gutter follows the Theme page's "Spacing" slider, so
                        // it can be tuned (down to 0dp, flush) at runtime.
                        options =
                            CatalogOptions(
                                paneGap = (themeValues.paneGapDp ?: DEFAULT_PANE_GAP_DP).dp,
                            ),
                    )
                    // The sheet, hosted in this activity over the full-screen screen.
                    if (sheetOpen) {
                        SheetSettings(onDismiss = { sheetOpen = false })
                    }
                }
            }
        }
    }
}

@Composable
@Preview
fun SampleAppPreview() {
    SampleApp()
}
