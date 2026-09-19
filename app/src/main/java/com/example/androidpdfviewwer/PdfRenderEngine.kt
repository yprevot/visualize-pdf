package com.example.androidpdfviewwer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.LruCache
import android.util.Size
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * Native engine (PdfRenderer) hardened:
 * - copies content:// to seekable cache file (Drive/Gmail fix)
 * - caps bitmap size to avoid OOM / max-texture black pages
 * - maps password/corrupt errors instead of generic false
 * - cancelable renders + shutdown() to avoid leaks
 *
 * Kept as fallback when Pdfium is unavailable. Primary path is [PdfiumEngine].
 */
class PdfRenderEngine(private val context: Context) : PdfEngine {

    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    private var cacheFile: File? = null

    private val executor = Executors.newFixedThreadPool(2)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val pending = ConcurrentHashMap<Int, Future<*>>()

    private val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSizeKb = (maxMemoryKb / 8).coerceAtMost(32 * 1024)
    private val bitmapCache = object : LruCache<Int, Bitmap>(cacheSizeKb) {
        override fun sizeOf(key: Int, value: Bitmap): Int = value.byteCount / 1024
        override fun entryRemoved(evicted: Boolean, key: Int, oldValue: Bitmap, newValue: Bitmap?) {
            // Don't recycle here: the ImageView may still hold it. Let GC handle it.
        }
    }
    private val pageSizes = ConcurrentHashMap<Int, Size>()

    override var pageCount: Int = 0
        private set

    override fun openPdf(uri: Uri, password: String?): PdfOpenResult {
        close()
        return try {
            val seekable: File = PdfFileHelper.copyToCache(context, uri)
                ?: return PdfOpenResult.Failure(PdfError.IO)
            // Only delete previous temp copies, never the original file:// source.
            if (seekable != cacheFile && seekable.parentFile == context.cacheDir) {
                cacheFile = seekable
            } else if (seekable.parentFile != context.cacheDir) {
                cacheFile = null
            } else {
                cacheFile = seekable
            }
            fileDescriptor = ParcelFileDescriptor.open(seekable, ParcelFileDescriptor.MODE_READ_ONLY)
            val fd = fileDescriptor ?: return PdfOpenResult.Failure(PdfError.IO)
            try {
                pdfRenderer = PdfRenderer(fd)
            } catch (e: SecurityException) {
                close()
                return PdfOpenResult.Failure(PdfError.PASSWORD_REQUIRED, e)
            } catch (e: java.io.IOException) {
                close()
                val msg = (e.message ?: "").lowercase()
                return if ("password" in msg || "encrypt" in msg) {
                    PdfOpenResult.Failure(PdfError.PASSWORD_REQUIRED, e)
                } else {
                    PdfOpenResult.Failure(PdfError.CORRUPT, e)
                }
            }
            pageCount = pdfRenderer?.pageCount ?: 0
            if (pageCount <= 0) {
                close()
                PdfOpenResult.Failure(PdfError.CORRUPT)
            } else {
                PdfOpenResult.Success(pageCount)
            }
        } catch (e: OutOfMemoryError) {
            close()
            PdfOpenResult.Failure(PdfError.OOM, Exception(e))
        } catch (e: Exception) {
            e.printStackTrace()
            close()
            PdfOpenResult.Failure(PdfError.UNKNOWN, e)
        }
    }

    /** Legacy Boolean API used by older callers. */
    fun openPdfLegacy(uri: Uri): Boolean = openPdf(uri) is PdfOpenResult.Success

    override fun pageAspect(pageIndex: Int): Float? {
        pageSizes[pageIndex]?.let { return it.width.toFloat() / it.height.toFloat() }
        return null
    }

    override fun renderPage(pageIndex: Int, targetWidth: Int, onRendered: (Bitmap) -> Unit) {
        if (pageIndex < 0 || pageIndex >= pageCount || pdfRenderer == null) return

        bitmapCache.get(pageIndex)?.let { cached ->
            if (!cached.isRecycled) {
                onRendered(cached)
                return
            }
        }
        pending[pageIndex]?.cancel(true)

        val future = executor.submit {
            val renderer = pdfRenderer ?: return@submit
            synchronized(renderer) {
                // Renderer may have been closed while queued.
                if (pdfRenderer !== renderer) return@synchronized
                try {
                    val page = renderer.openPage(pageIndex)
                    try {
                        val srcW = page.width
                        val srcH = page.height
                        if (srcW <= 0 || srcH <= 0) return@synchronized
                        pageSizes.putIfAbsent(pageIndex, Size(srcW, srcH))

                        val (width, height) = cappedSize(srcW, srcH, targetWidth)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmapCache.put(pageIndex, bitmap)
                        mainHandler.post { onRendered(bitmap) }
                    } finally {
                        try { page.close() } catch (_: Exception) {}
                    }
                } catch (e: Exception) {
                    if (Thread.currentThread().isInterrupted) return@synchronized
                    e.printStackTrace()
                } finally {
                    pending.remove(pageIndex)
                }
            }
        }
        pending[pageIndex] = future
    }

    override fun cancelPending(pageIndex: Int) {
        pending.remove(pageIndex)?.cancel(true)
    }

    internal fun cappedSize(srcW: Int, srcH: Int, targetWidth: Int): Pair<Int, Int> =
        computeCappedSize(srcW, srcH, targetWidth)

    override fun close() {
        try {
            pending.values.forEach { it.cancel(true) }
            pending.clear()
            bitmapCache.evictAll()
            pageSizes.clear()
            try { pdfRenderer?.close() } catch (_: Exception) {}
            pdfRenderer = null
            try { fileDescriptor?.close() } catch (_: Exception) {}
            fileDescriptor = null
            // Delete only our temp copy.
            cacheFile?.let { f ->
                if (f.parentFile == context.cacheDir && f.name.startsWith("pdf_")) {
                    try { f.delete() } catch (_: Exception) {}
                }
            }
            cacheFile = null
            pageCount = 0
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun shutdown() {
        close()
        executor.shutdownNow()
    }
}
