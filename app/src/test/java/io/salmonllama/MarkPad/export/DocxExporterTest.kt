package io.salmonllama.MarkPad.export

import io.salmonllama.MarkPad.model.Note
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

class DocxExporterTest {

    @Test
    fun export_createsValidDocxZipStructure() {
        val note = Note(
            id = "docx-test",
            title = "Docx Sample",
            content = """
                # Project Overview
                ## Features
                This is a **bold** paragraph with & special characters.
                - Item One
                - Item Two
                ```kotlin
                val message = "Hello"
                ```
            """.trimIndent(),
            updatedAt = System.currentTimeMillis()
        )

        val exporter = DocxExporter()
        val outputStream = ByteArrayOutputStream()
        exporter.export(note, outputStream)

        val bytes = outputStream.toByteArray()
        assertTrue("Output should not be empty", bytes.isNotEmpty())

        val zipEntries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                val content = zipIn.bufferedReader().readText()
                zipEntries[entry.name] = content
                entry = zipIn.nextEntry
            }
        }

        // Verify required OPC package entries exist
        assertTrue("Contains [Content_Types].xml", zipEntries.containsKey("[Content_Types].xml"))
        assertTrue("Contains _rels/.rels", zipEntries.containsKey("_rels/.rels"))
        assertTrue("Contains word/_rels/document.xml.rels", zipEntries.containsKey("word/_rels/document.xml.rels"))
        assertTrue("Contains word/styles.xml", zipEntries.containsKey("word/styles.xml"))
        assertTrue("Contains word/document.xml", zipEntries.containsKey("word/document.xml"))

        val documentXml = zipEntries["word/document.xml"]
        assertNotNull(documentXml)
        assertTrue("Contains header", documentXml!!.contains("Project Overview"))
        assertTrue("Contains subheader", documentXml.contains("Features"))
        assertTrue("Escaped ampersand", documentXml.contains("&amp;"))
        assertTrue("Contains bullet item", documentXml.contains("Item One"))
        assertTrue("Contains code content", documentXml.contains("val message = &quot;Hello&quot;") || documentXml.contains("val message = \"Hello\"") || documentXml.contains("&quot;Hello&quot;"))
    }

    @Test
    fun export_correctlyGeneratesDocxTableStructure() {
        val note = Note(
            id = "docx-table-test",
            title = "Table Note",
            content = """
                # Performance Comparison

                | Feature | Status | Impact |
                | :--- | :---: | ---: |
                | Caching | **Enabled** | High |
                | Indexing | *Active* | Medium |
            """.trimIndent(),
            updatedAt = System.currentTimeMillis()
        )

        val exporter = DocxExporter()
        val outputStream = ByteArrayOutputStream()
        exporter.export(note, outputStream)

        val bytes = outputStream.toByteArray()
        assertTrue("Output should not be empty", bytes.isNotEmpty())

        val zipEntries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                val content = zipIn.bufferedReader().readText()
                zipEntries[entry.name] = content
                entry = zipIn.nextEntry
            }
        }

        val documentXml = zipEntries["word/document.xml"]
        assertNotNull(documentXml)
        assertTrue("Contains table element", documentXml!!.contains("<w:tbl>"))
        assertTrue("Contains table header declaration", documentXml.contains("<w:tblHeader/>"))
        assertTrue("Contains header cell text", documentXml.contains("Feature"))
        assertTrue("Contains header cell text", documentXml.contains("Status"))
        assertTrue("Contains header cell text", documentXml.contains("Impact"))
        assertTrue("Contains body row text", documentXml.contains("Caching"))
        assertTrue("Contains formatted bold cell text", documentXml.contains("Enabled"))
        assertTrue("Contains left justification", documentXml.contains("<w:jc w:val=\"left\"/>"))
        assertTrue("Contains center justification", documentXml.contains("<w:jc w:val=\"center\"/>"))
        assertTrue("Contains right justification", documentXml.contains("<w:jc w:val=\"right\"/>"))
    }

    @Test
    fun export_rendersTaskItemsWithCheckboxesAndStrikethrough() {
        val note = Note(
            id = "docx-task-test",
            title = "Task List Note",
            content = """
                # Sprint Checklist
                - [ ] Pending task item
                - [x] Completed task item
            """.trimIndent(),
            updatedAt = System.currentTimeMillis()
        )

        val exporter = DocxExporter()
        val outputStream = ByteArrayOutputStream()
        exporter.export(note, outputStream)

        val bytes = outputStream.toByteArray()
        val zipEntries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                zipEntries[entry.name] = zipIn.bufferedReader().readText()
                entry = zipIn.nextEntry
            }
        }

        val documentXml = zipEntries["word/document.xml"]
        assertNotNull(documentXml)
        assertTrue("Contains unchecked symbol", documentXml!!.contains("☐ "))
        assertTrue("Contains checked symbol", documentXml.contains("☑ "))
        assertTrue("Contains pending task text", documentXml.contains("Pending task item"))
        assertTrue("Contains completed task text", documentXml.contains("Completed task item"))
        assertTrue("Contains strikethrough formatting for completed task", documentXml.contains("<w:strike/>"))
    }

    @Test
    fun export_rendersFullGfmBlocksAndInlineSpans() {
        val note = Note(
            id = "docx-gfm-test",
            title = "Full GFM Document",
            content = """
                #### Heading Level 4
                ###### Heading Level 6

                > [!WARNING]
                > Backup your database before migrating.

                1. First ordered item
                  1. Nested ordered item

                Visit [GitHub](https://github.com) with ~~strikethrough~~, <kbd>Ctrl+C</kbd>, H<sub>2</sub>O, E=mc<sup>2</sup>, ==highlight==, and footnote[^1].

                <details>
                <summary>Expandable Architecture</summary>
                Detailed subsystem notes.
                </details>

                [^1]: Footnote explanation text.
            """.trimIndent(),
            updatedAt = System.currentTimeMillis()
        )

        val exporter = DocxExporter()
        val outputStream = ByteArrayOutputStream()
        exporter.export(note, outputStream)

        val zipEntries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(outputStream.toByteArray())).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                zipEntries[entry.name] = zipIn.bufferedReader().readText()
                entry = zipIn.nextEntry
            }
        }

        val stylesXml = zipEntries["word/styles.xml"]!!
        assertTrue("Defines Heading4 style", stylesXml.contains("w:styleId=\"Heading4\""))
        assertTrue("Defines Heading6 style", stylesXml.contains("w:styleId=\"Heading6\""))

        val relsXml = zipEntries["word/_rels/document.xml.rels"]!!
        assertTrue("Registers external hyperlink relationship", relsXml.contains("Target=\"https://github.com\""))
        assertTrue("Uses External TargetMode", relsXml.contains("TargetMode=\"External\""))

        val documentXml = zipEntries["word/document.xml"]!!
        assertTrue("Renders Heading4 paragraph", documentXml.contains("<w:pStyle w:val=\"Heading4\"/>"))
        assertTrue("Renders Heading6 paragraph", documentXml.contains("<w:pStyle w:val=\"Heading6\"/>"))
        assertTrue("Renders Warning alert header", documentXml.contains("[Warning] "))
        assertTrue("Renders ordered list numbering", documentXml.contains("1. "))
        assertTrue("Renders hyperlink tag", documentXml.contains("<w:hyperlink r:id=\"rIdLink1\">"))
        assertTrue("Renders strikethrough tag", documentXml.contains("<w:strike/>"))
        assertTrue("Renders subscript tag", documentXml.contains("<w:vertAlign w:val=\"subscript\"/>"))
        assertTrue("Renders superscript tag", documentXml.contains("<w:vertAlign w:val=\"superscript\"/>"))
        assertTrue("Renders highlight shading", documentXml.contains("w:fill=\"FFF8C5\""))
        assertTrue("Renders details summary", documentXml.contains("Expandable Architecture"))
        assertTrue("Renders details body", documentXml.contains("Detailed subsystem notes."))
        assertTrue("Renders footnote definition", documentXml.contains("[1]: "))
    }
}
