package com.notedown.markpad.export

import com.notedown.markpad.model.Note
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class PdfExporterTest {

    @Test
    fun export_delegatesToDocumentWriter() {
        var recordedNote: Note? = null
        var recordedBytes: ByteArray? = null

        val exporter = PdfExporter { note, out ->
            recordedNote = note
            out.write("PDF_TABLE_OUTPUT".toByteArray())
        }

        val testNote = Note(
            id = "pdf-test",
            title = "Table PDF",
            content = """
                | Service | Tier | Cost |
                | :--- | :---: | ---: |
                | Compute | Enterprise | ${'$'}150 |
            """.trimIndent(),
            updatedAt = System.currentTimeMillis()
        )

        val output = ByteArrayOutputStream()
        exporter.export(testNote, output)

        assertEquals("pdf-test", recordedNote?.id)
        assertTrue(recordedNote?.content?.contains("Service") == true)
        assertEquals("PDF_TABLE_OUTPUT", String(output.toByteArray()))
    }
}
