package io.salmonllama.MarkPad.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserPreferencesRepositoryTest {

    @Test
    fun inMemoryRepository_defaultValuesAreNullAndZero() {
        val repo = InMemoryUserPreferencesRepository()
        assertNull(repo.getLastOpenedNoteId())
        assertEquals(0, repo.getLastCursorPosition("any-note"))
    }

    @Test
    fun inMemoryRepository_persistsAndRetrievesLastOpenedNoteId() {
        val repo = InMemoryUserPreferencesRepository()
        repo.setLastOpenedNoteId("meeting-retro")
        assertEquals("meeting-retro", repo.getLastOpenedNoteId())

        repo.setLastOpenedNoteId("cli-cheatsheet")
        assertEquals("cli-cheatsheet", repo.getLastOpenedNoteId())
    }

    @Test
    fun inMemoryRepository_persistsAndRetrievesCursorPositionPerNote() {
        val repo = InMemoryUserPreferencesRepository()
        repo.setLastCursorPosition("note-1", 100)
        repo.setLastCursorPosition("note-2", 250)

        assertEquals(100, repo.getLastCursorPosition("note-1"))
        assertEquals(250, repo.getLastCursorPosition("note-2"))
        assertEquals(0, repo.getLastCursorPosition("unknown-note"))
    }

    @Test
    fun inMemoryRepository_defaultThemeAndFontsAreDarkAndMonospaceSans() {
        val repo = InMemoryUserPreferencesRepository()
        assertEquals(io.salmonllama.MarkPad.model.AppTheme.DARK, repo.getAppTheme())
        assertEquals(io.salmonllama.MarkPad.model.AppFont.MONOSPACE, repo.getEditorFont())
        assertEquals(io.salmonllama.MarkPad.model.AppFont.SANS_SERIF, repo.getUiFont())
    }

    @Test
    fun inMemoryRepository_persistsAndRetrievesTheme() {
        val repo = InMemoryUserPreferencesRepository()
        repo.setAppTheme(io.salmonllama.MarkPad.model.AppTheme.PAPERGLOW)
        assertEquals(io.salmonllama.MarkPad.model.AppTheme.PAPERGLOW, repo.getAppTheme())

        repo.setAppTheme(io.salmonllama.MarkPad.model.AppTheme.OLED)
        assertEquals(io.salmonllama.MarkPad.model.AppTheme.OLED, repo.getAppTheme())

        repo.setAppTheme(io.salmonllama.MarkPad.model.AppTheme.LIGHT)
        assertEquals(io.salmonllama.MarkPad.model.AppTheme.LIGHT, repo.getAppTheme())
    }

    @Test
    fun inMemoryRepository_persistsAndRetrievesEditorFontAndUiFont() {
        val repo = InMemoryUserPreferencesRepository()
        repo.setEditorFont(io.salmonllama.MarkPad.model.AppFont.SERIF)
        repo.setUiFont(io.salmonllama.MarkPad.model.AppFont.MONOSPACE)

        assertEquals(io.salmonllama.MarkPad.model.AppFont.SERIF, repo.getEditorFont())
        assertEquals(io.salmonllama.MarkPad.model.AppFont.MONOSPACE, repo.getUiFont())
    }
}
