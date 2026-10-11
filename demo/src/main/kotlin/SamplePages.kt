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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import net.slions.compose.toolkit.ItemSwitch
import net.slions.compose.toolkit.Page
import net.slions.compose.toolkit.group
import net.slions.compose.toolkit.rememberValue
import net.slions.compose.toolkit.section

const val SampleTitle = "Compose Toolkit"

/**
 * The root page of the sample tree, shared by every sample host (the full-screen screen
 * and the bottom sheet): a card group with one page row per item type (each exercising the
 * type's various configurations), the root's own preferences below them, and two action
 * rows at the end.
 *
 * The action rows carry an [item] action that replaces navigation — their trailing action
 * icon is the open-in-new (not the page-row chevron) accordingly: the Settings row opens
 * the theme in a separate activity hosting its own [net.slions.compose.toolkit.Catalog]
 * (its own page tree and a separate search scope, yet the same process-wide store), and
 * the Sheet row opens this very tree in a bottom sheet in the current activity. The live
 * "Theme" page is not part of this tree at all: it is hosted only by that activity.
 *
 * @param onOpenSettings Invoked when the Settings row is tapped, to start the settings
 *   activity.
 * @param onOpenSheet Invoked when the Sheet row is tapped, to show the bottom sheet.
 */
@Composable
fun sampleRootPage(
    onOpenSettings: () -> Unit,
    onOpenSheet: () -> Unit,
): Page {
    // The page builders are plain functions (a page is plain data), so any theme values
    // or state they need are resolved here, in the composable scope.
    val colorScheme = MaterialTheme.colorScheme
    val elevatedElevation = CardDefaults.elevatedCardElevation()
    val sliderState = remember { mutableFloatStateOf(0.75f) }
    val radioGroup1 = rememberSaveable { mutableStateOf("a") }
    val radioGroup3 = rememberSaveable { mutableStateOf("c") }
    return Page(id = "root", title = SampleTitle) {
        section(key = "key_paged", title = "Pages")
        group(key = "pages_group") {
            // A carded page row: no content, so the standard row is rendered from the page
            // (title/summary/icon from the page, chevron and navigation by default).
            item(page = itemPage(colorScheme, elevatedElevation))
            item(page = groupPage(colorScheme))
            item(page = checkboxPage())
            item(page = switchPage())
            item(page = sliderPage(sliderState))
            item(page = listPage())
            item(page = multiSelectListPage())
            item(page = textFieldPage())
            item(page = radioButtonPage(radioGroup1, radioGroup3))
            item(page = itemActionsPage())
            item(page = itemActionIconButtonPage())
            item(page = itemActionsSwitchPage())
            item(page = nestedPage())
        }
        section(key = "section_others", title = "Others")
        group(key = "actions_group") {
            // Action rows: a single action, so a plain item (no page) — the open-in-new
            // indicator (not a page-row chevron) signals they open something external.
            item(
                title = "Settings",
                summary = "A second Catalog: its own tree and search, sharing this app's store.",
                icon = { Icon(imageVector = Icons.Outlined.Settings, contentDescription = null) },
                actionIcon = { Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) },
                onClick = onOpenSettings,
            )
            item(
                title = "Bottom sheet",
                summary = "This settings tree inside a bottom sheet.",
                icon = { Icon(imageVector = Icons.Outlined.Settings, contentDescription = null) },
                actionIcon = { Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) },
                onClick = onOpenSheet,
            )
        }
    }
}
