package io.salmonllama.MarkPad.export

import io.salmonllama.MarkPad.model.Note
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

class MarkdownExporterTest {

    @Test
    fun export_writesExactMarkdownContent() {
        val note = Note(
            id = "test-note",
            title = "Sample Note",
            content = "# Sample Note\n\nThis is content with **bold** and `code`.",
            updatedAt = System.currentTimeMillis()
        )

        val exporter = MarkdownExporter()
        val outputStream = ByteArrayOutputStream()
        exporter.export(note, outputStream)

        val result = outputStream.toString(StandardCharsets.UTF_8.name())
        assertEquals(note.content, result)
    }

    @Test
    fun export_handlesEmptyContent() {
        val note = Note(
            id = "empty-note",
            title = "Empty",
            content = "",
            updatedAt = System.currentTimeMillis()
        )

        val exporter = MarkdownExporter()
        val outputStream = ByteArrayOutputStream()
        exporter.export(note, outputStream)

        assertEquals("", outputStream.toString(StandardCharsets.UTF_8.name()))
    }
}
