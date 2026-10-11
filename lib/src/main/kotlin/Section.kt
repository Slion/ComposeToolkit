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

/**
 * Adds a category header to the lazy list.
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
    SearchIndexer.record(key, title)
    item(key = key, contentType = "Section") {
        Section(
            title = title,
            modifier = modifier.then(highlightedKeyModifier(key)),
        )
    }
}

@Composable
public fun Section(title: String, modifier: Modifier = Modifier) {
    BasicItem(
        textContainer = {
            val theme = LocalItemTheme.current
            Box(
                modifier = Modifier.padding(theme.categoryPadding),
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
