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

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyListScope
import net.slions.compose.toolkit.Page
import net.slions.compose.toolkit.Catalog
import net.slions.compose.toolkit.ProvideCatalogLocals
import net.slions.compose.toolkit.ProvideItemTheme
import net.slions.compose.toolkit.item
import net.slions.compose.toolkit.windowAdaptiveInfoFor

/**
 * The default maximum width of the in-activity bottom sheet: on wide windows (an unfolded
 * foldable) the sheet is centered at this width with a visible scrim on either side instead
 * of stretching edge to edge. 480dp keeps the single-pane content comfortably readable
 * without dominating the window.
 */
private val DEFAULT_MAX_SHEET_WIDTH: Dp = 480.dp

/**
 * The settings tree in a bottom sheet hosted in the current activity (not a separate one):
 * the same pages as the full-screen host, adapted to the sheet's measured size. The theme
 * values are read from the same shared preference flow as the host, so opening or closing
 * the sheet never resets or forks the theme.
 *
 * @param onDismiss Closes the sheet (scrim tap, or system back at the root).
 * @param maxWidth The sheet's maximum width; on wide windows (an unfolded foldable) the
 * sheet is centered at this width instead of stretching edge to edge. On a narrower
 * container it fills the width as usual. Defaults to [DEFAULT_MAX_SHEET_WIDTH].
 */
@Composable
fun SheetSettings(
    onDismiss: () -> Unit,
    maxWidth: Dp = DEFAULT_MAX_SHEET_WIDTH,
) {
    ProvideCatalogLocals {
        val context = LocalContext.current
        ProvideItemTheme {
            DraggableBottomSheet(onDismiss, maxWidth) { size ->
                SheetSettingsScreen(
                    root =
                        sampleRootPage(
                            onOpenSettings = {
                                context.startActivity(
                                    Intent(context, SettingsActivity::class.java)
                                )
                            },
                            onOpenSheet = onDismiss,
                        ),
                    hostSize = size,
                    onBack = onDismiss,
                )
            }
        }
    }
}

/**
 * A simple, self-contained bottom sheet: a rounded panel anchored to the bottom of its
 * container, over a scrim (tapping the scrim dismisses), that the user drags to change its
 * expand fraction (0.4 = peeked, 1 = full height of the container). Its width is at most
 * [maxWidth] (centered over the scrim on wider containers). It reports the content's
 * measured [IntSize] to [content] so the hosted content can adapt to the sheet's current
 * size.
 */
@Composable
private fun DraggableBottomSheet(
    onDismiss: () -> Unit,
    maxWidth: Dp = DEFAULT_MAX_SHEET_WIDTH,
    content: @Composable (IntSize) -> Unit,
) {
    val density = LocalDensity.current
    var expand by remember { mutableFloatStateOf(0.75f) }
    var containerHeightPx by remember { mutableStateOf(0) }
    var contentSize by remember { mutableStateOf(IntSize.Zero) }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .onSizeChanged { containerHeightPx = it.height }
            .clickable(onClick = onDismiss),
    ) {
        // The sheet's height is its expand fraction of the container, in dp.
        val sheetHeight =
            with(density) { (containerHeightPx * expand).toInt().coerceAtLeast(0).toDp() }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                // Clamp to maxWidth, then fill: edge to edge on a narrow container, centered
                // at maxWidth on a wide one.
                .widthIn(max = maxWidth)
                .fillMaxWidth()
                .height(sheetHeight)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .navigationBarsPadding()
                .pointerInput(containerHeightPx) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            // Snap to the nearest of peeked / expanded.
                            expand = if (expand > 0.5f) 1f else 0.4f
                        },
                    ) { _, dragAmount ->
                        if (containerHeightPx > 0) {
                            expand = (expand - dragAmount / containerHeightPx).coerceIn(0.4f, 1f)
                        }
                    }
                },
        ) {
            // The drag handle: a small pill at the top of the sheet.
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
                    .size(32.dp, 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
            )
            // The hosted content fills the rest of the sheet and reports its measured size.
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(top = 12.dp)
                    .onSizeChanged { contentSize = it },
            ) {
                if (contentSize != IntSize.Zero) {
                    content(contentSize)
                }
            }
        }
    }
}

/**
 * The settings tree adapted to a host measured at [hostSize]: a [WindowAdaptiveInfo] is built
 * for that size (no hinges — the fold does not apply to the sheet's surface) and passed to
 * [Catalog.adaptiveInfo], so the screen's single- vs two-pane layout follows the
 * sheet rather than the window.
 *
 * @param onBack Called when the screen's back reaches the root (system back on the list, or
 * back at the top level); the host closes the sheet.
 */
@Composable
private fun SheetSettingsScreen(
    root: Page,
    hostSize: IntSize,
    onBack: () -> Unit,
) {
    val density = LocalDensity.current
    // The sheet is a narrow, modal host: it must never split into a list + detail, so force
    // the single-pane layout (the list navigates to the detail and back). Back at the root
    // closes the sheet.
    Catalog(
        title = SampleTitle,
        root = root,
        adaptiveInfo = windowAdaptiveInfoFor(hostSize, density),
        singlePaneOnly = true,
        onBack = onBack,
    )
}
