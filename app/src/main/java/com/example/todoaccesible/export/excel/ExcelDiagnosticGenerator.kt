package com.example.todoaccesible.export.excel

import android.content.Context
import android.graphics.BitmapFactory
import com.example.todoaccesible.R
import com.example.todoaccesible.data.local.entities.DiagnosticEntity
import com.example.todoaccesible.data.local.entities.SectionEntity
import com.example.todoaccesible.data.model.AnswerValue
import com.example.todoaccesible.data.model.Credito
import com.example.todoaccesible.ui.admin.review.ReviewRow
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFColor
import org.apache.poi.xssf.usermodel.XSSFFont
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Excel detallado del diagnóstico, mismo formato que `exportarDiagnosticoExcel`
 * de la web: portada con logo, datos del proyecto, tabla de preguntas con
 * columnas AP/P/NC coloreadas (verde/ámbar/rojo), crédito Required/Plus y
 * comentarios.
 *
 * Nota: no se usa `java.awt.Color` (no existe en Android) — los colores se
 * arman con `XSSFColor(byteArrayOf(r, g, b), null)`.
 */
object ExcelDiagnosticGenerator {

    fun generate(context: Context, diagnostic: DiagnosticEntity, rows: List<ReviewRow>, sections: List<SectionEntity>): File {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Scorecard")

        sheet.setColumnWidth(0, 6 * 256)
        sheet.setColumnWidth(1, 5 * 256)
        sheet.setColumnWidth(2, 5 * 256)
        sheet.setColumnWidth(3, 5 * 256)
        sheet.setColumnWidth(4, 11 * 256)
        sheet.setColumnWidth(5, 55 * 256)
        sheet.setColumnWidth(6, 45 * 256)

        val styles = Styles(workbook)

        var rowIndex = 0
        rowIndex = drawCoverImage(context, workbook, sheet, rowIndex)
        rowIndex = writeProjectInfo(sheet, styles, diagnostic, rowIndex)
        rowIndex = writeLegend(sheet, styles, rowIndex)
        rowIndex = writeTableHeader(sheet, styles, rowIndex)
        writeQuestionRows(sheet, styles, rows, sections, rowIndex)

        val dir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
        val file = File(dir, "diagnostico_${diagnostic.id}.xlsx")
        FileOutputStream(file).use { workbook.write(it) }
        workbook.close()
        return file
    }

    private fun drawCoverImage(context: Context, workbook: XSSFWorkbook, sheet: XSSFSheet, startRow: Int): Int {
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.badge_distintivo)
        val stream = ByteArrayOutputStream()
        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
        val pictureIndex = workbook.addPicture(stream.toByteArray(), XSSFWorkbook.PICTURE_TYPE_PNG)

        val drawing = sheet.createDrawingPatriarch()
        val anchor = workbook.creationHelper.createClientAnchor()
        anchor.setCol1(0)
        anchor.row1 = startRow
        anchor.setCol2(1)
        anchor.row2 = startRow + 5
        val picture = drawing.createPicture(anchor, pictureIndex)
        picture.resize(0.6)

