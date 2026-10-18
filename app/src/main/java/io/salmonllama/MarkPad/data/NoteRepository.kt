package io.salmonllama.MarkPad.data

import io.salmonllama.MarkPad.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

interface NoteRepository {
    fun getNotes(): Flow<List<Note>>
    suspend fun saveNote(note: Note)
    suspend fun createNote(title: String = "Untitled Scratchpad", content: String = ""): Note
    suspend fun deleteNote(id: String)
}

class InMemoryNoteRepository(
    initialNotes: List<Note> = SeedNotes.items
) : NoteRepository {
    private val _notes = MutableStateFlow(initialNotes)

    override fun getNotes(): Flow<List<Note>> = _notes.asStateFlow()

    override suspend fun saveNote(note: Note) {
        _notes.update { list ->
            val index = list.indexOfFirst { it.id == note.id }
            if (index >= 0) {
                list.toMutableList().apply { set(index, note.copy(updatedAt = System.currentTimeMillis())) }
            } else {
                listOf(note) + list
            }
        }
    }

    override suspend fun createNote(title: String, content: String): Note {
        val newNote = Note(
            id = UUID.randomUUID().toString(),
            title = title,
            content = content,
            updatedAt = System.currentTimeMillis()
        )
        _notes.update { listOf(newNote) + it }
        return newNote
    }

    override suspend fun deleteNote(id: String) {
        _notes.update { list -> list.filterNot { it.id == id } }
    }
}
