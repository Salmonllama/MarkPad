package io.salmonllama.MarkPad.export

import io.salmonllama.MarkPad.model.Note
import io.salmonllama.MarkPad.ui.markdown.AlertType
import io.salmonllama.MarkPad.ui.markdown.InlineMarkdownParser
import io.salmonllama.MarkPad.ui.markdown.InlineSpan
import io.salmonllama.MarkPad.ui.markdown.MarkdownNode
import io.salmonllama.MarkPad.ui.markdown.MarkdownParser
import io.salmonllama.MarkPad.ui.markdown.TableAlignment
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DocxExporter : NoteExporter {

    override fun export(note: Note, outputStream: OutputStream) {
        val hyperlinks = LinkedHashMap<String, String>()
        fun registerLink(url: String): String {
            return hyperlinks.getOrPut(url) { "rIdLink${hyperlinks.size + 1}" }
        }

        val documentXml = buildDocumentXml(note, ::registerLink)

        val zipOut = ZipOutputStream(outputStream)
        zipOut.use { zip ->
            // 1. [Content_Types].xml
            writeEntry(zip, "[Content_Types].xml", buildContentTypesXml())

            // 2. _rels/.rels
            writeEntry(zip, "_rels/.rels", buildPackageRelsXml())

            // 3. word/_rels/document.xml.rels
            writeEntry(zip, "word/_rels/document.xml.rels", buildDocumentRelsXml(hyperlinks))

            // 4. word/styles.xml
            writeEntry(zip, "word/styles.xml", buildStylesXml())

            // 5. word/document.xml
            writeEntry(zip, "word/document.xml", documentXml)
        }
    }

    private fun writeEntry(zip: ZipOutputStream, path: String, content: String) {
        val entry = ZipEntry(path)
        zip.putNextEntry(entry)
        zip.write(content.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()
    }

    private fun buildContentTypesXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
          <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
          <Default Extension="xml" ContentType="application/xml"/>
          <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
          <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
        </Types>
    """.trimIndent()

    private fun buildPackageRelsXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
        </Relationships>
    """.trimIndent()

    private fun buildDocumentRelsXml(hyperlinks: Map<String, String>): String {
        val linkRels = buildString {
            for ((url, rId) in hyperlinks) {
                append("\n  ")
                append(
                    """<Relationship Id="$rId" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/hyperlink" Target="${escapeXml(url)}" TargetMode="External"/>"""
                )
            }
        }
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>$linkRels
            </Relationships>
        """.trimIndent()
    }

    private fun buildStylesXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
          <w:docDefaults>
            <w:rPrDefault>
              <w:rPr>
                <w:rFonts w:ascii="Calibri" w:hAnsi="Calibri"/>
                <w:sz w:val="22"/>
              </w:rPr>
            </w:rPrDefault>
          </w:docDefaults>
          <w:style w:type="paragraph" w:styleId="Heading1">
            <w:name w:val="heading 1"/>
            <w:rPr>
              <w:b/>
              <w:sz w:val="36"/>
              <w:color w:val="1F497D"/>
            </w:rPr>
          </w:style>
          <w:style w:type="paragraph" w:styleId="Heading2">
            <w:name w:val="heading 2"/>
            <w:rPr>
              <w:b/>
              <w:sz w:val="28"/>
              <w:color w:val="2E74B5"/>
            </w:rPr>
          </w:style>
          <w:style w:type="paragraph" w:styleId="Heading3">
            <w:name w:val="heading 3"/>
            <w:rPr>
              <w:b/>
              <w:sz w:val="24"/>
              <w:color w:val="333333"/>
            </w:rPr>
          </w:style>
          <w:style w:type="paragraph" w:styleId="Heading4">
            <w:name w:val="heading 4"/>
            <w:rPr>
              <w:b/>
              <w:sz w:val="22"/>
              <w:color w:val="333333"/>
            </w:rPr>
          </w:style>
          <w:style w:type="paragraph" w:styleId="Heading5">
            <w:name w:val="heading 5"/>
            <w:rPr>
              <w:b/>
              <w:sz w:val="20"/>
              <w:color w:val="333333"/>
            </w:rPr>
          </w:style>
          <w:style w:type="paragraph" w:styleId="Heading6">
            <w:name w:val="heading 6"/>
            <w:rPr>
              <w:b/>
              <w:sz w:val="20"/>
              <w:color w:val="656D76"/>
            </w:rPr>
          </w:style>
          <w:style w:type="paragraph" w:styleId="CodeBlock">
            <w:name w:val="Code Block"/>
            <w:rPr>
              <w:rFonts w:ascii="Consolas" w:hAnsi="Consolas"/>
              <w:sz w:val="19"/>
            </w:rPr>
          </w:style>
        </w:styles>
    """.trimIndent()

    private fun buildDocumentXml(
        note: Note,
        registerLink: (String) -> String
    ): String {
        val bodyContent = StringBuilder()

        fun renderNodes(nodes: List<MarkdownNode>) {
            for (node in nodes) {
                when (node) {
                    is MarkdownNode.Header -> {
                        val styleId = when (node.level) {
                            1 -> "Heading1"
                            2 -> "Heading2"
                            3 -> "Heading3"
                            4 -> "Heading4"
                            5 -> "Heading5"
                            else -> "Heading6"
                        }
                        bodyContent.append("""<w:p><w:pPr><w:pStyle w:val="$styleId"/></w:pPr>${renderParagraphRuns(node.text, registerLink)}</w:p>""")
                    }
                    is MarkdownNode.Paragraph -> {
                        bodyContent.append("""<w:p>${renderParagraphRuns(node.text, registerLink)}</w:p>""")
                    }
                    is MarkdownNode.BulletItem -> {
                        val leftTwips = (node.indentLevel + 1) * 420
                        val bulletSymbol = when (node.indentLevel % 3) {
                            0 -> "• "
                            1 -> "◦ "
                            else -> "▪ "
                        }
                        bodyContent.append("""<w:p><w:pPr><w:ind w:left="$leftTwips"/></w:pPr><w:r><w:t xml:space="preserve">$bulletSymbol</w:t></w:r>${renderParagraphRuns(node.text, registerLink)}</w:p>""")
                    }
                    is MarkdownNode.OrderedItem -> {
                        val leftTwips = (node.indentLevel + 1) * 420
                        bodyContent.append("""<w:p><w:pPr><w:ind w:left="$leftTwips"/></w:pPr><w:r><w:t xml:space="preserve">${node.number}. </w:t></w:r>${renderParagraphRuns(node.text, registerLink)}</w:p>""")
                    }
                    is MarkdownNode.TaskItem -> {
                        val leftTwips = (node.indentLevel + 1) * 420
                        val checkboxSymbol = if (node.isChecked) "☑ " else "☐ "
                        val runs = if (node.isChecked) {
                            renderCheckedTaskRuns(node.text, registerLink)
                        } else {
                            renderParagraphRuns(node.text, registerLink)
                        }
                        bodyContent.append("""<w:p><w:pPr><w:ind w:left="$leftTwips"/></w:pPr><w:r><w:t xml:space="preserve">$checkboxSymbol</w:t></w:r>$runs</w:p>""")
                    }
                    is MarkdownNode.CodeBlock -> {
                        if (node.language.isNotBlank()) {
                            bodyContent.append("""<w:p><w:pPr><w:pStyle w:val="CodeBlock"/><w:shd w:val="clear" w:color="auto" w:fill="E9ECEF"/></w:pPr><w:r><w:rPr><w:b/><w:rFonts w:ascii="Consolas" w:hAnsi="Consolas"/><w:color w:val="57606A"/></w:rPr><w:t xml:space="preserve">${escapeXml(node.language)}</w:t></w:r></w:p>""")
                        }
                        val codeLines = node.code.lines()
                        for (line in codeLines) {
                            bodyContent.append("""<w:p><w:pPr><w:pStyle w:val="CodeBlock"/><w:shd w:val="clear" w:color="auto" w:fill="F4F4F4"/></w:pPr><w:r><w:rPr><w:rFonts w:ascii="Consolas" w:hAnsi="Consolas"/></w:rPr><w:t xml:space="preserve">${escapeXml(line)}</w:t></w:r></w:p>""")
                        }
                    }
                    is MarkdownNode.Blockquote -> {
                        val leftTwips = node.depth.coerceIn(1, 5) * 360
                        bodyContent.append("""<w:p><w:pPr><w:pBdr><w:left w:val="single" w:sz="18" w:space="10" w:color="2E74B5"/></w:pBdr><w:ind w:left="$leftTwips"/></w:pPr>${renderBlockquoteRuns(node.text, registerLink)}</w:p>""")
                    }
                    is MarkdownNode.AlertBlock -> {
                        val (borderColor, fillColor) = when (node.type) {
                            AlertType.NOTE -> "0969DA" to "F0F6FF"
                            AlertType.TIP -> "1A7F37" to "EEFBEE"
                            AlertType.IMPORTANT -> "8250DF" to "F7F3FF"
                            AlertType.WARNING -> "9A6700" to "FFF8E6"
                            AlertType.CAUTION -> "CF222E" to "FFF0F0"
                        }
                        bodyContent.append(
                            """<w:p><w:pPr><w:pBdr><w:left w:val="single" w:sz="24" w:space="10" w:color="$borderColor"/></w:pBdr><w:shd w:val="clear" w:color="auto" w:fill="$fillColor"/><w:ind w:left="360"/></w:pPr><w:r><w:rPr><w:b/><w:color w:val="$borderColor"/></w:rPr><w:t xml:space="preserve">[${node.type.displayName}] </w:t></w:r>${renderParagraphRuns(node.text, registerLink)}</w:p>"""
                        )
                    }
                    is MarkdownNode.DetailsBlock -> {
                        bodyContent.append(
                            """<w:p><w:r><w:rPr><w:b/></w:rPr><w:t xml:space="preserve">▸ </w:t></w:r>${renderTableHeaderRuns(node.summary, registerLink)}</w:p>"""
                        )
                        if (node.body.isNotBlank()) {
                            renderNodes(MarkdownParser.parse(node.body))
                        }
                    }
                    is MarkdownNode.FootnoteDefinition -> {
                        bodyContent.append(
                            """<w:p><w:r><w:rPr><w:b/><w:sz w:val="18"/><w:color w:val="57606A"/></w:rPr><w:t xml:space="preserve">[${escapeXml(node.id)}]: </w:t></w:r>${renderParagraphRuns(node.text, registerLink)}</w:p>"""
                        )
                    }
                    is MarkdownNode.HorizontalRule -> {
                        bodyContent.append("""<w:p><w:pPr><w:pBdr><w:bottom w:val="single" w:sz="6" w:space="1" w:color="CCCCCC"/></w:pBdr></w:pPr></w:p>""")
                    }
                    is MarkdownNode.Table -> {
                        bodyContent.append(renderTableXml(node, registerLink))
                    }
                }
            }
        }

        renderNodes(MarkdownParser.parse(note.content))

        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
              <w:body>
                $bodyContent
                <w:sectPr>
                  <w:pgSz w:w="11906" w:h="16838"/>
                  <w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440"/>
                </w:sectPr>
              </w:body>
            </w:document>
        """.trimIndent()
    }

    private fun renderParagraphRuns(
        text: String,
        registerLink: (String) -> String
    ): String {
        return renderInlineRuns(
            text = text,
            registerLink = registerLink,
            forceBold = false,
            forceItalic = false,
            forceStrike = false,
            defaultColor = null
        )
    }

    private fun renderCheckedTaskRuns(
        text: String,
        registerLink: (String) -> String
    ): String {
        return renderInlineRuns(
            text = text,
            registerLink = registerLink,
            forceBold = false,
            forceItalic = false,
            forceStrike = true,
            defaultColor = "777777"
        )
    }

    private fun renderBlockquoteRuns(
        text: String,
        registerLink: (String) -> String
    ): String {
        return renderInlineRuns(
            text = text,
            registerLink = registerLink,
            forceBold = false,
            forceItalic = true,
            forceStrike = false,
            defaultColor = "555555"
        )
    }

    private fun renderTableHeaderRuns(
        text: String,
        registerLink: (String) -> String
    ): String {
        return renderInlineRuns(
            text = text,
            registerLink = registerLink,
            forceBold = true,
            forceItalic = false,
            forceStrike = false,
            defaultColor = null
        )
    }

    private fun renderInlineRuns(
        text: String,
        registerLink: (String) -> String,
        forceBold: Boolean,
        forceItalic: Boolean,
        forceStrike: Boolean,
        defaultColor: String?
    ): String {
        val builder = StringBuilder()
        val spans = InlineMarkdownParser.parse(text)
        for (span in spans) {
            val isBold = forceBold || span is InlineSpan.Bold || span is InlineSpan.BoldItalic
            val isItalic = forceItalic || span is InlineSpan.Italic || span is InlineSpan.BoldItalic || span is InlineSpan.Image
            val isStrike = forceStrike || span is InlineSpan.Strikethrough
            val isUnderline = span is InlineSpan.Underline || span is InlineSpan.Link
            val isCode = span is InlineSpan.InlineCode || span is InlineSpan.KeyboardKey
            val isKbd = span is InlineSpan.KeyboardKey
            val isHighlight = span is InlineSpan.Highlight
            val isSup = span is InlineSpan.Superscript || span is InlineSpan.FootnoteReference
            val isSub = span is InlineSpan.Subscript

            val colorHex = when {
                span is InlineSpan.Link || span is InlineSpan.FootnoteReference -> "0969DA"
                span is InlineSpan.Image -> "57606A"
                else -> defaultColor
            }

            val rPrContent = buildString {
                if (isBold) append("<w:b/>")
                if (isItalic) append("<w:i/>")
                if (isStrike) append("<w:strike/>")
                if (isUnderline) append("""<w:u w:val="single"/>""")
                if (isCode) append("""<w:rFonts w:ascii="Consolas" w:hAnsi="Consolas"/>""")
                if (isKbd) append("""<w:bdr w:val="single" w:sz="4" w:space="1" w:color="D0D7DE"/>""")
                if (colorHex != null) append("""<w:color w:val="$colorHex"/>""")
                when {
                    isHighlight -> append("""<w:shd w:val="clear" w:color="auto" w:fill="FFF8C5"/>""")
                    isKbd -> append("""<w:shd w:val="clear" w:color="auto" w:fill="F6F8FA"/>""")
                    span is InlineSpan.InlineCode -> {
                        val fill = if (forceBold) "E9ECEF" else "F0F0F0"
                        append("""<w:shd w:val="clear" w:color="auto" w:fill="$fill"/>""")
                    }
                }
                if (isSup) append("""<w:vertAlign w:val="superscript"/>""")
                if (isSub) append("""<w:vertAlign w:val="subscript"/>""")
            }

            val rPrXml = if (rPrContent.isNotEmpty()) "<w:rPr>$rPrContent</w:rPr>" else ""
            val displayText = when (span) {
                is InlineSpan.KeyboardKey -> "[${span.text}]"
                is InlineSpan.Image -> "[Image: ${span.text.ifBlank { span.url }} (${span.url})]"
                else -> span.text
            }
            val runXml = """<w:r>$rPrXml<w:t xml:space="preserve">${escapeXml(displayText)}</w:t></w:r>"""

            if (span is InlineSpan.Link) {
                val rId = registerLink(span.url)
                builder.append("""<w:hyperlink r:id="$rId">$runXml</w:hyperlink>""")
            } else {
                builder.append(runXml)
            }
        }
        return builder.toString()
    }

    private fun renderTableXml(
        table: MarkdownNode.Table,
        registerLink: (String) -> String
    ): String {
        if (table.headers.isEmpty()) return ""
        val colCount = table.headers.size
        val totalWidth = 9026 // Twips: A4 page width (11906) - 2 * 1440 margin
        val colWidth = totalWidth / maxOf(1, colCount)

        val gridBuilder = StringBuilder()
        for (i in 0 until colCount) {
            gridBuilder.append("""<w:gridCol w:w="$colWidth"/>""")
        }

        val rowsBuilder = StringBuilder()

        // Header Row
        val headerCellsBuilder = StringBuilder()
        table.headers.forEachIndexed { colIdx, headerText ->
            val alignment = table.alignments.getOrElse(colIdx) { TableAlignment.LEFT }
            val jcVal = when (alignment) {
                TableAlignment.LEFT -> "left"
                TableAlignment.CENTER -> "center"
                TableAlignment.RIGHT -> "right"
            }
            headerCellsBuilder.append(
                """<w:tc><w:tcPr><w:tcW w:w="$colWidth" w:type="dxa"/><w:shd w:val="clear" w:color="auto" w:fill="F2F4F7"/><w:tcMar><w:top w:w="120" w:type="dxa"/><w:bottom w:w="120" w:type="dxa"/><w:left w:w="160" w:type="dxa"/><w:right w:w="160" w:type="dxa"/></w:tcMar></w:tcPr><w:p><w:pPr><w:jc w:val="$jcVal"/></w:pPr>${renderTableHeaderRuns(headerText, registerLink)}</w:p></w:tc>"""
            )
        }
        rowsBuilder.append("""<w:tr><w:trPr><w:tblHeader/></w:trPr>$headerCellsBuilder</w:tr>""")

        // Data Rows
        table.rows.forEachIndexed { rowIndex, row ->
            val rowCellsBuilder = StringBuilder()
            val fillShade = if (rowIndex % 2 == 1) """<w:shd w:val="clear" w:color="auto" w:fill="F9FAFB"/>""" else ""
            table.headers.indices.forEach { colIdx ->
                val cellText = row.cells.getOrElse(colIdx) { "" }
                val alignment = table.alignments.getOrElse(colIdx) { TableAlignment.LEFT }
                val jcVal = when (alignment) {
                    TableAlignment.LEFT -> "left"
                    TableAlignment.CENTER -> "center"
                    TableAlignment.RIGHT -> "right"
                }
                rowCellsBuilder.append(
                    """<w:tc><w:tcPr><w:tcW w:w="$colWidth" w:type="dxa"/>$fillShade<w:tcMar><w:top w:w="120" w:type="dxa"/><w:bottom w:w="120" w:type="dxa"/><w:left w:w="160" w:type="dxa"/><w:right w:w="160" w:type="dxa"/></w:tcMar></w:tcPr><w:p><w:pPr><w:jc w:val="$jcVal"/></w:pPr>${renderParagraphRuns(cellText, registerLink)}</w:p></w:tc>"""
                )
            }
            rowsBuilder.append("""<w:tr>$rowCellsBuilder</w:tr>""")
        }

        return """
            <w:tbl>
              <w:tblPr>
                <w:tblW w:w="$totalWidth" w:type="dxa"/>
                <w:tblBorders>
                  <w:top w:val="single" w:sz="4" w:space="0" w:color="D0D5DD"/>
                  <w:left w:val="single" w:sz="4" w:space="0" w:color="D0D5DD"/>
                  <w:bottom w:val="single" w:sz="4" w:space="0" w:color="D0D5DD"/>
                  <w:right w:val="single" w:sz="4" w:space="0" w:color="D0D5DD"/>
                  <w:insideH w:val="single" w:sz="4" w:space="0" w:color="E4E7EC"/>
                  <w:insideV w:val="single" w:sz="4" w:space="0" w:color="E4E7EC"/>
                </w:tblBorders>
              </w:tblPr>
              <w:tblGrid>$gridBuilder</w:tblGrid>
              $rowsBuilder
            </w:tbl>
            <w:p><w:pPr><w:spacing w:after="160"/></w:pPr></w:p>
        """.trimIndent()
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
