package com.example.app_01

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.TypedValue
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ThreeDgsChatPdfExport {

    data class PdfExportResult(
        val scriptFile: File,
        val pdfFile: File,
        val extractedPieceCount: Int,
    )

    // A4: 595 x 842 points
    private const val PAGE_WIDTH = 595f
    private const val PAGE_HEIGHT = 842f
    private const val MARGIN_LEFT = 40f
    private const val MARGIN_RIGHT = 40f
    private const val MARGIN_TOP = 50f
    private const val MARGIN_BOTTOM = 50f

    private val usableWidth = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT

    private val headingSizes = intArrayOf(22, 18, 16, 14)
    private val bodyTextSize = 11f
    private val tableTextSize = 9f
    private val chartLabelSize = 8f

    // ── colour palette ──────────────────────────────────────────────────
    private val colorPrimary = Color.parseColor("#1B4F8A")
    private val colorText = Color.parseColor("#1A1A1A")
    private val colorGrid = Color.parseColor("#CCCCCC")
    private val colorHeaderBg = Color.parseColor("#E8F0FE")
    private val colorTableAlt = Color.parseColor("#F5F8FC")
    private val chartColors = intArrayOf(
        Color.parseColor("#1B4F8A"),
        Color.parseColor("#2E7D32"),
        Color.parseColor("#C62828"),
        Color.parseColor("#EF6C00"),
        Color.parseColor("#6A1B9A"),
        Color.parseColor("#00838F"),
        Color.parseColor("#4527A0"),
        Color.parseColor("#AD1457"),
    )

    private val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorText
        textSize = bodyTextSize
        typeface = Typeface.DEFAULT
    }

    // ── public entry ────────────────────────────────────────────────────

    fun tryExportToPdf(
        context: Context,
        fullMarkdown: String,
        subdirectory: String = "3dgs_llm_exports",
        fileBasePrefix: String = "3dgs_report",
        docTitle: String = "3DGS 분석 보고서",
    ): PdfExportResult? {
        val py = ThreeDgsChatDocxExport.extractFirstPythonFence(fullMarkdown) ?: return null
        return try {
            val outDir = File(context.getExternalFilesDir(null), subdirectory).apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val base = "${fileBasePrefix}_$stamp"
            val pyFile = File(outDir, "$base.py")
            val pdfFile = File(outDir, "$base.pdf")
            pyFile.writeText(py, Charsets.UTF_8)
            val pieces = ThreeDgsChatDocxExport.extractPiecesFromPythonDocxScript(py)
            renderPdf(pieces, pdfFile, docTitle)
            PdfExportResult(scriptFile = pyFile, pdfFile = pdfFile, extractedPieceCount = pieces.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ── PDF rendering engine ────────────────────────────────────────────

    private fun renderPdf(
        pieces: List<ThreeDgsChatDocxExport.DocPiece>,
        outputFile: File,
        title: String,
    ) {
        val document = PdfDocument()
        var page = newPage(document)
        var canvas: Canvas = page.canvas
        var y = MARGIN_TOP

        fun newPageIfNeeded(needed: Float) {
            if (y + needed > PAGE_HEIGHT - MARGIN_BOTTOM) {
                document.finishPage(page)
                page = newPage(document)
                canvas = page.canvas
                y = MARGIN_TOP
            }
        }

        // title page header
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorPrimary
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
        }
        val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.GRAY
            textSize = 10f
            typeface = Typeface.DEFAULT
        }
        val stampText = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREAN).format(Date())
        val underlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorPrimary
            strokeWidth = 2f
        }

        var isFirstHeading = true
        var chartSeq = 0

        for (piece in pieces) {
            when (piece) {
                is ThreeDgsChatDocxExport.DocPiece.Heading -> {
                    val level = piece.level.coerceIn(0, headingSizes.lastIndex)
                    val size = headingSizes[level].toFloat()
                    val headingPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = if (level <= 1) colorPrimary else colorText
                        textSize = size
                        typeface = if (level <= 1) Typeface.DEFAULT_BOLD else Typeface.DEFAULT_BOLD
                    }
                    newPageIfNeeded(60f)
                    if (isFirstHeading) {
                        // 표지 타이틀
                        canvas.drawText(piece.text, MARGIN_LEFT, y + size, headingPaint)
                        y += size + 8f
                        canvas.drawText("생성: $stampText", MARGIN_LEFT, y + 12f, subtitlePaint)
                        y += 20f
                        canvas.drawLine(MARGIN_LEFT, y, PAGE_WIDTH - MARGIN_RIGHT, y, underlinePaint)
                        y += 4f
                        // cover page subtitle
                        canvas.drawText(
                            title,
                            MARGIN_LEFT,
                            y + 18f,
                            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = Color.GRAY; textSize = 12f
                            },
                        )
                        y += 28f
                        isFirstHeading = false
                    } else {
                        newPageIfNeeded(60f)
                        canvas.drawLine(MARGIN_LEFT, y - 4f, PAGE_WIDTH - MARGIN_RIGHT, y - 4f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = colorGrid; strokeWidth = 0.7f
                        })
                        canvas.drawText(piece.text, MARGIN_LEFT, y + size + 2f, headingPaint)
                        y += size + 16f
                    }
                }

                is ThreeDgsChatDocxExport.DocPiece.Paragraph -> {
                    val para = piece.text.trim()
                    if (para.isEmpty()) continue
                    val paraTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = colorText; textSize = bodyTextSize; typeface = Typeface.DEFAULT
                    }
                    val layout = StaticLayout.Builder
                        .obtain(para, 0, para.length, paraTextPaint, usableWidth.toInt())
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(4f, 1f)
                        .build()
                    newPageIfNeeded(layout.height.toFloat() + 8f)
                    canvas.save()
                    canvas.translate(MARGIN_LEFT, y)
                    layout.draw(canvas)
                    canvas.restore()
                    y += layout.height + 8f
                }

                is ThreeDgsChatDocxExport.DocPiece.Table -> {
                    if (piece.rows.isEmpty()) continue
                    if (looksLikeChartTable(piece.rows)) {
                        // 차트로 렌더링
                        newPageIfNeeded(280f)
                        chartSeq++
                        val needed = renderBarChart(canvas, piece.rows, chartSeq, y)
                        y += needed + 12f
                    } else {
                        val tableHeight = measureTableHeight(piece.rows)
                        newPageIfNeeded(tableHeight + 12f)
                        y = drawTable(canvas, piece.rows, y)
                    }
                }

                is ThreeDgsChatDocxExport.DocPiece.PageBreak -> {
                    document.finishPage(page)
                    page = newPage(document)
                    canvas = page.canvas
                    y = MARGIN_TOP
                }
            }
        }

        document.finishPage(page)
        FileOutputStream(outputFile).use { fos -> document.writeTo(fos) }
        document.close()
    }

    // ── page factory ────────────────────────────────────────────────────

    private fun newPage(document: PdfDocument): PdfDocument.Page {
        val pageInfo = PdfDocument.PageInfo.Builder(
            PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), document.pages.size + 1
        ).create()
        val page = document.startPage(pageInfo)
        // page number footer
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.GRAY; textSize = 8f
        }
        page.canvas.drawText(
            "- ${document.pages.size + 1} -",
            PAGE_WIDTH / 2f - 14f,
            PAGE_HEIGHT - 20f,
            footerPaint,
        )
        return page
    }

    // ── table rendering ─────────────────────────────────────────────────

    private fun measureTableHeight(rows: List<List<String>>): Float {
        val cols = rows.maxOf { it.size }
        val colWidth = usableWidth / cols
        val cellPad = 6f
        val lineHeight = tableTextSize + 4f

        val measurePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = tableTextSize; typeface = Typeface.DEFAULT
        }

        return rows.sumOf { row ->
            val maxLines = row.maxOf { cell ->
                wrapText(cell, (colWidth - cellPad * 2).toInt(), measurePaint).size.coerceAtLeast(1)
            }
            (maxLines * lineHeight + cellPad * 2).toDouble()
        }.toFloat() + 4f
    }

    private fun drawTable(canvas: Canvas, rows: List<List<String>>, startY: Float): Float {
        val cols = rows.maxOf { it.size }
        val colWidth = usableWidth / cols
        val cellPad = 6f
        val lineHeight = tableTextSize + 4f

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 0.8f; color = colorGrid
        }
        val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL; color = colorHeaderBg
        }
        val altBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL; color = colorTableAlt
        }
        val cellPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorText; textSize = tableTextSize; typeface = Typeface.DEFAULT
        }
        val headerTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorPrimary; textSize = tableTextSize
            typeface = Typeface.DEFAULT_BOLD
        }

        var y = startY
        for ((rIdx, row) in rows.withIndex()) {
            val rowLines = row.maxOf { cell ->
                val textPaint = if (rIdx == 0) headerTextPaint else cellPaint
                wrapText(cell, (colWidth - cellPad * 2).toInt(), textPaint).size.coerceAtLeast(1)
            }
            val rowHeight = rowLines * lineHeight + cellPad * 2

            if (rIdx == 0) {
                canvas.drawRect(MARGIN_LEFT, y, PAGE_WIDTH - MARGIN_RIGHT, y + rowHeight, headerBgPaint)
            } else if (rIdx % 2 == 0) {
                canvas.drawRect(MARGIN_LEFT, y, PAGE_WIDTH - MARGIN_RIGHT, y + rowHeight, altBgPaint)
            }

            for (cIdx in 0 until cols) {
                val x = MARGIN_LEFT + cIdx * colWidth
                canvas.drawRect(x, y, x + colWidth, y + rowHeight, borderPaint)
                val text = row.getOrElse(cIdx) { "" }
                val textPaint = if (rIdx == 0) headerTextPaint else cellPaint

                canvas.save()
                canvas.clipRect(x + 1f, y + 1f, x + colWidth - 1f, y + rowHeight - 1f)

                val wrapped = wrapText(text, (colWidth - cellPad * 2).toInt(), textPaint)
                var cy = y + cellPad + tableTextSize
                for (line in wrapped) {
                    canvas.drawText(line, x + cellPad, cy, textPaint)
                    cy += lineHeight
                }

                canvas.restore()
            }
            y += rowHeight
        }

        val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 1.5f; color = colorGrid
        }
        canvas.drawRect(MARGIN_LEFT, startY, PAGE_WIDTH - MARGIN_RIGHT, y, outerPaint)
        return y + 8f
    }

    private fun wrapText(text: String, maxWidthPx: Int, paint: TextPaint): List<String> {
        if (text.isEmpty()) return listOf("")
        val lines = mutableListOf<String>()
        var remaining = text
        while (remaining.isNotEmpty()) {
            val count = paint.breakText(remaining, true, maxWidthPx.toFloat(), null)
            val safeCount = if (count <= 0) 1 else count
            lines.add(remaining.substring(0, safeCount.coerceAtMost(remaining.length)))
            remaining = remaining.substring(safeCount.coerceAtMost(remaining.length))
        }
        return lines.ifEmpty { listOf("") }
    }

    // ── chart detection & rendering ─────────────────────────────────────

    /**
     * 첫 행(헤더)에 "차트:" 접두사가 포함된 표는 차트로 간주.
     * 형식:  [차트:bar:타이틀] [값컬럼1] [값컬럼2] …
     * 본문:  [라벨]         [숫자1]    [숫자2]  …
     */
    private fun looksLikeChartTable(rows: List<List<String>>): Boolean {
        if (rows.size < 2) return false
        val firstCell = rows.firstOrNull()?.firstOrNull().orEmpty()
        return firstCell.startsWith("차트:") || firstCell.startsWith("chart:", ignoreCase = true)
    }

    private fun renderBarChart(
        canvas: Canvas,
        rows: List<List<String>>,
        chartIndex: Int,
        startY: Float,
    ): Float {
        val header = rows.first()
        val dataRows = rows.drop(1)
        if (dataRows.isEmpty()) return 0f

        val chartType = when {
            header.first().contains("bar", ignoreCase = true) -> "bar"
            header.first().contains("line", ignoreCase = true) -> "line"
            header.first().contains("pie", ignoreCase = true) -> "pie"
            else -> "bar"
        }
        val chartTitle = header.first().replace(Regex("""차트:\w*:?"""), "").trim().ifEmpty { "차트 $chartIndex" }
        val seriesLabels = header.drop(1)
        val labels = dataRows.map { it.firstOrNull().orEmpty() }
        val seriesCount = seriesLabels.size.coerceIn(1, dataRows.firstOrNull()?.size?.minus(1) ?: 1)

        // title
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorPrimary; textSize = 14f; typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText(chartTitle, PAGE_WIDTH / 2f - titlePaint.measureText(chartTitle) / 2f, startY + 14f, titlePaint)
        var cy = startY + 32f

        val chartLeft = MARGIN_LEFT + 50f
        val chartRight = PAGE_WIDTH - MARGIN_RIGHT - 20f
        val chartWidth = chartRight - chartLeft
        val chartTop = cy
        val chartBottom = startY + 240f
        val chartHeight = chartBottom - chartTop

        val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY; strokeWidth = 1.2f
        }
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorGrid; strokeWidth = 0.4f; pathEffect = DashPathEffect(floatArrayOf(4f, 3f), 0f)
        }
        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY; textSize = chartLabelSize; typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
        }
        val valuePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorText; textSize = 7f; typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
        }

        if (chartType == "bar") {
            // y-axis
            canvas.drawLine(chartLeft, chartTop, chartLeft, chartBottom, axisPaint)
            canvas.drawLine(chartLeft, chartBottom, chartRight, chartBottom, axisPaint)

            val maxVal = dataRows.flatMap { row ->
                row.drop(1).map { it.toFloatOrNull() ?: 0f }
            }.maxOrNull() ?: 1f
            val valStep = computeNiceStep(maxVal)

            // grid lines
            var gv = 0f
            while (gv <= maxVal + valStep) {
                val gy = chartBottom - (gv / (maxVal + valStep * 0.5f) * chartHeight).coerceIn(0f, chartHeight)
                if (gv > 0f) canvas.drawLine(chartLeft, gy, chartRight, gy, gridPaint)
                canvas.drawText("%.0f".format(gv), chartLeft - 6f, gy + 3f, labelPaint)
                gv += valStep
            }

            val groupWidth = chartWidth / labels.size
            val barGroupWidth = groupWidth * 0.7f
            val barWidth = barGroupWidth / seriesCount

            for ((li, label) in labels.withIndex()) {
                val gx = chartLeft + li * groupWidth + groupWidth * 0.15f
                for (si in 0 until seriesCount) {
                    val valStr = dataRows.getOrNull(li)?.getOrNull(si + 1).orEmpty()
                    val v = valStr.toFloatOrNull() ?: 0f
                    val barH = (v / (maxVal + valStep * 0.3f) * chartHeight).coerceAtLeast(0f)
                    val barX = gx + si * barWidth
                    val barY = chartBottom - barH
                    val color = chartColors[si % chartColors.size]
                    canvas.drawRect(barX, barY, barX + barWidth - 1f, chartBottom, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.FILL; this.color = color
                    })
                    canvas.drawText(valStr, barX + barWidth / 2f, barY - 3f, valuePaint)
                }
                canvas.drawText(label, gx + barGroupWidth / 2f, chartBottom + 12f, labelPaint)
            }
        } else if (chartType == "pie") {
            val centerX = (chartLeft + chartRight) / 2f
            val centerY = (chartTop + chartBottom) / 2f
            val radius = minOf(chartWidth, chartHeight) / 2f - 20f

            val values = dataRows.map { row ->
                row.getOrElse(1) { "0" }.toFloatOrNull() ?: 0f
            }
            val total = values.sum().coerceAtLeast(1f)

            val piePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            var angle = -90f
            for ((i, v) in values.withIndex()) {
                val sweep = v / total * 360f
                piePaint.color = chartColors[i % chartColors.size]
                piePaint.style = Paint.Style.FILL
                canvas.drawArc(chartLeft + 20f, chartTop + 20f, chartLeft + 20f + radius * 2, chartTop + 20f + radius * 2, angle, sweep, true, piePaint)
                angle += sweep
            }

            // legend
            var lx = chartLeft + radius * 2 + 40f
            var ly = chartTop + 20f
            val legendPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val legendText = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colorText; textSize = chartLabelSize; typeface = Typeface.DEFAULT
            }
            for ((i, label) in labels.withIndex()) {
                legendPaint.color = chartColors[i % chartColors.size]
                legendPaint.style = Paint.Style.FILL
                canvas.drawRect(lx, ly, lx + 10f, ly + 10f, legendPaint)
                val pct = "%.1f%%".format(values[i] / total * 100f)
                canvas.drawText("$label  $pct", lx + 14f, ly + 10f, legendText)
                ly += 16f
            }
            cy = chartBottom + 10f
        } else {
            // fallback: simple message
            val msgPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.GRAY; textSize = 10f; typeface = Typeface.DEFAULT
            }
            canvas.drawText("(이 차트 유형은 지원되지 않습니다. 표로 확인하세요.)", chartLeft, chartTop + 40f, msgPaint)
            cy = chartTop + 60f
        }

        return 260f // total chart area height
    }

    private fun computeNiceStep(maxVal: Float): Float {
        if (maxVal <= 0f) return 1f
        val magnitude = Math.pow(10.0, Math.floor(Math.log10(maxVal.toDouble()))).toFloat()
        val residual = maxVal / magnitude
        return when {
            residual <= 2f -> 0.2f * magnitude
            residual <= 5f -> 0.5f * magnitude
            else -> magnitude
        }
    }
}
