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

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.HingePolicy
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass

/**
 * The window-derived adaptive state of the screen: the [WindowAdaptiveInfo] to adapt to
 * (the host's, when the host passed one, the quantized window's otherwise) and the pane
 * scaffold [directive] derived from it.
 */
internal class ScreenAdaptive(
    val windowAdaptiveInfo: WindowAdaptiveInfo,
    val directive: PaneScaffoldDirective,
) {
    /** Whether the directive splits the screen into list + detail panes. */
    val isTwoPane: Boolean
        get() = directive.maxHorizontalPartitions >= 2
}

/**
 * Remembers the [ScreenAdaptive] of the screen.
 *
 * @param adaptiveInfo Adaptive layout info computed for the host of the screen instead
 * of for the window (see [Catalog.adaptiveInfo]); null uses the window.
 * @param singlePaneOnly Forces a single partition (see [Catalog.singlePaneOnly]).
 * @param options The fold/adaptive workarounds (see [CatalogOptions]).
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun rememberScreenAdaptive(
    adaptiveInfo: WindowAdaptiveInfo?,
    singlePaneOnly: Boolean,
    options: CatalogOptions,
): ScreenAdaptive {
    val density = LocalDensity.current
    // Both LocalWindowInfo.current.containerSize, currentWindowAdaptiveInfoV2(), and
    // Compose's onSizeChanged lag behind a non-recreating resize on foldables (e.g.
    // spreading the app across both Surface Duo screens): the actual window is already
    // at the new size, but the composition locals haven't caught up yet. The one source
    // proven to be current is the Android view itself, so observe its size changes via a
    // layout listener (it fires in the same layout pass as the resize — no per-frame
    // cost, unlike a Choreographer poll).
    val rootView = LocalView.current
    var measuredSize by remember { mutableStateOf(IntSize.Zero) }
    DisposableEffect(rootView) {
        val listener =
            android.view.View.OnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
                val size = IntSize(v.width, v.height)
                if (size.width > 0 && size.height > 0 && size != measuredSize) {
                    measuredSize = size
                }
            }
        rootView.addOnLayoutChangeListener(listener)
        onDispose { rootView.removeOnLayoutChangeListener(listener) }
    }
    // Fallback for the very first frame before the listener has run.
    val containerSize = LocalWindowInfo.current.containerSize
    val effectiveSize = if (measuredSize != IntSize.Zero) measuredSize else containerSize
    // currentWindowAdaptiveInfoV2() provides the window posture (hinges), which we still
    // need. The size class is re-derived from the measured size above.
    val baseInfo = adaptiveInfo ?: currentWindowAdaptiveInfoV2()
    val windowAdaptiveInfo =
        remember(baseInfo, adaptiveInfo, effectiveSize, density) {
            if (adaptiveInfo != null) {
                baseInfo
            } else {
                quantizedAdaptiveInfo(baseInfo, effectiveSize, density)
            }
        }
    val directive =
        remember(windowAdaptiveInfo, singlePaneOnly, options) {
            paneScaffoldDirective(windowAdaptiveInfo, singlePaneOnly, options)
        }
    return ScreenAdaptive(windowAdaptiveInfo, directive)
}

/**
 * Rebuilds [base]'s size class from a measured [size] in px, keeping its posture — the
 * size class of the composition locals is stale during a non-recreating resize, so the
 * directive would be derived from the wrong width.
 *
 * A pure function so it can be memoized (e.g. in `remember`) and unit-tested.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun quantizedAdaptiveInfo(
    base: WindowAdaptiveInfo,
    size: IntSize,
    density: Density,
): WindowAdaptiveInfo {
    // Build the size class the same way currentWindowAdaptiveInfoV2() does: quantize
    // the current dp size to the standard breakpoints (width to 0/600/840, height to
    // 0/480/900). The scaffold directive matches these exact breakpoint values (it does
    // `when (minWidthDp.dp) { 0.dp -> …; 600.dp -> …; 840.dp -> … }`), so a raw,
    // non-breakpoint dp such as 537 would not classify and would fall through to the
    // default (three panes).
    val widthDp = with(density) { size.width.toDp().value }
    val heightDp = with(density) { size.height.toDp().value }
    return WindowAdaptiveInfo(
        // `compute` is deprecated in favour of `computeWindowSizeClass`, which is not
        // resolvable against the window-core version this project compiles against, so
        // the deprecated overload is used.
        windowSizeClass =
            @Suppress("DEPRECATION") WindowSizeClass.Companion.compute(widthDp, heightDp),
        windowPosture = base.windowPosture,
    )
}

/**
 * The pane scaffold directive of the screen: the two-pane activation, the hinge handling
 * and the pane gap are the documented workarounds, exposed via [options] (see
 * [CatalogOptions]).
 *
 * A pure function so it can be memoized (e.g. in `remember`) and unit-tested.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun paneScaffoldDirective(
    windowAdaptiveInfo: WindowAdaptiveInfo,
    singlePaneOnly: Boolean,
    options: CatalogOptions,
): PaneScaffoldDirective {
    val base =
        if (options.twoPanesOnMediumWidth) {
            calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth(
                windowAdaptiveInfo = windowAdaptiveInfo,
                verticalHingePolicy = options.hingePolicy,
            )
        } else {
            calculatePaneScaffoldDirective(
                windowAdaptiveInfo = windowAdaptiveInfo,
                verticalHingePolicy = options.hingePolicy,
            )
        }
    // A host that must never split (a bottom sheet, a dialog) forces a single partition
    // so the screen stays single-pane at any width.
    val directive = if (singlePaneOnly) base.copy(maxHorizontalPartitions = 1) else base
    // The material3 recommended directives carry a 24dp gutter between the panes; the
    // option overrides it (0.dp makes the panes sit flush).
    return directive.copy(horizontalPartitionSpacerSize = options.paneGap)
}
