package io.salmonllama.MarkPad.export

import io.salmonllama.MarkPad.model.Note
import java.io.OutputStream

interface NoteExporter {
    fun export(note: Note, outputStream: OutputStream)
}