        return startRow + 6
    }

    private fun writeProjectInfo(sheet: XSSFSheet, styles: Styles, diagnostic: DiagnosticEntity, startRow: Int): Int {
        var row = startRow
        sheet.createRow(row).createCell(2).apply {
            setCellValue("Scorecard v2.6 NB")
            setCellStyle(styles.titleStyle)
        }
        row++
        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale("es", "MX")).format(Date(diagnostic.fechaEnvio ?: diagnostic.fechaCreacion))
        val infoLines = listOf(
            "Proyecto: ${diagnostic.projectName}",
            "Ubicación: ${diagnostic.ubicacion}",
            "Revisión: ${diagnostic.revision}",
            "Fecha: $dateStr",
            "Responsable: ${diagnostic.responsable}"
        )
        infoLines.forEach { line ->
            sheet.createRow(row).createCell(2).apply {
                setCellValue(line)
                setCellStyle(styles.infoStyle)
            }
            row++
        }
        return row + 1
    }

    private fun writeLegend(sheet: XSSFSheet, styles: Styles, startRow: Int): Int {
        sheet.createRow(startRow).createCell(0).apply {
            setCellValue("Leyenda: AP (Aprobado)   P (Pendiente)   NC (No cumple)   0 (No aplica)")
            setCellStyle(styles.infoStyle)
        }
        return startRow + 2
    }

    private fun writeTableHeader(sheet: XSSFSheet, styles: Styles, startRow: Int): Int {
        val headerRow = sheet.createRow(startRow)
        val headers = listOf("No.", "AP", "P", "NC", "Crédito", "Concepto", "Comentarios/Observaciones")
        headers.forEachIndexed { index, text ->
            headerRow.createCell(index).apply {
                setCellValue(text)
                setCellStyle(styles.headerStyle)
            }
        }
        return startRow + 1
    }

    private fun writeQuestionRows(
        sheet: XSSFSheet,
        styles: Styles,
        rows: List<ReviewRow>,
        sections: List<SectionEntity>,
        startRow: Int
    ) {
        var rowIndex = startRow
        val sectionNameById = sections.associate { it.id to it.nombre }
        val bySection = rows.groupBy { it.question.seccionId }.toSortedMap(compareBy { it.toIntOrNull() ?: 0 })

        bySection.forEach { (seccionId, sectionRows) ->
            val sectionNombre = sectionNameById[seccionId] ?: seccionId
            val headerRow = sheet.createRow(rowIndex)
            headerRow.createCell(0).apply { setCellValue("$seccionId.00"); setCellStyle(styles.sectionStyle) }
            headerRow.createCell(1).apply { setCellValue(sectionNombre); setCellStyle(styles.sectionStyle) }
            for (col in 2..6) {
                headerRow.createCell(col).setCellStyle(styles.sectionStyle)
            }
            sheet.addMergedRegion(CellRangeAddress(rowIndex, rowIndex, 1, 6))
            rowIndex++

            sectionRows.sortedBy { it.question.codigo }.forEach { row ->
                val dataRow = sheet.createRow(rowIndex)
                dataRow.createCell(0).apply { setCellValue(row.question.codigo); setCellStyle(styles.infoStyle) }

                val valor = row.answer?.valor
                dataRow.createCell(1).apply {
                    if (valor == AnswerValue.APROBADO) setCellValue(1.0)
                    setCellStyle(if (valor == AnswerValue.APROBADO) styles.apStyle else styles.emptyStyle)
                }
                dataRow.createCell(2).apply {
                    if (valor == AnswerValue.PENDIENTE) setCellValue(1.0)
                    setCellStyle(if (valor == AnswerValue.PENDIENTE) styles.pStyle else styles.emptyStyle)
                }
                dataRow.createCell(3).apply {
                    if (valor == AnswerValue.NO_CUMPLE) setCellValue(1.0)
                    setCellStyle(if (valor == AnswerValue.NO_CUMPLE) styles.ncStyle else styles.emptyStyle)
                }
                dataRow.createCell(4).apply {
                    setCellValue(if (row.question.credito == Credito.REQUIRED) "Required" else "Plus")
                    setCellStyle(if (row.question.credito == Credito.REQUIRED) styles.requiredStyle else styles.plusStyle)
                }
                dataRow.createCell(5).apply { setCellValue(row.question.concepto); setCellStyle(styles.infoStyle) }
                dataRow.createCell(6).apply {
                    setCellValue(row.answer?.comentario.orEmpty())
                    setCellStyle(styles.infoStyle)
                }
                rowIndex++
            }
        }
    }

    private fun rgb(r: Int, g: Int, b: Int) = XSSFColor(byteArrayOf(r.toByte(), g.toByte(), b.toByte()), null)

    private class Styles(workbook: XSSFWorkbook) {
        val titleStyle: CellStyle = workbook.createCellStyle().apply {
            setFont(workbook.createFont().apply { bold = true; fontHeightInPoints = 14 })
        }
        val infoStyle: CellStyle = workbook.createCellStyle().apply {
            wrapText = true
        }
        val headerStyle: CellStyle = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            setFont(workbook.createFont().apply { bold = true })
            alignment = HorizontalAlignment.CENTER
        }
        val sectionStyle: CellStyle = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.GREY_40_PERCENT.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            setFont(workbook.createFont().apply { bold = true; color = IndexedColors.WHITE.index })
        }
        val requiredStyle: XSSFCellStyle = (workbook.createCellStyle() as XSSFCellStyle).apply {
            setFillForegroundColor(rgb(0x0B, 0x25, 0x45))
            fillPattern = FillPatternType.SOLID_FOREGROUND
            setFont(workbook.createFont().apply { bold = true; color = IndexedColors.WHITE.index } as XSSFFont)
            alignment = HorizontalAlignment.CENTER
        }
        val plusStyle: XSSFCellStyle = (workbook.createCellStyle() as XSSFCellStyle).apply {
            setFillForegroundColor(rgb(0xEC, 0x1E, 0x79))
            fillPattern = FillPatternType.SOLID_FOREGROUND
            setFont(workbook.createFont().apply { bold = true; color = IndexedColors.WHITE.index } as XSSFFont)
            alignment = HorizontalAlignment.CENTER
        }
        val apStyle: XSSFCellStyle = (workbook.createCellStyle() as XSSFCellStyle).apply {
            setFillForegroundColor(rgb(0x1E, 0x8E, 0x3E))
            fillPattern = FillPatternType.SOLID_FOREGROUND
            alignment = HorizontalAlignment.CENTER
        }
        val pStyle: XSSFCellStyle = (workbook.createCellStyle() as XSSFCellStyle).apply {
            setFillForegroundColor(rgb(0xF2, 0x99, 0x00))
            fillPattern = FillPatternType.SOLID_FOREGROUND
            alignment = HorizontalAlignment.CENTER
        }
        val ncStyle: XSSFCellStyle = (workbook.createCellStyle() as XSSFCellStyle).apply {
            setFillForegroundColor(rgb(0xD9, 0x30, 0x25))
            fillPattern = FillPatternType.SOLID_FOREGROUND
            alignment = HorizontalAlignment.CENTER
        }
        val emptyStyle: CellStyle = workbook.createCellStyle().apply {
            alignment = HorizontalAlignment.CENTER
        }
    }
}
