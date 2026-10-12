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
}
