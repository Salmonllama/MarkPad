package io.salmonllama.MarkPad.export

import io.salmonllama.MarkPad.model.Note
import java.io.OutputStream
import java.nio.charset.StandardCharsets

class MarkdownExporter : NoteExporter {
    override fun export(note: Note, outputStream: OutputStream) {
        outputStream.write(note.content.toByteArray(StandardCharsets.UTF_8))
        outputStream.flush()
    }
}
