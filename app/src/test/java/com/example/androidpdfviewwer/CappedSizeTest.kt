package com.example.androidpdfviewwer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Local unit tests for the bitmap-size guard shared by both render engines.
 * A wrong size here means black pages, OOM crashes or max-texture failures
 * on large documents (A3, posters, 300dpi scans).
 */
class CappedSizeTest {

    @Test
    fun normalPage_keepsAspect() {
        // A4 @ 595x842 rendered at 1080 wide.
        val (w, h) = computeCappedSize(595, 842, 1080)
        assertEquals(1080, w)
        assertEquals((1080L * 842 / 595).toInt(), h)
    }

    @Test
    fun zeroTargetWidth_usesSourceSize() {
        val (w, h) = computeCappedSize(595, 842, 0)
        assertEquals(595, w)
        assertEquals(842, h)
    }

    @Test
    fun hugePage_clampsLongSide() {
        // Poster 8000x12000 must fit into MAX_BITMAP_SIDE.
        val (w, h) = computeCappedSize(8000, 12000, 8000)
        assertTrue("width=$w", w <= MAX_BITMAP_SIDE)
        assertTrue("height=$h", h <= MAX_BITMAP_SIDE)
        // Aspect preserved within 1px rounding.
        assertEquals(12000.0 / 8000.0, h.toDouble() / w, 0.01)
    }

    @Test
    fun hugePixelCount_clampsTotalPixels() {
        // Wide banner: side clamp alone is not enough, pixel clamp must kick in.
        val (w, h) = computeCappedSize(4000, 8000, 4000)
        assertTrue("pixels=${w * h}", w.toLong() * h <= MAX_BITMAP_PIXELS)
        assertTrue(w >= 1 && h >= 1)
    }

    @Test
    fun landscapePage_keepsAspect() {
        val (w, h) = computeCappedSize(842, 595, 1080)
        assertEquals(1080, w)
        assertEquals((1080L * 595 / 842).toInt(), h)
    }
}
