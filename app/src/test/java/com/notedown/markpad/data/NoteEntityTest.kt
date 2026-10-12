package com.notedown.markpad.data

import com.notedown.markpad.data.local.NoteEntity
import com.notedown.markpad.model.Note
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteEntityTest {

    @Test
    fun toDomain_mapsAllFieldsCorrectly() {
        val entity = NoteEntity(
            id = "test-id",
            title = "Test Title",
            content = "# Content",
            updatedAt = 123456789L
        )

        val domain = entity.toDomain()

        assertEquals("test-id", domain.id)
        assertEquals("Test Title", domain.title)
        assertEquals("# Content", domain.content)
        assertEquals(123456789L, domain.updatedAt)
    }

    @Test
    fun fromDomain_mapsAllFieldsCorrectly() {
        val note = Note(
            id = "note-42",
            title = "Architecture Notes",
            content = "## Details",
            updatedAt = 987654321L
        )

        val entity = NoteEntity.fromDomain(note)

        assertEquals("note-42", entity.id)
        assertEquals("Architecture Notes", entity.title)
        assertEquals("## Details", entity.content)
        assertEquals(987654321L, entity.updatedAt)
    }
}
