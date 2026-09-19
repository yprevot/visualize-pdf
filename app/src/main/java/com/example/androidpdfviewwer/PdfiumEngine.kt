package com.example.androidpdfviewwer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.LruCache
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * Primary engine based on Pdfium (via android-pdf-viewer).
 * Handles password-protected files, XFA/forms, JBIG2/JPEG2000, CMYK and
 * slightly corrupt files that android.graphics.pdf.PdfRenderer rejects.
 */
class PdfiumEngine(private val context: Context) : PdfEngine {

    private val pdfiumCore = PdfiumCore(context)
    private var document: PdfDocument? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var cacheFile: File? = null

    private val executor = Executors.newFixedThreadPool(2)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val pending = ConcurrentHashMap<Int, Future<*>>()

    private val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val bitmapCache = object : LruCache<Int, Bitmap>(maxMemoryKb / 8) {
        override fun sizeOf(key: Int, value: Bitmap): Int = value.byteCount / 1024
    }

    override var pageCount: Int = 0
        private set

    override fun openPdf(uri: Uri, password: String?): PdfOpenResult {
        close()
        return try {
            val seekable = PdfFileHelper.copyToCache(context, uri)
                ?: return PdfOpenResult.Failure(PdfError.IO)
            if (seekable.parentFile == context.cacheDir) cacheFile = seekable
            fileDescriptor = ParcelFileDescriptor.open(seekable, ParcelFileDescriptor.MODE_READ_ONLY)
            val fd = fileDescriptor ?: return PdfOpenResult.Failure(PdfError.IO)
            val doc: PdfDocument = try {
                if (password.isNullOrEmpty()) pdfiumCore.newDocument(fd)
                else pdfiumCore.newDocument(fd, password)
            } catch (e: Exception) {
                close()
                return mapOpenException(e)
            }
            document = doc
            pageCount = try {
                pdfiumCore.getPageCount(doc)
            } catch (e: Exception) {
                close()
                return PdfOpenResult.Failure(PdfError.CORRUPT, e)
            }
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

    private fun mapOpenException(e: Exception): PdfOpenResult.Failure {
        val name = e.javaClass.simpleName.lowercase()
        val msg = (e.message ?: "").lowercase()
        return if ("password" in name || "password" in msg || "encrypt" in msg) {
            PdfOpenResult.Failure(PdfError.PASSWORD_REQUIRED, e)
        } else {
            PdfOpenResult.Failure(PdfError.CORRUPT, e)
        }
    }

    override fun pageAspect(pageIndex: Int): Float? {
        val doc = document ?: return null
        return try {
            synchronized(doc) {
                pdfiumCore.openPage(doc, pageIndex)
                val w = pdfiumCore.getPageWidthPoint(doc, pageIndex)
                val h = pdfiumCore.getPageHeightPoint(doc, pageIndex)
                if (w > 0 && h > 0) w.toFloat() / h else null
                // NOTE: pdfium-android 1.9.x has no closePage(); pages are
                // released when the document is closed.
            }
        } catch (_: Exception) { null }
    }

    override fun renderPage(pageIndex: Int, targetWidth: Int, onRendered: (Bitmap) -> Unit) {
        val doc = document ?: return
        if (pageIndex < 0 || pageIndex >= pageCount) return
        bitmapCache.get(pageIndex)?.let {
            if (!it.isRecycled) {
                onRendered(it)
                return
            }
        }
        pending[pageIndex]?.cancel(true)
        val future = executor.submit {
            synchronized(doc) {
                if (document !== doc) return@synchronized
                try {
                    pdfiumCore.openPage(doc, pageIndex)
                    val srcW = pdfiumCore.getPageWidthPoint(doc, pageIndex)
                    val srcH = pdfiumCore.getPageHeightPoint(doc, pageIndex)
                    if (srcW <= 0 || srcH <= 0) return@synchronized
                    val (width, height) = cappedSize(srcW, srcH, targetWidth)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    pdfiumCore.renderPageBitmap(doc, bitmap, pageIndex, 0, 0, width, height, true)
                    bitmapCache.put(pageIndex, bitmap)
                    mainHandler.post { onRendered(bitmap) }
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

    private fun cappedSize(srcW: Int, srcH: Int, targetWidth: Int): Pair<Int, Int> =
        computeCappedSize(srcW, srcH, targetWidth)

    override fun cancelPending(pageIndex: Int) {
        pending.remove(pageIndex)?.cancel(true)
    }

    override fun close() {
        try {
            pending.values.forEach { it.cancel(true) }
            pending.clear()
            bitmapCache.evictAll()
            document?.let { doc ->
                try { pdfiumCore.closeDocument(doc) } catch (_: Exception) {}
            }
            document = null
            try { fileDescriptor?.close() } catch (_: Exception) {}
            fileDescriptor = null
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
