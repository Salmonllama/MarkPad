package com.notedown.markpad.data

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
        assertEquals(com.notedown.markpad.model.AppTheme.DARK, repo.getAppTheme())
        assertEquals(com.notedown.markpad.model.AppFont.MONOSPACE, repo.getEditorFont())
        assertEquals(com.notedown.markpad.model.AppFont.SANS_SERIF, repo.getUiFont())
    }

    @Test
    fun inMemoryRepository_persistsAndRetrievesTheme() {
        val repo = InMemoryUserPreferencesRepository()
        repo.setAppTheme(com.notedown.markpad.model.AppTheme.PAPERGLOW)
        assertEquals(com.notedown.markpad.model.AppTheme.PAPERGLOW, repo.getAppTheme())

        repo.setAppTheme(com.notedown.markpad.model.AppTheme.OLED)
        assertEquals(com.notedown.markpad.model.AppTheme.OLED, repo.getAppTheme())

        repo.setAppTheme(com.notedown.markpad.model.AppTheme.LIGHT)
        assertEquals(com.notedown.markpad.model.AppTheme.LIGHT, repo.getAppTheme())
    }

    @Test
    fun inMemoryRepository_persistsAndRetrievesEditorFontAndUiFont() {
        val repo = InMemoryUserPreferencesRepository()
        repo.setEditorFont(com.notedown.markpad.model.AppFont.SERIF)
        repo.setUiFont(com.notedown.markpad.model.AppFont.MONOSPACE)

        assertEquals(com.notedown.markpad.model.AppFont.SERIF, repo.getEditorFont())
        assertEquals(com.notedown.markpad.model.AppFont.MONOSPACE, repo.getUiFont())
    }
}
