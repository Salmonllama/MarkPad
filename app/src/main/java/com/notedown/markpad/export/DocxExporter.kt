package com.notedown.markpad.export

import com.notedown.markpad.model.Note
import com.notedown.markpad.ui.markdown.MarkdownNode
import com.notedown.markpad.ui.markdown.MarkdownParser
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DocxExporter : NoteExporter {

    override fun export(note: Note, outputStream: OutputStream) {
        val zipOut = ZipOutputStream(outputStream)
        zipOut.use { zip ->
            // 1. [Content_Types].xml
            writeEntry(zip, "[Content_Types].xml", buildContentTypesXml())

            // 2. _rels/.rels
            writeEntry(zip, "_rels/.rels", buildPackageRelsXml())

            // 3. word/_rels/document.xml.rels
            writeEntry(zip, "word/_rels/document.xml.rels", buildDocumentRelsXml())

            // 4. word/styles.xml
            writeEntry(zip, "word/styles.xml", buildStylesXml())

            // 5. word/document.xml
            writeEntry(zip, "word/document.xml", buildDocumentXml(note))
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

    private fun buildDocumentRelsXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
        </Relationships>
    """.trimIndent()

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
          <w:style w:type="paragraph" w:styleId="CodeBlock">
            <w:name w:val="Code Block"/>
            <w:rPr>
              <w:rFonts w:ascii="Consolas" w:hAnsi="Consolas"/>
              <w:sz w:val="19"/>
            </w:rPr>
          </w:style>
        </w:styles>
    """.trimIndent()

    private fun buildDocumentXml(note: Note): String {
        val nodes = MarkdownParser.parse(note.content)
        val bodyContent = StringBuilder()

        for (node in nodes) {
            when (node) {
                is MarkdownNode.Header -> {
                    val styleId = when (node.level) {
                        1 -> "Heading1"
                        2 -> "Heading2"
                        else -> "Heading3"
                    }
                    bodyContent.append("""<w:p><w:pPr><w:pStyle w:val="$styleId"/></w:pPr>${renderParagraphRuns(node.text)}</w:p>""")
                }
                is MarkdownNode.Paragraph -> {
                    bodyContent.append("""<w:p>${renderParagraphRuns(node.text)}</w:p>""")
                }
                is MarkdownNode.BulletItem -> {
                    bodyContent.append("""<w:p><w:pPr><w:ind w:left="420"/></w:pPr><w:r><w:t xml:space="preserve">• </w:t></w:r>${renderParagraphRuns(node.text)}</w:p>""")
                }
                is MarkdownNode.CodeBlock -> {
                    val codeLines = node.code.lines()
                    for (line in codeLines) {
                        bodyContent.append("""<w:p><w:pPr><w:pStyle w:val="CodeBlock"/><w:shd w:val="clear" w:color="auto" w:fill="F4F4F4"/></w:pPr><w:r><w:rPr><w:rFonts w:ascii="Consolas" w:hAnsi="Consolas"/></w:rPr><w:t xml:space="preserve">${escapeXml(line)}</w:t></w:r></w:p>""")
                    }
                }
                is MarkdownNode.Blockquote -> {
                    bodyContent.append("""<w:p><w:pPr><w:pBdr><w:left w:val="single" w:sz="18" w:space="10" w:color="2E74B5"/></w:pBdr><w:ind w:left="360"/></w:pPr><w:r><w:rPr><w:i/><w:color w:val="555555"/></w:rPr><w:t xml:space="preserve">${escapeXml(node.text)}</w:t></w:r></w:p>""")
                }
                is MarkdownNode.HorizontalRule -> {
                    bodyContent.append("""<w:p><w:pPr><w:pBdr><w:bottom w:val="single" w:sz="6" w:space="1" w:color="CCCCCC"/></w:pBdr></w:pPr></w:p>""")
                }
            }
        }

        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
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

    private fun renderParagraphRuns(text: String): String {
        val builder = StringBuilder()
        val spans = com.notedown.markpad.ui.markdown.InlineMarkdownParser.parse(text)
        for (span in spans) {
            when (span) {
                is com.notedown.markpad.ui.markdown.InlineSpan.Bold -> {
                    builder.append("""<w:r><w:rPr><w:b/></w:rPr><w:t xml:space="preserve">${escapeXml(span.text)}</w:t></w:r>""")
                }
                is com.notedown.markpad.ui.markdown.InlineSpan.Italic -> {
                    builder.append("""<w:r><w:rPr><w:i/></w:rPr><w:t xml:space="preserve">${escapeXml(span.text)}</w:t></w:r>""")
                }
                is com.notedown.markpad.ui.markdown.InlineSpan.BoldItalic -> {
                    builder.append("""<w:r><w:rPr><w:b/><w:i/></w:rPr><w:t xml:space="preserve">${escapeXml(span.text)}</w:t></w:r>""")
                }
                is com.notedown.markpad.ui.markdown.InlineSpan.InlineCode -> {
                    builder.append("""<w:r><w:rPr><w:rFonts w:ascii="Consolas" w:hAnsi="Consolas"/><w:shd w:val="clear" w:color="auto" w:fill="F0F0F0"/></w:rPr><w:t xml:space="preserve">${escapeXml(span.text)}</w:t></w:r>""")
                }
                is com.notedown.markpad.ui.markdown.InlineSpan.Normal -> {
                    builder.append("""<w:r><w:t xml:space="preserve">${escapeXml(span.text)}</w:t></w:r>""")
                }
            }
        }
        return builder.toString()
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
