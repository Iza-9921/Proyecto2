package com.example.todoaccesible.export.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
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

    fun generate(context: Context, diagnostic: DiagnosticEntity, scorecard: ScorecardResult, esDefinitivo: Boolean = false): File {
        val document = PdfDocument()
        val page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
        val canvas = page.canvas

        drawHeader(context, canvas, diagnostic, esDefinitivo)
        drawSummaryBoxes(canvas, diagnostic, scorecard)
        val gridBottom = drawSectionGrid(canvas, scorecard.sections)
        var bottom = gridBottom + 14f
        if (esDefinitivo && diagnostic.fechaValidacion != null) {
            bottom = drawValidationInfo(canvas, diagnostic, bottom) + 10f
        }
        drawBadgesAndLegend(context, canvas, bottom)

        document.finishPage(page)

        val dir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
        val file = File(dir, "scorecard_${diagnostic.id}.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    private fun drawHeader(context: Context, canvas: Canvas, diagnostic: DiagnosticEntity, esDefinitivo: Boolean) {
        val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.logo_todo_accesible_pdf)
        val logoWidth = 90f
        val logoHeight = logoWidth * logoBitmap.height / logoBitmap.width
        canvas.drawBitmap(logoBitmap, null, RectF(36f, 28f, 36f + logoWidth, 28f + logoHeight), null)

        decodeCompanyLogo(context, diagnostic.logoEmpresaUri)?.let { empresaLogo ->
            val maxWidth = 90f
            val maxHeight = 60f
            var empresaWidth = maxWidth
            var empresaHeight = empresaWidth * empresaLogo.height / empresaLogo.width
            if (empresaHeight > maxHeight) {
                empresaHeight = maxHeight
                empresaWidth = empresaHeight * empresaLogo.width / empresaLogo.height
            }
            val right = PAGE_WIDTH - 36f
            canvas.drawBitmap(empresaLogo, null, RectF(right - empresaWidth, 28f, right, 28f + empresaHeight), null)
        }

        val titlePaint = Paint().apply {
            color = TEXT_COLOR
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        val tituloSufijo = if (esDefinitivo) " — Resultado oficial (Definitivo)" else " — (Preliminar)"
        canvas.drawText("Scorecard v2.6$tituloSufijo", 150f, 45f, titlePaint)

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

        // Círculo de nivel alcanzado a la derecha: no se dibuja para "En proceso"
        // (no es un nivel distintivo, solo el estado antes de alcanzar Plata).
        if (scorecard.nivel != Nivel.EN_PROCESO) {
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
    }

    /** Devuelve la coordenada Y del borde inferior de la última fila, para poder acomodar el contenido siguiente. */
    private fun drawSectionGrid(canvas: Canvas, sections: List<SectionScore>): Float {
        val startTop = 268f
        val columnWidth = (PAGE_WIDTH - 36f * 2 - 12f) / 2
        val rowHeight = 70f
        val rowGap = 8f
        var maxBottom = startTop

        sections.sortedBy { it.seccionId.toIntOrNull() ?: 0 }.forEachIndexed { index, section ->
            val column = index % 2
            val row = index / 2
            val left = 36f + column * (columnWidth + 12f)
            val top = startTop + row * (rowHeight + rowGap)
            val rect = RectF(left, top, left + columnWidth, top + rowHeight)
            drawRoundedCard(canvas, rect)

            val titlePaint = Paint().apply { color = TEXT_COLOR; textSize = 10.5f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
            canvas.drawText("${section.seccionId}. ${section.seccionNombre}", rect.left + 10f, rect.top + 16f, titlePaint)

            drawCreditBar(canvas, "Required", section.required, rect.left + 10f, rect.top + 30f, rect.width() - 20f, REQUIRED_COLOR, compact = true)
            drawCreditBar(canvas, "Plus", section.plus, rect.left + 10f, rect.top + 54f, rect.width() - 20f, PLUS_COLOR, compact = true)

            maxBottom = maxOf(maxBottom, rect.bottom)
        }
        return maxBottom
    }

    /** Datos de la validación oficial del admin. Devuelve la Y donde terminó de dibujar, para acomodar lo siguiente. */
    private fun drawValidationInfo(canvas: Canvas, diagnostic: DiagnosticEntity, startTop: Float): Float {
        val titlePaint = Paint().apply { color = TEXT_COLOR; textSize = 10.5f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val infoPaint = Paint().apply { color = TEXT_COLOR; textSize = 9.5f; isAntiAlias = true }

        var top = startTop + 12f
        canvas.drawText("Validación del administrador", 36f, top, titlePaint)
        top += 16f

        diagnostic.validadoPorNombre?.let {
            canvas.drawText("Validado por: $it", 36f, top, infoPaint)
            top += 14f
        }
        diagnostic.fechaValidacion?.let {
            val fechaStr = SimpleDateFormat("dd/MM/yyyy", Locale("es", "MX")).format(Date(it))
            canvas.drawText("Fecha de validación: $fechaStr", 36f, top, infoPaint)
            top += 14f
        }
        if (!diagnostic.observacionesAdmin.isNullOrBlank()) {
            canvas.drawText("Observaciones: ${diagnostic.observacionesAdmin}", 36f, top, infoPaint)
            top += 14f
        }
        return top
    }

    /**
     * Fila inferior con las tres insignias (Plata/Oro/Magenta) a la izquierda
     * y la tarjeta "Niveles distintivo" a la derecha, replicando la portada
     * de referencia.
     */
    private fun drawBadgesAndLegend(context: Context, canvas: Canvas, top: Float) {
        val badgeSize = 56f
        val badgeGap = 14f
        val badges = listOf(
            Triple(R.drawable.badge_plata, "Plata", nivelColorInt(Nivel.PLATA)),
            Triple(R.drawable.badge_oro, "Oro", nivelColorInt(Nivel.ORO)),
            Triple(R.drawable.badge_magenta, "Magenta", nivelColorInt(Nivel.MAGENTA))
        )
        badges.forEachIndexed { index, (resId, label, accentColor) ->
            val left = 36f + index * (badgeSize + badgeGap)
            val bitmap = BitmapFactory.decodeResource(context.resources, resId)
            canvas.drawBitmap(bitmap, null, RectF(left, top, left + badgeSize, top + badgeSize), null)
            val captionPaint = Paint().apply {
                color = accentColor
                textSize = 8.5f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText(label, left + badgeSize / 2f, top + badgeSize + 12f, captionPaint)
        }

        val cardLeft = 250f
        val cardRight = PAGE_WIDTH - 36f
        val cardHeight = 76f
        val cardRect = RectF(cardLeft, top, cardRight, top + cardHeight)
        drawRoundedCard(canvas, cardRect)

        val titlePaint = Paint().apply { color = TEXT_COLOR; textSize = 10f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        canvas.drawText("Niveles distintivo:", cardLeft + 14f, top + 18f, titlePaint)

        val labelPaint = Paint().apply { color = TEXT_COLOR; textSize = 9f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val descPaint = Paint().apply { color = TEXT_COLOR; textSize = 9f; isAntiAlias = true }
        val rows = listOf(
            "Plata" to "100% Créditos Required",
            "Oro" to "63% al 79% Créditos Plus",
            "Magenta" to "80% al 100% créditos Plus"
        )
        rows.forEachIndexed { index, (label, desc) ->
            val y = top + 36f + index * 14f
            canvas.drawText(label, cardLeft + 14f, y, labelPaint)
            canvas.drawText(desc, cardLeft + 74f, y, descPaint)
        }

        val footnotePaint = Paint().apply { color = MUTED_COLOR; textSize = 7.5f; isAntiAlias = true }
        canvas.drawText(
            "Para Oro y Magenta se debe haber cumplido con el 100% de créditos Required.",
            36f, top + cardHeight + 14f, footnotePaint
        )
    }

    private fun decodeCompanyLogo(context: Context, uriStr: String?): Bitmap? {
        if (uriStr.isNullOrBlank()) return null
        return try {
            context.contentResolver.openInputStream(Uri.parse(uriStr))?.use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            null
        }
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
