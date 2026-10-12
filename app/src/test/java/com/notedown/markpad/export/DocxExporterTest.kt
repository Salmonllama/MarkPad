package com.notedown.markpad.export

import com.notedown.markpad.model.Note
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
}
