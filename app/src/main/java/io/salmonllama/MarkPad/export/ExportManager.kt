package io.salmonllama.MarkPad.export

import io.salmonllama.MarkPad.model.Note
import java.io.OutputStream

class ExportManager(
    private val markdownExporter: NoteExporter = MarkdownExporter(),
    private val docxExporter: NoteExporter = DocxExporter(),
    private val pdfExporter: NoteExporter = PdfExporter()
) {

    fun export(note: Note, format: ExportFormat, outputStream: OutputStream) {
        val exporter = when (format) {
            ExportFormat.PDF -> pdfExporter
            ExportFormat.DOCX -> docxExporter
            ExportFormat.MARKDOWN -> markdownExporter
        }
        exporter.export(note, outputStream)
    }

    companion object {
        fun sanitizeFileName(rawTitle: String): String {
            val sanitized = rawTitle
                .trim()
                .replace(Regex("""[\\/:*?"<>|]"""), "-")
                .replace(Regex("""\s+"""), "_")
                .trim { it == '.' || it == '-' || it == '_' }
            return if (sanitized.isBlank()) "scratchpad" else sanitized
        }
    }
}
