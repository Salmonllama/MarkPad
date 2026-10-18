package io.salmonllama.MarkPad.export

import io.salmonllama.MarkPad.model.Note
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.OutputStream

class ExportManagerTest {

    @Test
    fun sanitizeFileName_removesIllegalCharactersAndSpaces() {
        assertEquals("My_Project_Report", ExportManager.sanitizeFileName("My Project Report"))
        assertEquals("Invalid-File-Name", ExportManager.sanitizeFileName("Invalid:File/Name"))
        assertEquals("Notes_2026-10-12", ExportManager.sanitizeFileName("Notes 2026/10/12"))
        assertEquals("Clean_Title", ExportManager.sanitizeFileName("  Clean Title  "))
        assertEquals("scratchpad", ExportManager.sanitizeFileName("   "))
        assertEquals("scratchpad", ExportManager.sanitizeFileName("???:::***"))
    }

    @Test
    fun export_delegatesToCorrectExporter() {
        var markdownCalled = false
        var docxCalled = false
        var pdfCalled = false

        val dummyNote = Note("1", "Title", "Content", 0L)
        val dummyStream = ByteArrayOutputStream()

        val mockMarkdown = object : NoteExporter {
            override fun export(note: Note, outputStream: OutputStream) {
                markdownCalled = true
            }
        }
        val mockDocx = object : NoteExporter {
            override fun export(note: Note, outputStream: OutputStream) {
                docxCalled = true
            }
        }
        val mockPdf = object : NoteExporter {
            override fun export(note: Note, outputStream: OutputStream) {
                pdfCalled = true
            }
        }

        val manager = ExportManager(mockMarkdown, mockDocx, mockPdf)

        manager.export(dummyNote, ExportFormat.MARKDOWN, dummyStream)
        assertTrue("Markdown exporter was called", markdownCalled)

        manager.export(dummyNote, ExportFormat.DOCX, dummyStream)
        assertTrue("Docx exporter was called", docxCalled)

        manager.export(dummyNote, ExportFormat.PDF, dummyStream)
        assertTrue("Pdf exporter was called", pdfCalled)
    }
}
