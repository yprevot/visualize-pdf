package com.example.androidpdfviewwer

import android.graphics.Bitmap
import android.net.Uri

enum class PdfError {
    PASSWORD_REQUIRED,
    CORRUPT,
    TOO_LARGE,
    IO,
    OOM,
    UNKNOWN
}

sealed interface PdfOpenResult {
    data class Success(val pageCount: Int) : PdfOpenResult
    data class Failure(val error: PdfError, val cause: Throwable? = null) : PdfOpenResult
}

interface PdfEngine {
    val pageCount: Int

    fun openPdf(uri: Uri, password: String? = null): PdfOpenResult

    fun renderPage(pageIndex: Int, targetWidth: Int, onRendered: (Bitmap) -> Unit)

    /** Aspect ratio (width / height) if known, null otherwise. */
    fun pageAspect(pageIndex: Int): Float?

    fun cancelPending(pageIndex: Int) {}

    fun close()
    fun shutdown()
}

const val MAX_BITMAP_SIDE = 4096
const val MAX_BITMAP_PIXELS = 4096 * 4096

/**
 * Pure function: target bitmap size preserving aspect ratio, clamped to the
 * max-texture side ([MAX_BITMAP_SIDE]) and to [MAX_BITMAP_PIXELS] total.
 * Prevents OOM / black pages on huge documents (A3, posters, 300dpi scans).
 */
internal fun computeCappedSize(srcW: Int, srcH: Int, targetWidth: Int): Pair<Int, Int> {
    require(srcW > 0 && srcH > 0)
    var width = if (targetWidth > 0) targetWidth else srcW
    var height = (width.toLong() * srcH / srcW).toInt().coerceAtLeast(1)
    if (width > MAX_BITMAP_SIDE || height > MAX_BITMAP_SIDE) {
        val scale = MAX_BITMAP_SIDE.toFloat() / maxOf(width, height)
        width = (width * scale).toInt().coerceAtLeast(1)
        height = (height * scale).toInt().coerceAtLeast(1)
    }
    val pixels = width.toLong() * height
    if (pixels > MAX_BITMAP_PIXELS) {
        val scale = kotlin.math.sqrt(MAX_BITMAP_PIXELS.toDouble() / pixels)
        width = (width * scale).toInt().coerceAtLeast(1)
        height = (height * scale).toInt().coerceAtLeast(1)
    }
    return width to height
}
