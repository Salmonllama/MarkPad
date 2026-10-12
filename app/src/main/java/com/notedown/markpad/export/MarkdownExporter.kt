package com.notedown.markpad.export

import com.notedown.markpad.model.Note
import java.io.OutputStream
import java.nio.charset.StandardCharsets

class MarkdownExporter : NoteExporter {
    override fun export(note: Note, outputStream: OutputStream) {
        outputStream.write(note.content.toByteArray(StandardCharsets.UTF_8))
        outputStream.flush()
    }
}
