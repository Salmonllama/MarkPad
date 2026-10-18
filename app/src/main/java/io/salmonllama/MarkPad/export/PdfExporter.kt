package io.salmonllama.MarkPad.export

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
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.SubscriptSpan
import android.text.style.SuperscriptSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import io.salmonllama.MarkPad.model.Note
import io.salmonllama.MarkPad.ui.markdown.AlertType
import io.salmonllama.MarkPad.ui.markdown.InlineMarkdownParser
import io.salmonllama.MarkPad.ui.markdown.InlineSpan
import io.salmonllama.MarkPad.ui.markdown.MarkdownNode
import io.salmonllama.MarkPad.ui.markdown.MarkdownParser
import io.salmonllama.MarkPad.ui.markdown.TableAlignment
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

        /**
         * Pure-JVM inspection helper used by unit tests to verify that all GFM block and inline
         * constructs are mapped to their expected PDF layout representations without requiring
         * Android native Canvas/PdfDocument JNI bindings.
         */
        internal fun buildRenderOperationsSummary(note: Note): List<String> {
            val ops = mutableListOf<String>()
            fun summarizeNodes(nodes: List<MarkdownNode>) {
                for (node in nodes) {
                    when (node) {
                        is MarkdownNode.Header -> {
                            val plain = InlineMarkdownParser.toPlainText(node.text)
                            ops.add("H${node.level}:$plain")
                        }
                        is MarkdownNode.Paragraph -> {
                            val spans = InlineMarkdownParser.parse(node.text)
                            val spanTags = spans.joinToString(",") { span ->
                                when (span) {
                                    is InlineSpan.Bold -> "B(${span.text})"
                                    is InlineSpan.Italic -> "I(${span.text})"
                                    is InlineSpan.BoldItalic -> "BI(${span.text})"
                                    is InlineSpan.Strikethrough -> "S(${span.text})"
                                    is InlineSpan.Underline -> "U(${span.text})"
                                    is InlineSpan.Highlight -> "HL(${span.text})"
                                    is InlineSpan.InlineCode -> "CODE(${span.text})"
                                    is InlineSpan.KeyboardKey -> "KBD(${span.text})"
                                    is InlineSpan.Superscript -> "SUP(${span.text})"
                                    is InlineSpan.Subscript -> "SUB(${span.text})"
                                    is InlineSpan.FootnoteReference -> "FNREF(${span.id})"
                                    is InlineSpan.Link -> "LINK(${span.text}->${span.url})"
                                    is InlineSpan.Image -> "IMG(${span.text}->${span.url})"
                                    is InlineSpan.Normal -> "TXT(${span.text})"
                                }
                            }
                            ops.add("P:$spanTags")
                        }
                        is MarkdownNode.BulletItem -> {
                            ops.add("BULLET[indent=${node.indentLevel}]:${InlineMarkdownParser.toPlainText(node.text)}")
                        }
                        is MarkdownNode.OrderedItem -> {
                            ops.add("ORDERED[indent=${node.indentLevel},num=${node.number}]:${InlineMarkdownParser.toPlainText(node.text)}")
                        }
                        is MarkdownNode.TaskItem -> {
                            ops.add("TASK[indent=${node.indentLevel},checked=${node.isChecked}]:${InlineMarkdownParser.toPlainText(node.text)}")
                        }
                        is MarkdownNode.CodeBlock -> {
                            ops.add("CODEBLOCK[lang=${node.language}]:${node.code}")
                        }
                        is MarkdownNode.Blockquote -> {
                            ops.add("QUOTE[depth=${node.depth}]:${InlineMarkdownParser.toPlainText(node.text)}")
                        }
                        is MarkdownNode.AlertBlock -> {
                            ops.add("ALERT[type=${node.type.name}]:${InlineMarkdownParser.toPlainText(node.text)}")
                        }
                        is MarkdownNode.DetailsBlock -> {
                            ops.add("DETAILS[open=${node.isOpenByDefault}]:${InlineMarkdownParser.toPlainText(node.summary)}")
                            summarizeNodes(MarkdownParser.parse(node.body))
                        }
                        is MarkdownNode.FootnoteDefinition -> {
                            ops.add("FOOTNOTE[id=${node.id}]:${InlineMarkdownParser.toPlainText(node.text)}")
                        }
                        is MarkdownNode.HorizontalRule -> {
                            ops.add("HR")
                        }
                        is MarkdownNode.Table -> {
                            ops.add("TABLE[cols=${node.headers.size},rows=${node.rows.size}]")
                        }
                    }
                }
            }
            summarizeNodes(MarkdownParser.parse(note.content))
            return ops
        }

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

            val footnotePaint = TextPaint(textPaint).apply {
                textSize = 9f
                color = Color.rgb(87, 96, 106)
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

            val codeLangPaint = Paint(codePaint).apply {
                textSize = 8.5f
                color = Color.rgb(100, 100, 100)
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
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
                    val displayText = when (span) {
                        is InlineSpan.KeyboardKey -> "[${span.text}]"
                        is InlineSpan.Image -> "[Image: ${span.text.ifBlank { span.url }} (${span.url})]"
                        else -> span.text
                    }
                    ssb.append(displayText)
                    val end = ssb.length
                    if (end <= start) continue

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
                        is InlineSpan.Strikethrough -> {
                            ssb.setSpan(StrikethroughSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        is InlineSpan.Underline -> {
                            ssb.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        is InlineSpan.Highlight -> {
                            ssb.setSpan(BackgroundColorSpan(Color.rgb(255, 248, 197)), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        is InlineSpan.InlineCode -> {
                            ssb.setSpan(TypefaceSpan("monospace"), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            ssb.setSpan(BackgroundColorSpan(Color.rgb(240, 240, 240)), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        is InlineSpan.KeyboardKey -> {
                            ssb.setSpan(TypefaceSpan("monospace"), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            ssb.setSpan(BackgroundColorSpan(Color.rgb(235, 237, 240)), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        is InlineSpan.Superscript -> {
                            ssb.setSpan(SuperscriptSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            ssb.setSpan(RelativeSizeSpan(0.75f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        is InlineSpan.Subscript -> {
                            ssb.setSpan(SubscriptSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            ssb.setSpan(RelativeSizeSpan(0.75f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        is InlineSpan.FootnoteReference -> {
                            ssb.setSpan(SuperscriptSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            ssb.setSpan(RelativeSizeSpan(0.75f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            ssb.setSpan(ForegroundColorSpan(Color.rgb(9, 105, 218)), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        is InlineSpan.Link -> {
                            ssb.setSpan(URLSpan(span.url), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            ssb.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            ssb.setSpan(ForegroundColorSpan(Color.rgb(9, 105, 218)), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        is InlineSpan.Image -> {
                            ssb.setSpan(StyleSpan(Typeface.ITALIC), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            ssb.setSpan(ForegroundColorSpan(Color.rgb(87, 96, 106)), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        is InlineSpan.Normal -> {}
                    }
                }
                return ssb
            }

            fun createLayout(text: CharSequence, paint: TextPaint, width: Int): StaticLayout {
                return StaticLayout.Builder.obtain(text, 0, text.length, paint, maxOf(1, width))
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

            fun renderNodes(nodes: List<MarkdownNode>) {
                for (node in nodes) {
                    when (node) {
                        is MarkdownNode.Header -> {
                            val (size, spaceBefore, spaceAfter) = when (node.level) {
                                1 -> Triple(20f, 18f, 8f)
                                2 -> Triple(16f, 15f, 7f)
                                3 -> Triple(13.5f, 12f, 6f)
                                4 -> Triple(11.5f, 10f, 5f)
                                5 -> Triple(10.5f, 8f, 4f)
                                else -> Triple(10f, 8f, 4f)
                            }
                            titlePaint.textSize = size
                            titlePaint.color = if (node.level >= 6) {
                                Color.rgb(101, 109, 118)
                            } else {
                                Color.rgb(20, 20, 20)
                            }
                            val cleanHeaderText = InlineMarkdownParser.toPlainText(node.text)
                            val lines = wrapText(cleanHeaderText, titlePaint, contentWidth)
                            val totalHeight = spaceBefore + (lines.size * (size + 4f)) + spaceAfter
                            checkPageBreak(totalHeight)

                            currentY += spaceBefore
                            for (line in lines) {
                                canvas.drawText(line, MARGIN_HORIZONTAL, currentY, titlePaint)
                                currentY += size + 4f
                            }
                            if (node.level == 1 || node.level == 2) {
                                canvas.drawLine(
                                    MARGIN_HORIZONTAL,
                                    currentY - (size * 0.6f),
                                    MARGIN_HORIZONTAL + contentWidth,
                                    currentY - (size * 0.6f),
                                    linePaint
                                )
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
                            val levelOffset = node.indentLevel * 14f
                            val bulletIndent = 16f
                            val totalIndent = levelOffset + bulletIndent
                            val bulletWidth = (contentWidth - totalIndent).toInt().coerceAtLeast(40)
                            val layout = createLayout(ssb, textPaint, bulletWidth)

                            val firstLineHeight = if (layout.lineCount > 0) {
                                (layout.getLineBottom(0) - layout.getLineTop(0)).toFloat()
                            } else 15f
                            checkPageBreak(firstLineHeight)

                            val baseline = if (layout.lineCount > 0) layout.getLineBaseline(0).toFloat() else 12f
                            val bulletSymbol = when (node.indentLevel % 3) {
                                0 -> "•"
                                1 -> "◦"
                                else -> "▪"
                            }
                            canvas.drawText(bulletSymbol, MARGIN_HORIZONTAL + levelOffset, currentY + baseline, bulletPaint)

                            drawLayout(layout, MARGIN_HORIZONTAL + totalIndent, bulletWidth.toFloat(), spacingAfter = 4f)
                        }
                        is MarkdownNode.OrderedItem -> {
                            val spans = InlineMarkdownParser.parse(node.text)
                            val ssb = buildSpannable(spans)
                            val levelOffset = node.indentLevel * 14f
                            val numberIndent = 20f
                            val totalIndent = levelOffset + numberIndent
                            val itemWidth = (contentWidth - totalIndent).toInt().coerceAtLeast(40)
                            val layout = createLayout(ssb, textPaint, itemWidth)

                            val firstLineHeight = if (layout.lineCount > 0) {
                                (layout.getLineBottom(0) - layout.getLineTop(0)).toFloat()
                            } else 15f
                            checkPageBreak(firstLineHeight)

                            val baseline = if (layout.lineCount > 0) layout.getLineBaseline(0).toFloat() else 12f
                            canvas.drawText("${node.number}.", MARGIN_HORIZONTAL + levelOffset, currentY + baseline, bulletPaint)

                            drawLayout(layout, MARGIN_HORIZONTAL + totalIndent, itemWidth.toFloat(), spacingAfter = 4f)
                        }
                        is MarkdownNode.TaskItem -> {
                            val spans = InlineMarkdownParser.parse(node.text)
                            val ssb = buildSpannable(spans)
                            if (node.isChecked && ssb.isNotEmpty()) {
                                ssb.setSpan(StrikethroughSpan(), 0, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            }
                            val levelOffset = node.indentLevel * 14f
                            val taskIndent = 18f
                            val totalIndent = levelOffset + taskIndent
                            val taskWidth = (contentWidth - totalIndent).toInt().coerceAtLeast(40)
                            val itemPaint = if (node.isChecked) {
                                TextPaint(textPaint).apply { color = Color.rgb(120, 120, 120) }
                            } else {
                                textPaint
                            }
                            val layout = createLayout(ssb, itemPaint, taskWidth)

                            val firstLineHeight = if (layout.lineCount > 0) {
                                (layout.getLineBottom(0) - layout.getLineTop(0)).toFloat()
                            } else 15f
                            checkPageBreak(firstLineHeight)

                            val baseline = if (layout.lineCount > 0) layout.getLineBaseline(0).toFloat() else 12f
                            val checkboxSymbol = if (node.isChecked) "☑" else "☐"
                            canvas.drawText(checkboxSymbol, MARGIN_HORIZONTAL + levelOffset, currentY + baseline, bulletPaint)

                            drawLayout(layout, MARGIN_HORIZONTAL + totalIndent, taskWidth.toFloat(), spacingAfter = 4f)
                        }
                        is MarkdownNode.CodeBlock -> {
                            val codeLines = node.code.lines()
                            val hasLang = node.language.isNotBlank()
                            val headerOffset = if (hasLang) 14f else 0f
                            val blockHeight = headerOffset + (codeLines.size * 13f) + 16f
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
                            if (hasLang) {
                                canvas.drawText(node.language, MARGIN_HORIZONTAL + 8f, codeY, codeLangPaint)
                                codeY += 14f
                            }
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
                            val depthCount = node.depth.coerceIn(1, 5)
                            val quoteIndent = (depthCount * 10f) + 6f
                            val quoteWidth = (contentWidth - quoteIndent).toInt().coerceAtLeast(40)
                            val layout = createLayout(ssb, quotePaint, quoteWidth)

                            val startY = currentY
                            drawLayout(layout, MARGIN_HORIZONTAL + quoteIndent, quoteWidth.toFloat(), spacingAfter = 8f)
                            val blockHeight = (currentY - startY - 8f).coerceAtLeast(12f)

                            // Draw nested left accent bars
                            bgPaint.color = Color.rgb(46, 116, 181)
                            for (d in 0 until depthCount) {
                                val barX = MARGIN_HORIZONTAL + (d * 10f)
                                canvas.drawRect(
                                    barX,
                                    startY,
                                    barX + 3f,
                                    startY + blockHeight,
                                    bgPaint
                                )
                            }
                        }
                        is MarkdownNode.AlertBlock -> {
                            val (accentRgb, fillRgb) = when (node.type) {
                                AlertType.NOTE -> Color.rgb(9, 105, 218) to Color.rgb(240, 246, 255)
                                AlertType.TIP -> Color.rgb(26, 127, 55) to Color.rgb(238, 251, 242)
                                AlertType.IMPORTANT -> Color.rgb(130, 80, 223) to Color.rgb(247, 243, 255)
                                AlertType.WARNING -> Color.rgb(154, 103, 0) to Color.rgb(255, 248, 230)
                                AlertType.CAUTION -> Color.rgb(207, 34, 46) to Color.rgb(255, 240, 240)
                            }
                            val alertIndent = 14f
                            val alertWidth = (contentWidth - alertIndent - 8f).toInt().coerceAtLeast(40)
                            val headerSsb = SpannableStringBuilder("[${node.type.displayName}]").apply {
                                setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                                setSpan(ForegroundColorSpan(accentRgb), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            }
                            val headerLayout = createLayout(headerSsb, textPaint, alertWidth)
                            val bodySpans = InlineMarkdownParser.parse(node.text)
                            val bodyLayout = createLayout(buildSpannable(bodySpans), textPaint, alertWidth)

                            val boxHeight = headerLayout.height + bodyLayout.height + 16f
                            checkPageBreak(boxHeight)

                            val boxTop = currentY
                            bgPaint.color = fillRgb
                            canvas.drawRoundRect(
                                RectF(MARGIN_HORIZONTAL, boxTop, MARGIN_HORIZONTAL + contentWidth, boxTop + boxHeight),
                                4f,
                                4f,
                                bgPaint
                            )
                            bgPaint.color = accentRgb
                            canvas.drawRect(
                                MARGIN_HORIZONTAL,
                                boxTop,
                                MARGIN_HORIZONTAL + 4f,
                                boxTop + boxHeight,
                                bgPaint
                            )

                            currentY += 6f
                            drawLayout(headerLayout, MARGIN_HORIZONTAL + alertIndent, alertWidth.toFloat(), spacingAfter = 2f)
                            drawLayout(bodyLayout, MARGIN_HORIZONTAL + alertIndent, alertWidth.toFloat(), spacingAfter = 10f)
                        }
                        is MarkdownNode.DetailsBlock -> {
                            val summarySsb = SpannableStringBuilder("▸ ").apply {
                                append(buildSpannable(InlineMarkdownParser.parse(node.summary)))
                                setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            }
                            val summaryLayout = createLayout(summarySsb, textPaint, contentWidth.toInt())
                            drawLayout(summaryLayout, MARGIN_HORIZONTAL, contentWidth, spacingAfter = 4f)
                            if (node.body.isNotBlank()) {
                                renderNodes(MarkdownParser.parse(node.body))
                            }
                        }
                        is MarkdownNode.FootnoteDefinition -> {
                            val fnSsb = SpannableStringBuilder("[${node.id}]: ").apply {
                                setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                                append(buildSpannable(InlineMarkdownParser.parse(node.text)))
                            }
                            val fnLayout = createLayout(fnSsb, footnotePaint, contentWidth.toInt())
                            drawLayout(fnLayout, MARGIN_HORIZONTAL, contentWidth, spacingAfter = 4f)
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
                                    val headerLen = InlineMarkdownParser.toPlainText(node.headers[colIdx]).length
                                    val maxRowLen = node.rows.maxOfOrNull {
                                        InlineMarkdownParser.toPlainText(it.cells.getOrElse(colIdx) { "" }).length
                                    } ?: 0
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
                                    val align = when (node.alignments.getOrElse(colIdx) { TableAlignment.LEFT }) {
                                        TableAlignment.LEFT -> Layout.Alignment.ALIGN_NORMAL
                                        TableAlignment.CENTER -> Layout.Alignment.ALIGN_CENTER
                                        TableAlignment.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
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
            }

            renderNodes(MarkdownParser.parse(note.content))

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
