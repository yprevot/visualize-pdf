package com.example.androidpdfviewwer

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object PdfFileHelper {

    /**
     * Copy any content:// or file:// Uri to a seekable temp file in cacheDir.
     * Fixes PDFs from Drive/OneDrive/Gmail that don't support ParcelFileDescriptor
     * random access, which is the #1 cause of "some documents don't render".
     */
    fun copyToCache(context: Context, uri: Uri, fileName: String = "incoming.pdf"): File? {
        try {
            if (uri.scheme == "file") {
                uri.path?.let { path ->
                    val src = File(path)
                    if (src.exists() && src.canRead()) return src
                }
            }
            val dest = File(context.cacheDir, "pdf_${System.currentTimeMillis()}_$fileName")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            return dest.takeIf { it.exists() && it.length() > 0 }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun fileProviderUri(context: Context, file: File): Uri {
        return androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}
