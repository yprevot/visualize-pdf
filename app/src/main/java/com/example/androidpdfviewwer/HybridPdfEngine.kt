package com.example.androidpdfviewwer

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri

/**
 * Tries Pdfium first (best compatibility), falls back to native PdfRenderer.
 * Remembers which engine succeeded so renders go to the right one.
 */
class HybridPdfEngine(context: Context) : PdfEngine {

    private val pdfium = PdfiumEngine(context)
    private val native = PdfRenderEngine(context)
    private var active: PdfEngine? = null

    override val pageCount: Int get() = active?.pageCount ?: 0

    override fun openPdf(uri: Uri, password: String?): PdfOpenResult {
        // Close both before trying.
        pdfium.close()
        native.close()
        active = null

        val first = pdfium.openPdf(uri, password)
        if (first is PdfOpenResult.Success) {
            active = pdfium
            return first
        }
        // Don't fallback on password-required: user must type it for whichever engine.
        if (first is PdfOpenResult.Failure && first.error == PdfError.PASSWORD_REQUIRED) {
            return first
        }
        val second = native.openPdf(uri, password)
        if (second is PdfOpenResult.Success) {
            active = native
            // Pdfium already closed; release its temp copy handling is independent.
            return second
        }
        // Prefer the Pdfium error (more accurate) unless native gave password signal.
        return if (second is PdfOpenResult.Failure && second.error == PdfError.PASSWORD_REQUIRED) second else first
    }

    override fun renderPage(pageIndex: Int, targetWidth: Int, onRendered: (Bitmap) -> Unit) {
        active?.renderPage(pageIndex, targetWidth, onRendered)
    }

    override fun pageAspect(pageIndex: Int): Float? = active?.pageAspect(pageIndex)

    override fun cancelPending(pageIndex: Int) {
        active?.cancelPending(pageIndex)
    }

    override fun close() {
        try { pdfium.close() } catch (_: Exception) {}
        try { native.close() } catch (_: Exception) {}
        active = null
    }

    override fun shutdown() {
        try { pdfium.shutdown() } catch (_: Exception) {}
        try { native.shutdown() } catch (_: Exception) {}
        active = null
    }
}
