package net.slions.compose.toolkit

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class PageTransitionTest {

    private val width = 360.dp

    @Test
    fun noneEndpointsSwapInstantly() {
        val e = PageTransition.None.endpoints(PageDirection.Forward, width)
        assertEquals(0.dp, e.enterOffset)
        assertEquals(1f, e.enterAlpha)
        assertEquals(0.dp, e.exitOffset)
        assertEquals(0f, e.exitAlpha)
    }

    @Test
    fun fadeHasNoOffsetsAndIgnoresDirection() {
        val forward = PageTransition.Fade.endpoints(PageDirection.Forward, width)
        assertEquals(0.dp, forward.enterOffset)
        assertEquals(0f, forward.enterAlpha)
        assertEquals(0.dp, forward.exitOffset)
        assertEquals(0f, forward.exitAlpha)
        assertEquals(forward, PageTransition.Fade.endpoints(PageDirection.Backward, width))
    }

    @Test
    fun slideForwardEntersFromTheRight() {
        val e = PageTransition.Slide.endpoints(PageDirection.Forward, width)
        assertEquals(width, e.enterOffset)
        assertEquals(1f, e.enterAlpha)
        assertEquals(-width, e.exitOffset)
        assertEquals(1f, e.exitAlpha)
    }

    @Test
    fun slideBackwardIsMirrored() {
        val e = PageTransition.Slide.endpoints(PageDirection.Backward, width)
        assertEquals(-width, e.enterOffset)
        assertEquals(1f, e.enterAlpha)
        assertEquals(width, e.exitOffset)
        assertEquals(1f, e.exitAlpha)
    }

    @Test
    fun slideFadeBlendsHalfAlpha() {
        val e = PageTransition.SlideFade.endpoints(PageDirection.Forward, width)
        assertEquals(width, e.enterOffset)
        assertEquals(0.5f, e.enterAlpha)
        assertEquals(-width, e.exitOffset)
        assertEquals(0.5f, e.exitAlpha)
        // The backward variant mirrors the offsets and keeps the same alphas.
        val back = PageTransition.SlideFade.endpoints(PageDirection.Backward, width)
        assertEquals(-width, back.enterOffset)
        assertEquals(width, back.exitOffset)
        assertEquals(0.5f, back.enterAlpha)
        assertEquals(0.5f, back.exitAlpha)
    }

    @Test
    fun customDelegatesToItsTransform() {
        val transition =
            PageTransition.Custom(
                durationMs = 120,
                transform = { _, w -> PageTransitionEndpoints(w / 2, 0.25f, -w / 2, 0.25f) },
            )
        assertEquals(120, transition.durationMs)
        val e = transition.endpoints(PageDirection.Forward, width)
        assertEquals(width / 2, e.enterOffset)
        assertEquals(0.25f, e.enterAlpha)
        assertEquals(-width / 2, e.exitOffset)
        assertEquals(0.25f, e.exitAlpha)
    }

    @Test
    fun catalogOptionsDefaults() {
        val options = CatalogOptions()
        assertEquals(PageTransition.Slide, options.pageTransition)
        assertEquals(24.dp, options.paneGap)
        // The default slide has the canonical duration and easing.
        assertEquals(300, PageTransition.Slide.durationMs)
        assertEquals(FastOutSlowInEasing, PageTransition.Slide.easing)
    }
}
