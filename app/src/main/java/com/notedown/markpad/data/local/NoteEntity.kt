package com.notedown.markpad.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.notedown.markpad.model.FileSource
import com.notedown.markpad.model.Note

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    @ColumnInfo(index = true) val updatedAt: Long,
    val sourceUri: String? = null,
    val fileSource: String = FileSource.INTERNAL.name,
    val sourceFileName: String? = null
) {
    fun toDomain(): Note = Note(
        id = id,
        title = title,
        content = content,
        updatedAt = updatedAt,
        sourceUri = sourceUri,
        fileSource = FileSource.fromNameOrDefault(fileSource),
        sourceFileName = sourceFileName
    )

    companion object {
        fun fromDomain(note: Note): NoteEntity = NoteEntity(
            id = note.id,
            title = note.title,
            content = note.content,
            updatedAt = note.updatedAt,
            sourceUri = note.sourceUri,
            fileSource = note.fileSource.name,
            sourceFileName = note.sourceFileName
        )
    }
}
