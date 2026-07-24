package com.example.todoaccesible.export.pdf

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.todoaccesible.R
import com.example.todoaccesible.data.local.entities.DiagnosticEntity
import com.example.todoaccesible.data.model.Nivel
import com.example.todoaccesible.domain.scoring.CreditScore
import com.example.todoaccesible.domain.scoring.ScorecardResult
import com.example.todoaccesible.domain.scoring.SectionScore
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Genera el PDF del scorecard replicando la portada de referencia
 * "Todo Accesible v2.6": logo, datos del proyecto, cuantificación parcial
 * (Required en azul marino #0B2545, Plus en fucsia) y las 8 tarjetas de
 * sección. Dibuja directamente con PdfDocument+Canvas para tener control
 * exacto de layout y colores sin depender de una librería externa.
 */
object PdfScorecardGenerator {

    private const val PAGE_WIDTH = 612 // US Letter @ 72dpi
    private const val PAGE_HEIGHT = 792
    private val REQUIRED_COLOR = Color.parseColor("#0B2545")
    private val PLUS_COLOR = Color.parseColor("#EC1E79")
    private val TEXT_COLOR = Color.parseColor("#1A1A1A")
    private val MUTED_COLOR = Color.parseColor("#6B7280")
    private val CARD_BORDER = Color.parseColor("#D9D9D9")

    fun generate(context: Context, diagnostic: DiagnosticEntity, scorecard: ScorecardResult): File {
        val document = PdfDocument()
        val page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
        val canvas = page.canvas

        drawHeader(context, canvas, diagnostic)
        drawSummaryBoxes(canvas, diagnostic, scorecard)
        drawSectionGrid(canvas, scorecard.sections)
        drawFooterLegend(canvas)

        document.finishPage(page)

        val dir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
        val file = File(dir, "scorecard_${diagnostic.id}.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    private fun drawHeader(context: Context, canvas: Canvas, diagnostic: DiagnosticEntity) {
        val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.logo_todo_accesible)
        val logoWidth = 90f
        val logoHeight = logoWidth * logoBitmap.height / logoBitmap.width
        canvas.drawBitmap(logoBitmap, null, RectF(36f, 28f, 36f + logoWidth, 28f + logoHeight), null)

        val titlePaint = Paint().apply {
            color = TEXT_COLOR
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        canvas.drawText("Scorecard v2.6", 150f, 45f, titlePaint)

        val infoPaint = Paint().apply {
            color = TEXT_COLOR
            textSize = 10f
            isAntiAlias = true
        }
        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale("es", "MX")).format(Date(diagnostic.fechaEnvio ?: diagnostic.fechaCreacion))
        val lines = listOf(
            "Proyecto: ${diagnostic.projectName}",
            "Dirección: ${diagnostic.ubicacion}",
            "Revisión: ${diagnostic.revision}",
            "Fecha: $dateStr",
            "Responsable: ${diagnostic.responsable}"
        )
        lines.forEachIndexed { index, line ->
            canvas.drawText(line, 150f, 65f + index * 14f, infoPaint)
        }
    }

    private fun drawSummaryBoxes(canvas: Canvas, diagnostic: DiagnosticEntity, scorecard: ScorecardResult) {
        val top = 150f
        val boxRect = RectF(36f, top, PAGE_WIDTH - 36f, top + 100f)
        drawRoundedCard(canvas, boxRect)

        val labelPaint = Paint().apply { color = TEXT_COLOR; textSize = 12f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        canvas.drawText("Cuantificación parcial del proyecto", boxRect.left + 16f, boxRect.top + 22f, labelPaint)

        drawCreditBar(canvas, "Required", scorecard.required, boxRect.left + 16f, boxRect.top + 40f, boxRect.width() - 180f, REQUIRED_COLOR)
        drawCreditBar(canvas, "Plus", scorecard.plus, boxRect.left + 16f, boxRect.top + 70f, boxRect.width() - 180f, PLUS_COLOR)

        // Círculo de nivel alcanzado a la derecha
        val nivelCenterX = boxRect.right - 70f
        val nivelCenterY = boxRect.top + 55f
        val circlePaint = Paint().apply { color = nivelColorInt(scorecard.nivel); isAntiAlias = true }
        canvas.drawCircle(nivelCenterX, nivelCenterY, 36f, circlePaint)
        val nivelTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(scorecard.nivel.label.uppercase(), nivelCenterX, nivelCenterY + 4f, nivelTextPaint)
        val nivelCaptionPaint = Paint().apply { color = MUTED_COLOR; textSize = 9f; textAlign = Paint.Align.CENTER; isAntiAlias = true }
        canvas.drawText("Nivel alcanzado", nivelCenterX, boxRect.bottom - 8f, nivelCaptionPaint)
    }

    private fun drawSectionGrid(canvas: Canvas, sections: List<SectionScore>) {
        val startTop = 268f
        val columnWidth = (PAGE_WIDTH - 36f * 2 - 12f) / 2
        val rowHeight = 78f

        sections.sortedBy { it.seccionId.toIntOrNull() ?: 0 }.forEachIndexed { index, section ->
            val column = index % 2
            val row = index / 2
            val left = 36f + column * (columnWidth + 12f)
            val top = startTop + row * (rowHeight + 10f)
            val rect = RectF(left, top, left + columnWidth, top + rowHeight)
            drawRoundedCard(canvas, rect)

            val titlePaint = Paint().apply { color = TEXT_COLOR; textSize = 10.5f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
            canvas.drawText("${section.seccionId}. ${section.seccionNombre}", rect.left + 10f, rect.top + 16f, titlePaint)

            drawCreditBar(canvas, "Required", section.required, rect.left + 10f, rect.top + 30f, rect.width() - 20f, REQUIRED_COLOR, compact = true)
            drawCreditBar(canvas, "Plus", section.plus, rect.left + 10f, rect.top + 54f, rect.width() - 20f, PLUS_COLOR, compact = true)
        }
    }

    private fun drawFooterLegend(canvas: Canvas) {
        val top = PAGE_HEIGHT - 60f
        val paint = Paint().apply { color = MUTED_COLOR; textSize = 8f; isAntiAlias = true }
        canvas.drawText(
            "Niveles distintivo:  Plata 100% créditos Required · Oro 63% al 79% créditos Plus · Magenta 80% al 100% créditos Plus.",
            36f, top, paint
        )
        canvas.drawText(
            "Para Oro y Magenta se debe haber cumplido con el 100% de créditos Required.",
            36f, top + 12f, paint
        )
    }

    private fun drawRoundedCard(canvas: Canvas, rect: RectF) {
        val fillPaint = Paint().apply { color = Color.WHITE; isAntiAlias = true }
        val borderPaint = Paint().apply { color = CARD_BORDER; style = Paint.Style.STROKE; strokeWidth = 1f; isAntiAlias = true }
        canvas.drawRoundRect(rect, 8f, 8f, fillPaint)
        canvas.drawRoundRect(rect, 8f, 8f, borderPaint)
    }

    private fun drawCreditBar(
        canvas: Canvas,
        label: String,
        score: CreditScore,
        left: Float,
        top: Float,
        width: Float,
        color: Int,
        compact: Boolean = false
    ) {
        val labelTextSize = if (compact) 8f else 10f
        val labelPaint = Paint().apply { this.color = TEXT_COLOR; textSize = labelTextSize; isAntiAlias = true }
        canvas.drawText("$label  ${score.aprobados}/${score.total}  (${score.pct}%)", left, top, labelPaint)

        val barTop = top + 4f
        val barHeight = if (compact) 6f else 10f
        val barRect = RectF(left, barTop, left + width, barTop + barHeight)
        val bgPaint = Paint().apply { this.color = Color.argb(40, Color.red(color), Color.green(color), Color.blue(color)); isAntiAlias = true }
        canvas.drawRoundRect(barRect, barHeight / 2, barHeight / 2, bgPaint)

        val fillWidth = width * (score.pct.coerceIn(0, 100) / 100f)
        if (fillWidth > 0f) {
            val fillRect = RectF(left, barTop, left + fillWidth, barTop + barHeight)
            val fillPaint = Paint().apply { this.color = color; isAntiAlias = true }
            canvas.drawRoundRect(fillRect, barHeight / 2, barHeight / 2, fillPaint)
        }
    }

    private fun nivelColorInt(nivel: Nivel): Int = when (nivel) {
        Nivel.EN_PROCESO -> Color.parseColor("#6B7280")
        Nivel.PLATA -> Color.parseColor("#A6A6A6")
        Nivel.ORO -> Color.parseColor("#C9A227")
        Nivel.MAGENTA -> Color.parseColor("#EC1E79")
    }
}
