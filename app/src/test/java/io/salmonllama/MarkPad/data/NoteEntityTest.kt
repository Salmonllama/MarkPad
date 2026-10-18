package io.salmonllama.MarkPad.data

import io.salmonllama.MarkPad.data.local.NoteEntity
import io.salmonllama.MarkPad.model.Note
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
        assertEquals(null, entity.sourceUri)
        assertEquals("INTERNAL", entity.fileSource)
        assertEquals(null, entity.sourceFileName)
    }

    @Test
    fun externalFileFields_roundTripBetweenDomainAndEntity() {
        val note = Note(
            id = "ext-1",
            title = "Linux Setup Guide",
            content = "# Linux VM\n\nDetails here.",
            updatedAt = 555666777L,
            sourceUri = "content://com.android.terminal.documents/document/root%3Ahome%2Fdroid%2Fguide.md",
            fileSource = io.salmonllama.MarkPad.model.FileSource.LINUX_ENVIRONMENT,
            sourceFileName = "guide.md"
        )

        val entity = NoteEntity.fromDomain(note)
        assertEquals(note.sourceUri, entity.sourceUri)
        assertEquals("LINUX_ENVIRONMENT", entity.fileSource)
        assertEquals("guide.md", entity.sourceFileName)

        val restored = entity.toDomain()
        assertEquals(note, restored)
        org.junit.Assert.assertTrue(restored.isExternalFile)
    }

    @Test
    fun fileSource_fromUriAuthority_classifiesSourcesCorrectly() {
        assertEquals(
            io.salmonllama.MarkPad.model.FileSource.GOOGLE_DRIVE,
            io.salmonllama.MarkPad.model.FileSource.fromUriAuthority(
                authority = "com.google.android.apps.docs.storage",
                path = "/document/acc=1;doc=42"
            )
        )
        assertEquals(
            io.salmonllama.MarkPad.model.FileSource.LINUX_ENVIRONMENT,
            io.salmonllama.MarkPad.model.FileSource.fromUriAuthority(
                authority = "com.android.terminal.documents",
                path = "/document/home/droid/notes.md"
            )
        )
        assertEquals(
            io.salmonllama.MarkPad.model.FileSource.LINUX_ENVIRONMENT,
            io.salmonllama.MarkPad.model.FileSource.fromUriAuthority(
                authority = "com.android.externalstorage.documents",
                path = "/document/primary:Documents/source/project/README.md"
            )
        )
        assertEquals(
            io.salmonllama.MarkPad.model.FileSource.LOCAL_COMPUTER,
            io.salmonllama.MarkPad.model.FileSource.fromUriAuthority(
                authority = "com.android.externalstorage.documents",
                path = "/document/primary:Documents/notes.md"
            )
        )
    }
}
