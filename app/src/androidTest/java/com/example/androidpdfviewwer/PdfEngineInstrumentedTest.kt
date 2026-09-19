package com.example.androidpdfviewwer

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Device tests covering the QA matrix basics:
 * sample document opens on BOTH engines, one page renders to a valid bitmap,
 * corrupt files report Failure (not a crash), and the manifest exposes the
 * VIEW/SEND intent filters for file managers, mail and messaging apps.
 */
@RunWith(AndroidJUnit4::class)
class PdfEngineInstrumentedTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun samplePdf_opensOnHybridWith3Pages() {
        val uri = SamplePdfGenerator.createSamplePdf(context)!!
        val engine = HybridPdfEngine(context)
        try {
            val result = engine.openPdf(uri)
            assertTrue("expected Success, got $result", result is PdfOpenResult.Success)
            assertEquals(3, (result as PdfOpenResult.Success).pageCount)
        } finally {
            engine.shutdown()
        }
    }

    @Test
    fun samplePdf_opensOnNativeEngine() {
        val uri = SamplePdfGenerator.createSamplePdf(context)!!
        val engine = PdfRenderEngine(context)
        try {
            val result = engine.openPdf(uri)
            assertTrue("expected Success, got $result", result is PdfOpenResult.Success)
        } finally {
            engine.shutdown()
        }
    }

    @Test
    fun samplePdf_opensOnPdfiumEngine() {
        val uri = SamplePdfGenerator.createSamplePdf(context)!!
        val engine = PdfiumEngine(context)
        try {
            val result = engine.openPdf(uri)
            assertTrue("expected Success, got $result", result is PdfOpenResult.Success)
        } finally {
            engine.shutdown()
        }
    }

    @Test
    fun hybrid_rendersFirstPageToValidBitmap() {
        val uri = SamplePdfGenerator.createSamplePdf(context)!!
        val engine = HybridPdfEngine(context)
        try {
            assertTrue(engine.openPdf(uri) is PdfOpenResult.Success)
            val latch = CountDownLatch(1)
            var width = 0
            var height = 0
            engine.renderPage(0, 1080) { bitmap ->
                width = bitmap.width
                height = bitmap.height
                latch.countDown()
            }
            assertTrue("render timed out", latch.await(15, TimeUnit.SECONDS))
            assertTrue("width=$width", width > 0)
            assertTrue("height=$height", height > 0)
        } finally {
            engine.shutdown()
        }
    }

    @Test
    fun corruptFile_reportsFailureInsteadOfCrash() {
        val bad = File(context.cacheDir, "corrupt_test.pdf")
        bad.writeBytes(byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x2D, 0x62, 0x72, 0x6F, 0x6B, 0x65, 0x6E))
        val uri = PdfFileHelper.fileProviderUri(context, bad)
        val engine = HybridPdfEngine(context)
        try {
            val result = engine.openPdf(uri)
            assertTrue("expected Failure, got $result", result is PdfOpenResult.Failure)
        } finally {
            engine.shutdown()
            bad.delete()
        }
    }

    @Test
    fun manifest_exposesViewAndSendFilters() {
        val pm = context.packageManager
        // Real senders (file managers, Gmail, Drive) always attach a content://
        // URI, which is what our VIEW filters declare. A type-only intent
        // without data must NOT be used here: it can never match a
        // scheme-qualified filter, by PackageManager matching rules.
        val sampleUri = SamplePdfGenerator.createSamplePdf(context)!!
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(sampleUri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val sendIntent = Intent(Intent.ACTION_SEND).apply { type = "application/pdf" }
        assertTrue(
            "no activity handles ACTION_VIEW/pdf",
            pm.queryIntentActivities(viewIntent, 0).any { it.activityInfo.packageName == context.packageName }
        )
        assertTrue(
            "no activity handles ACTION_SEND/pdf",
            pm.queryIntentActivities(sendIntent, 0).any { it.activityInfo.packageName == context.packageName }
        )
    }
}
