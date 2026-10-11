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

package net.slions.compose.toolkit

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The direction of a page navigation, as derived from the navigation trail: [Forward] when
 * the new page is deeper in the tree (or a same-level switch), [Backward] when popping up
 * to a shallower page.
 */
public enum class PageDirection {
    /** Navigating deeper (or switching between pages of the same level). */
    Forward,

    /** Popping up to a shallower page. */
    Backward,
}

/**
 * The endpoints of a page transition, as a pair of starts and ends the engine
 * interpolates between over the transition's duration:
 *
 * - [enterOffset]/[enterAlpha] — the *incoming* content's **start** position (offset
 *   relative to its final position, positive = to the right) and alpha; it always ends
 *   at offset 0, alpha 1.
 * - [exitOffset]/[exitAlpha] — the *outgoing* content's **end** position; it always
 *   starts at offset 0, alpha 1.
 */
public data class PageTransitionEndpoints(
    public val enterOffset: Dp,
    public val enterAlpha: Float,
    public val exitOffset: Dp,
    public val exitAlpha: Float,
)

/**
 * How the panes animate when the shown page changes: the detail pane when another page
 * is opened, the list pane when the shown level changes, and the pane swap in single-pane
 * layouts. Set on [CatalogOptions.pageTransition].
 *
 * The direction of a direction-aware transition is derived from the navigation trail —
 * see [PageDirection] — so the same transition reads correctly both when navigating
 * deeper and when popping up.
 */
public sealed interface PageTransition {
    /** The duration of the transition, in milliseconds. */
    public val durationMs: Int

    /** The easing of the transition. */
    public val easing: Easing

    /**
     * The endpoints of the transition for a navigation in [direction], across a pane of
     * [width].
     */
    public fun endpoints(direction: PageDirection, width: Dp): PageTransitionEndpoints

    /** No animation: the content swaps instantly (the pre-transition behavior). */
    public data object None : PageTransition {
        override val durationMs: Int get() = 0

        override val easing: Easing get() = LinearEasing

        override fun endpoints(
            direction: PageDirection,
            width: Dp,
        ): PageTransitionEndpoints =
            PageTransitionEndpoints(0.dp, 1f, 0.dp, 0f)
    }

    /** A cross-fade between the outgoing and incoming content. */
    public data object Fade : PageTransition {
        override val durationMs: Int get() = DEFAULT_DURATION_MS

        override val easing: Easing get() = DEFAULT_EASING

        override fun endpoints(
            direction: PageDirection,
            width: Dp,
        ): PageTransitionEndpoints =
            PageTransitionEndpoints(0.dp, 0f, 0.dp, 0f)
    }

    /**
     * A direction-aware horizontal slide (the classic list-detail push): the incoming
     * content slides in from the right when navigating deeper, from the left when
     * popping up, and the outgoing content slides out the other way.
     */
    public data object Slide : PageTransition {
        override val durationMs: Int get() = DEFAULT_DURATION_MS

        override val easing: Easing get() = DEFAULT_EASING

        override fun endpoints(direction: PageDirection, width: Dp): PageTransitionEndpoints {
            val offset = width * (if (direction == PageDirection.Forward) 1f else -1f)
            return PageTransitionEndpoints(offset, 1f, -offset, 1f)
        }
    }

    /** A direction-aware slide blended with a cross-fade. */
    public data object SlideFade : PageTransition {
        override val durationMs: Int get() = DEFAULT_DURATION_MS

        override val easing: Easing get() = DEFAULT_EASING

        override fun endpoints(direction: PageDirection, width: Dp): PageTransitionEndpoints {
            val offset = width * (if (direction == PageDirection.Forward) 1f else -1f)
            return PageTransitionEndpoints(offset, 0.5f, -offset, 0.5f)
        }
    }

    /**
     * A host-defined transition: [transform] resolves the endpoints for the navigation
     * (its [PageDirection] and the pane's width), and the engine interpolates between
     * them over [durationMs] with [easing].
     *
     * @param durationMs The duration of the transition, in milliseconds.
     * @param easing The easing of the transition.
     * @param transform Resolves the endpoints of the transition; see
     *   [PageTransitionEndpoints] for the start/end conventions.
     */
    public data class Custom(
        override val durationMs: Int = DEFAULT_DURATION_MS,
        override val easing: Easing = DEFAULT_EASING,
        public val transform: (PageDirection, Dp) -> PageTransitionEndpoints,
    ) : PageTransition {
        override fun endpoints(
            direction: PageDirection,
            width: Dp,
        ): PageTransitionEndpoints =
            transform(direction, width)
    }

    public companion object {
        /** The default duration of the built-in transitions, in milliseconds. */
        public const val DEFAULT_DURATION_MS: Int = 300

        /** The default easing of the built-in transitions. */
        public val DEFAULT_EASING: Easing = FastOutSlowInEasing
    }
}
