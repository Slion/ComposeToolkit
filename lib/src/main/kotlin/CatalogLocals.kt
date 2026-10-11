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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Provides the [Catalog]'s two locals: the [LocalStore] the items read their state from
 * (a default, disk-backed store when null) and the [LocalItemTheme] they are styled with.
 *
 * @param flow The store flow the items read their state from.
 * @param theme The item theme the items are styled with.
 */
@Composable
public fun ProvideCatalogLocals(
    flow: MutableStateFlow<Store> = createDefaultStore(),
    theme: ItemTheme = itemTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalStore provides flow,
        LocalItemTheme provides theme,
        content = content,
    )
}

internal fun noLocalProvidedFor(name: String): Nothing {
    error("CompositionLocal $name not present")
}
