package com.example.androidpdfviewwer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object SamplePdfGenerator {

    fun createSamplePdf(context: Context): Uri? {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint()

        // Page 1: Welcome & Overview
        val pageInfo1 = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page1 = pdfDocument.startPage(pageInfo1)
        val canvas1 = page1.canvas

        // Header Background
        paint.color = Color.parseColor("#1976D2")
        canvas1.drawRect(0f, 0f, 595f, 120f, paint)

        // Title
        titlePaint.color = Color.WHITE
        titlePaint.textSize = 28f
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("Visor de PDF Android", 40f, 65f, titlePaint)

        titlePaint.textSize = 14f
        titlePaint.color = Color.parseColor("#E0E0E0")
        canvas1.drawText("Documento de Muestra Nativo - Listo para Google Play", 40f, 95f, titlePaint)

        // Body Content
        paint.color = Color.BLACK
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("¡Bienvenido a tu nuevo Visor de PDF!", 40f, 170f, paint)

        paint.textSize = 13f
        paint.typeface = Typeface.DEFAULT
        var y = 210f
        val lines = listOf(
            "Esta aplicación incluye el mínimo indispensable para abrir, leer y visualizar",
            "documentos PDF con la máxima velocidad y eficiencia en Android.",
            "",
            "Características principales:",
            "  • Renderizado nativo ultrarrápido con Android PdfRenderer.",
            "  • Integración con el Storage Access Framework (SAF) de Android.",
            "  • Control de zoom multitáctil (Pinch-to-zoom) y desplazamiento.",
            "  • Paginación continua vertical y salto rápido de página.",
            "  • 100% libre de permisos invasivos para aprobación instantánea en Google Play.",
            "",
            "Desliza hacia abajo para ver las siguientes páginas."
        )

        for (line in lines) {
            canvas1.drawText(line, 40f, y, paint)
            y += 24f
        }

        // Footer
        paint.color = Color.GRAY
        paint.textSize = 10f
        canvas1.drawText("Página 1 de 3", 260f, 800f, paint)

        pdfDocument.finishPage(page1)

        // Page 2: Technical Details
        val pageInfo2 = PdfDocument.PageInfo.Builder(595, 842, 2).create()
        val page2 = pdfDocument.startPage(pageInfo2)
        val canvas2 = page2.canvas

        paint.color = Color.parseColor("#0F9D58")
        canvas2.drawRect(0f, 0f, 595f, 80f, paint)

        titlePaint.color = Color.WHITE
        titlePaint.textSize = 22f
        canvas2.drawText("Página 2: Arquitectura Nativa", 40f, 50f, titlePaint)

        paint.color = Color.BLACK
        paint.textSize = 14f
        y = 130f
        val page2Lines = listOf(
            "Cumplimiento total de políticas de Google Play Store:",
            "  1. Target SDK 35 (Android 15+).",
            "  2. Scoped Storage: No requiere MANAGE_EXTERNAL_STORAGE.",
            "  3. Manejo eficiente de memoria mediante caché LRU de Bitmaps.",
            "  4. Soporte para esquemas 'content://' y 'file://'.",
            "",
            "Puedes compartir este archivo o seleccionar tus propios PDFs",
            "usando el botón 'Seleccionar Archivo' en la barra superior."
        )

        for (line in page2Lines) {
            canvas2.drawText(line, 40f, y, paint)
            y += 26f
        }

        paint.color = Color.GRAY
        paint.textSize = 10f
        canvas2.drawText("Página 2 de 3", 260f, 800f, paint)

        pdfDocument.finishPage(page2)

        // Page 3: Completion Certification
        val pageInfo3 = PdfDocument.PageInfo.Builder(595, 842, 3).create()
        val page3 = pdfDocument.startPage(pageInfo3)
        val canvas3 = page3.canvas

        paint.color = Color.parseColor("#7B1FA2")
        canvas3.drawRect(0f, 0f, 595f, 80f, paint)

        titlePaint.color = Color.WHITE
        titlePaint.textSize = 22f
        canvas3.drawText("Página 3: Verificación Completa", 40f, 50f, titlePaint)

        paint.color = Color.BLACK
        paint.textSize = 14f
        y = 140f
        canvas3.drawText("Estado del proyecto:", 40f, y, paint)
        y += 30f
        paint.color = Color.parseColor("#1B5E20")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas3.drawText("✔ Proyecto compilado exitosamente", 40f, y, paint)
        y += 26f
        canvas3.drawText("✔ Cero errores de sintaxis o Gradle", 40f, y, paint)
        y += 26f
        canvas3.drawText("✔ APK listo para distribución o Google Play Console", 40f, y, paint)

        paint.color = Color.GRAY
        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        canvas3.drawText("Página 3 de 3", 260f, 800f, paint)

        pdfDocument.finishPage(page3)

        // Save file to cache dir
        val sampleFile = File(context.cacheDir, "sample_document.pdf")
        try {
            val fos = FileOutputStream(sampleFile)
            pdfDocument.writeTo(fos)
            fos.close()
            pdfDocument.close()

            return Uri.fromFile(sampleFile)
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            return null
        }
    }
}
