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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Adds a category header to the lazy list.
 *
 * A section that is the page's first item drops its top clearance (the pane's header bar
 * already separates it from the list).
 *
 * @param key The lazy list key of the header.
 * @param title The title of the header. Also used as the header's search text.
 * @param modifier Modifier applied to the header.
 */
public fun LazyListScope.section(
    key: String,
    title: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    // The panes build a page's content through a [CountingListScope], so the position of
    // this registration is the section's slot in the page's list. A build without one (e.g.
    // the search index walk) is not first: the composable is not composed there anyway.
    val isFirst = (this as? CountingListScope)?.position == 0
    SearchIndexer.record(key, title)
    item(key = key, contentType = "Section") {
        Section(
            title = title,
            modifier = modifier.then(highlightedKeyModifier(key)),
            isFirst = isFirst,
        )
    }
}

/**
 * A category header.
 *
 * @param title The title of the header.
 * @param modifier Modifier applied to the header.
 * @param isFirst Whether the header is the page's first item: when true, the top
 *   clearance is dropped (the pane's header bar already separates the list from the
 *   bar).
 */
@Composable
public fun Section(title: String, modifier: Modifier = Modifier, isFirst: Boolean = false) {
    BasicItem(
        textContainer = {
            val theme = LocalItemTheme.current
            // The first item sits right below the pane's header bar: no top clearance.
            val direction = LocalLayoutDirection.current
            val padding =
                if (isFirst) {
                    PaddingValues(
                        start = theme.categoryPadding.calculateLeftPadding(direction),
                        top = 0.dp,
                        end = theme.categoryPadding.calculateRightPadding(direction),
                        bottom = theme.categoryPadding.calculateBottomPadding(),
                    )
                } else {
                    theme.categoryPadding
                }
            Box(
                modifier = Modifier.padding(padding),
                contentAlignment = Alignment.CenterStart,
            ) {
                CompositionLocalProvider(LocalContentColor provides theme.categoryColor) {
                    ProvideTextStyle(value = theme.categoryTextStyle) {
                        Text(text = title)
                    }
                }
            }
        },
        modifier = modifier,
    )
}
