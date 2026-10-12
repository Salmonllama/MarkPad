package com.notedown.markpad.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.notedown.markpad.model.Note

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    @ColumnInfo(index = true) val updatedAt: Long
) {
    fun toDomain(): Note = Note(
        id = id,
        title = title,
        content = content,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(note: Note): NoteEntity = NoteEntity(
            id = note.id,
            title = note.title,
            content = note.content,
            updatedAt = note.updatedAt
        )
    }
}
