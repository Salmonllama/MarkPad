package com.notedown.markpad.data

import com.notedown.markpad.data.local.NoteDao
import com.notedown.markpad.data.local.NoteEntity
import com.notedown.markpad.model.Note
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class RoomNoteRepository(
    private val noteDao: NoteDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : NoteRepository {

    override fun getNotes(): Flow<List<Note>> {
        return noteDao.getAllNotes().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveNote(note: Note) = withContext(ioDispatcher) {
        noteDao.upsertNote(NoteEntity.fromDomain(note))
    }

    override suspend fun createNote(title: String, content: String): Note = withContext(ioDispatcher) {
        val newNote = Note(
            id = UUID.randomUUID().toString(),
            title = title,
            content = content,
            updatedAt = System.currentTimeMillis()
        )
        noteDao.upsertNote(NoteEntity.fromDomain(newNote))
        newNote
    }

    override suspend fun deleteNote(id: String) = withContext(ioDispatcher) {
        noteDao.deleteNote(id)
    }
}
