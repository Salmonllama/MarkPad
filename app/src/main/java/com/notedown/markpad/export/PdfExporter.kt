package com.notedown.markpad.export

import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.BackgroundColorSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import com.notedown.markpad.model.Note
import com.notedown.markpad.ui.markdown.InlineMarkdownParser
import com.notedown.markpad.ui.markdown.InlineSpan
import com.notedown.markpad.ui.markdown.MarkdownNode
import com.notedown.markpad.ui.markdown.MarkdownParser
import java.io.OutputStream

class PdfExporter(
    private val documentWriter: (Note, OutputStream) -> Unit = ::defaultPdfDocumentWriter
) : NoteExporter {

    override fun export(note: Note, outputStream: OutputStream) {
        documentWriter(note, outputStream)
    }

    companion object {
        const val PAGE_WIDTH = 595 // A4 standard width (points)
        const val PAGE_HEIGHT = 842 // A4 standard height (points)
        const val MARGIN_HORIZONTAL = 54f
        const val MARGIN_TOP = 54f
        const val MARGIN_BOTTOM = 54f

        fun defaultPdfDocumentWriter(note: Note, outputStream: OutputStream) {
            val document = PdfDocument()
            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas

            val textPaint = TextPaint().apply {
                isAntiAlias = true
                color = Color.rgb(33, 33, 33)
                textSize = 10.5f
            }

            val titlePaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(20, 20, 20)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val tableHeaderPaint = TextPaint(textPaint).apply {
                isAntiAlias = true
                color = Color.rgb(20, 20, 20)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val codePaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(40, 40, 40)
                textSize = 9.5f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            }

            val bulletPaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(33, 33, 33)
                textSize = 10.5f
            }

            val bgPaint = Paint().apply {
                style = Paint.Style.FILL
            }

            val linePaint = Paint().apply {
                color = Color.rgb(200, 200, 200)
                strokeWidth = 1f
            }

            val contentWidth = PAGE_WIDTH - (MARGIN_HORIZONTAL * 2)
            val maxY = PAGE_HEIGHT - MARGIN_BOTTOM
            var currentY = MARGIN_TOP

            fun checkPageBreak(requiredHeight: Float) {
                if (currentY + requiredHeight > maxY) {
                    document.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    currentY = MARGIN_TOP
                }
            }

            fun buildSpannable(spans: List<InlineSpan>): SpannableStringBuilder {
                val ssb = SpannableStringBuilder()
                for (span in spans) {
                    val start = ssb.length
                    ssb.append(span.text)
                    val end = ssb.length
                    when (span) {
                        is InlineSpan.Bold -> {
                            ssb.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        is InlineSpan.Italic -> {
                            ssb.setSpan(StyleSpan(Typeface.ITALIC), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        is InlineSpan.BoldItalic -> {
                            ssb.setSpan(StyleSpan(Typeface.BOLD_ITALIC), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        is InlineSpan.InlineCode -> {
                            ssb.setSpan(TypefaceSpan("monospace"), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            ssb.setSpan(BackgroundColorSpan(Color.rgb(240, 240, 240)), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        is InlineSpan.Normal -> {}
                    }
                }
                return ssb
            }

            fun createLayout(text: CharSequence, paint: TextPaint, width: Int): StaticLayout {
                return StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1.25f)
                    .setIncludePad(false)
                    .build()
            }

            fun drawLayout(layout: StaticLayout, x: Float, width: Float, spacingAfter: Float) {
                if (currentY + layout.height <= maxY) {
                    canvas.save()
                    canvas.translate(x, currentY)
                    layout.draw(canvas)
                    canvas.restore()
                    currentY += layout.height + spacingAfter
                } else if (currentY > MARGIN_TOP && layout.height <= (maxY - MARGIN_TOP)) {
                    checkPageBreak(layout.height.toFloat())
                    canvas.save()
                    canvas.translate(x, currentY)
                    layout.draw(canvas)
                    canvas.restore()
                    currentY += layout.height + spacingAfter
                } else {
                    for (i in 0 until layout.lineCount) {
                        val lineTop = layout.getLineTop(i)
                        val lineBottom = layout.getLineBottom(i)
                        val lineHeight = (lineBottom - lineTop).toFloat()
                        checkPageBreak(lineHeight)

                        canvas.save()
                        canvas.clipRect(x, currentY, x + width, currentY + lineHeight)
                        canvas.translate(x, currentY - lineTop)
                        layout.draw(canvas)
                        canvas.restore()

                        currentY += lineHeight
                    }
                    currentY += spacingAfter
                }
            }

            val nodes = MarkdownParser.parse(note.content)

            for (node in nodes) {
                when (node) {
                    is MarkdownNode.Header -> {
                        val (size, spaceBefore, spaceAfter) = when (node.level) {
                            1 -> Triple(20f, 20f, 10f)
                            2 -> Triple(15f, 16f, 8f)
                            else -> Triple(12.5f, 12f, 6f)
                        }
                        titlePaint.textSize = size
                        val cleanHeaderText = InlineMarkdownParser.toPlainText(node.text)
                        val lines = wrapText(cleanHeaderText, titlePaint, contentWidth)
                        val totalHeight = spaceBefore + (lines.size * (size + 4f)) + spaceAfter
                        checkPageBreak(totalHeight)

                        currentY += spaceBefore
                        for (line in lines) {
                            canvas.drawText(line, MARGIN_HORIZONTAL, currentY, titlePaint)
                            currentY += size + 4f
                        }
                        currentY += spaceAfter
                    }
                    is MarkdownNode.Paragraph -> {
                        val spans = InlineMarkdownParser.parse(node.text)
                        val ssb = buildSpannable(spans)
                        val layout = createLayout(ssb, textPaint, contentWidth.toInt())
                        drawLayout(layout, MARGIN_HORIZONTAL, contentWidth, spacingAfter = 6f)
                    }
                    is MarkdownNode.BulletItem -> {
                        val spans = InlineMarkdownParser.parse(node.text)
                        val ssb = buildSpannable(spans)
                        val bulletIndent = 16f
                        val bulletWidth = (contentWidth - bulletIndent).toInt()
                        val layout = createLayout(ssb, textPaint, bulletWidth)

                        val firstLineHeight = if (layout.lineCount > 0) {
                            (layout.getLineBottom(0) - layout.getLineTop(0)).toFloat()
                        } else 15f
                        checkPageBreak(firstLineHeight)

                        val baseline = if (layout.lineCount > 0) layout.getLineBaseline(0).toFloat() else 12f
                        canvas.drawText("•", MARGIN_HORIZONTAL, currentY + baseline, bulletPaint)

                        drawLayout(layout, MARGIN_HORIZONTAL + bulletIndent, bulletWidth.toFloat(), spacingAfter = 4f)
                    }
                    is MarkdownNode.CodeBlock -> {
                        val codeLines = node.code.lines()
                        val blockHeight = (codeLines.size * 13f) + 16f
                        checkPageBreak(blockHeight)

                        bgPaint.color = Color.rgb(244, 244, 244)
                        val rect = RectF(
                            MARGIN_HORIZONTAL,
                            currentY - 4f,
                            MARGIN_HORIZONTAL + contentWidth,
                            currentY + blockHeight - 12f
                        )
                        canvas.drawRoundRect(rect, 4f, 4f, bgPaint)

                        var codeY = currentY + 10f
                        for (codeLine in codeLines) {
                            canvas.drawText(codeLine, MARGIN_HORIZONTAL + 8f, codeY, codePaint)
                            codeY += 13f
                        }
                        currentY += blockHeight + 4f
                    }
                    is MarkdownNode.Blockquote -> {
                        val spans = InlineMarkdownParser.parse(node.text)
                        val ssb = buildSpannable(spans)
                        val quotePaint = TextPaint(textPaint).apply {
                            color = Color.rgb(85, 85, 85)
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                        }
                        val quoteIndent = 14f
                        val quoteWidth = (contentWidth - quoteIndent).toInt()
                        val layout = createLayout(ssb, quotePaint, quoteWidth)

                        val startY = currentY
                        drawLayout(layout, MARGIN_HORIZONTAL + quoteIndent, quoteWidth.toFloat(), spacingAfter = 8f)
                        val blockHeight = currentY - startY - 8f

                        // Draw left accent bar
                        bgPaint.color = Color.rgb(46, 116, 181)
                        canvas.drawRect(
                            MARGIN_HORIZONTAL,
                            startY,
                            MARGIN_HORIZONTAL + 3f,
                            startY + blockHeight,
                            bgPaint
                        )
                    }
                    is MarkdownNode.HorizontalRule -> {
                        checkPageBreak(16f)
                        currentY += 8f
                        canvas.drawLine(
                            MARGIN_HORIZONTAL,
                            currentY,
                            MARGIN_HORIZONTAL + contentWidth,
                            currentY,
                            linePaint
                        )
                        currentY += 12f
                    }
                    is MarkdownNode.Table -> {
                        if (node.headers.isNotEmpty()) {
                            val colCount = node.headers.size
                            val charCounts = node.headers.indices.map { colIdx ->
                                val headerLen = node.headers[colIdx].length
                                val maxRowLen = node.rows.maxOfOrNull { it.cells.getOrElse(colIdx) { "" }.length } ?: 0
                                maxOf(headerLen, maxRowLen).coerceAtLeast(3)
                            }
                            val totalChars = charCounts.sum().toFloat()
                            val minColWidth = 40f
                            val colWidths = FloatArray(colCount)
                            if (totalChars > 0 && contentWidth / colCount >= minColWidth) {
                                for (c in 0 until colCount) {
                                    colWidths[c] = (charCounts[c] / totalChars) * contentWidth
                                }
                                if (colWidths.any { it < minColWidth }) {
                                    for (c in 0 until colCount) {
                                        colWidths[c] = contentWidth / colCount
                                    }
                                }
                            } else {
                                for (c in 0 until colCount) {
                                    colWidths[c] = contentWidth / colCount
                                }
                            }

                            val colX = FloatArray(colCount)
                            var accX = MARGIN_HORIZONTAL
                            for (c in 0 until colCount) {
                                colX[c] = accX
                                accX += colWidths[c]
                            }

                            val cellPadH = 6f
                            val cellPadV = 5f

                            fun createCellLayout(
                                text: String,
                                isHeader: Boolean,
                                colIdx: Int,
                                availWidth: Float
                            ): StaticLayout {
                                val align = when (node.alignments.getOrElse(colIdx) { com.notedown.markpad.ui.markdown.TableAlignment.LEFT }) {
                                    com.notedown.markpad.ui.markdown.TableAlignment.LEFT -> Layout.Alignment.ALIGN_NORMAL
                                    com.notedown.markpad.ui.markdown.TableAlignment.CENTER -> Layout.Alignment.ALIGN_CENTER
                                    com.notedown.markpad.ui.markdown.TableAlignment.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
                                }
                                val spans = InlineMarkdownParser.parse(text)
                                val ssb = buildSpannable(spans)
                                if (isHeader) {
                                    ssb.setSpan(StyleSpan(Typeface.BOLD), 0, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                                }
                                val paint = if (isHeader) tableHeaderPaint else textPaint
                                return StaticLayout.Builder.obtain(ssb, 0, ssb.length, paint, maxOf(1, availWidth.toInt()))
                                    .setAlignment(align)
                                    .setLineSpacing(0f, 1.15f)
                                    .setIncludePad(false)
                                    .build()
                            }

                            fun drawTableRow(
                                cells: List<String>,
                                isHeader: Boolean,
                                isAlternate: Boolean
                            ) {
                                val layouts = node.headers.indices.map { c ->
                                    val cellText = cells.getOrElse(c) { "" }
                                    val availW = colWidths[c] - (cellPadH * 2)
                                    createCellLayout(cellText, isHeader, c, availW)
                                }

                                val maxCellH = layouts.maxOfOrNull { it.height.toFloat() } ?: 12f
                                val rowHeight = maxCellH + (cellPadV * 2)

                                checkPageBreak(rowHeight)

                                val rowTop = currentY
                                val rowBottom = currentY + rowHeight

                                if (isHeader) {
                                    bgPaint.color = Color.rgb(240, 242, 247)
                                    canvas.drawRect(MARGIN_HORIZONTAL, rowTop, MARGIN_HORIZONTAL + contentWidth, rowBottom, bgPaint)
                                } else if (isAlternate) {
                                    bgPaint.color = Color.rgb(249, 250, 252)
                                    canvas.drawRect(MARGIN_HORIZONTAL, rowTop, MARGIN_HORIZONTAL + contentWidth, rowBottom, bgPaint)
                                }

                                for (c in 0 until colCount) {
                                    canvas.save()
                                    canvas.translate(colX[c] + cellPadH, rowTop + cellPadV)
                                    layouts[c].draw(canvas)
                                    canvas.restore()
                                }

                                canvas.drawLine(MARGIN_HORIZONTAL, rowTop, MARGIN_HORIZONTAL + contentWidth, rowTop, linePaint)
                                canvas.drawLine(MARGIN_HORIZONTAL, rowBottom, MARGIN_HORIZONTAL + contentWidth, rowBottom, linePaint)
                                canvas.drawLine(MARGIN_HORIZONTAL, rowTop, MARGIN_HORIZONTAL, rowBottom, linePaint)
                                canvas.drawLine(MARGIN_HORIZONTAL + contentWidth, rowTop, MARGIN_HORIZONTAL + contentWidth, rowBottom, linePaint)
                                for (c in 1 until colCount) {
                                    canvas.drawLine(colX[c], rowTop, colX[c], rowBottom, linePaint)
                                }

                                currentY += rowHeight
                            }

                            drawTableRow(node.headers, isHeader = true, isAlternate = false)
                            node.rows.forEachIndexed { rIdx, row ->
                                drawTableRow(row.cells, isHeader = false, isAlternate = rIdx % 2 == 1)
                            }
                            currentY += 10f
                        }
                    }
                }
            }

            document.finishPage(page)
            document.writeTo(outputStream)
            document.close()
        }

        private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
            val words = text.split(" ")
            val lines = mutableListOf<String>()
            var currentLine = StringBuilder()

            for (word in words) {
                val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (paint.measureText(candidate) <= maxWidth) {
                    currentLine = StringBuilder(candidate)
                } else {
                    if (currentLine.isNotEmpty()) {
                        lines.add(currentLine.toString())
                    }
                    currentLine = StringBuilder(word)
                }
            }
            if (currentLine.isNotEmpty()) {
                lines.add(currentLine.toString())
            }
            return lines.ifEmpty { listOf("") }
        }
    }
}
