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
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

class PdfRenderEngine(private val context: Context) {

    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null

    private val executor = Executors.newFixedThreadPool(2)
    private val mainHandler = Handler(Looper.getMainLooper())

    // LRU Memory Cache to keep rendered pages fast without crashing on memory limit
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8
    private val bitmapCache = object : LruCache<Int, Bitmap>(cacheSize) {
        override fun sizeOf(key: Int, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }

    var pageCount: Int = 0
        private set

    fun openPdf(uri: Uri): Boolean {
        close()
        return try {
            fileDescriptor = if (uri.scheme == "file") {
                val file = File(uri.path ?: return false)
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            } else {
                context.contentResolver.openFileDescriptor(uri, "r")
            }

            if (fileDescriptor != null) {
                pdfRenderer = PdfRenderer(fileDescriptor!!)
                pageCount = pdfRenderer?.pageCount ?: 0
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            close()
            false
        }
    }

    fun renderPage(pageIndex: Int, targetWidth: Int, onRendered: (Bitmap) -> Unit) {
        if (pageIndex < 0 || pageIndex >= pageCount || pdfRenderer == null) return

        val cachedBitmap = bitmapCache.get(pageIndex)
        if (cachedBitmap != null && !cachedBitmap.isRecycled) {
            onRendered(cachedBitmap)
            return
        }

        executor.execute {
            val renderer = pdfRenderer ?: return@execute
            synchronized(renderer) {
                try {
                    val page = renderer.openPage(pageIndex)
                    val pageWidth = page.width
                    val pageHeight = page.height

                    val width = if (targetWidth > 0) targetWidth else pageWidth
                    val height = (width * pageHeight) / pageWidth

                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)

                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    bitmapCache.put(pageIndex, bitmap)

                    mainHandler.post {
                        onRendered(bitmap)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun close() {
        try {
            bitmapCache.evictAll()
            pdfRenderer?.close()
            pdfRenderer = null
            fileDescriptor?.close()
            fileDescriptor = null
            pageCount = 0
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
