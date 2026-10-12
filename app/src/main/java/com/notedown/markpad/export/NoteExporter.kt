package com.notedown.markpad.export

import com.notedown.markpad.model.Note
import java.io.OutputStream

interface NoteExporter {
    fun export(note: Note, outputStream: OutputStream)
}
