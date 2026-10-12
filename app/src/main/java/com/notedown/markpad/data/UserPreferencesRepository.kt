package com.notedown.markpad.data

import android.content.Context
import android.content.SharedPreferences

interface UserPreferencesRepository {
    fun getLastOpenedNoteId(): String?
    fun setLastOpenedNoteId(noteId: String)
    fun getLastCursorPosition(noteId: String): Int
    fun setLastCursorPosition(noteId: String, position: Int)
    fun getAppTheme(): com.notedown.markpad.model.AppTheme
    fun setAppTheme(theme: com.notedown.markpad.model.AppTheme)
    fun getEditorFont(): com.notedown.markpad.model.AppFont
    fun setEditorFont(font: com.notedown.markpad.model.AppFont)
    fun getUiFont(): com.notedown.markpad.model.AppFont
    fun setUiFont(font: com.notedown.markpad.model.AppFont)
}

class InMemoryUserPreferencesRepository(
    private var lastOpenedNoteId: String? = null,
    private val cursorPositions: MutableMap<String, Int> = mutableMapOf(),
    private var appTheme: com.notedown.markpad.model.AppTheme = com.notedown.markpad.model.AppTheme.DARK,
    private var editorFont: com.notedown.markpad.model.AppFont = com.notedown.markpad.model.AppFont.MONOSPACE,
    private var uiFont: com.notedown.markpad.model.AppFont = com.notedown.markpad.model.AppFont.SANS_SERIF
) : UserPreferencesRepository {
    override fun getLastOpenedNoteId(): String? = lastOpenedNoteId

    override fun setLastOpenedNoteId(noteId: String) {
        lastOpenedNoteId = noteId
    }

    override fun getLastCursorPosition(noteId: String): Int = cursorPositions[noteId] ?: 0

    override fun setLastCursorPosition(noteId: String, position: Int) {
        cursorPositions[noteId] = position
    }

    override fun getAppTheme(): com.notedown.markpad.model.AppTheme = appTheme

    override fun setAppTheme(theme: com.notedown.markpad.model.AppTheme) {
        appTheme = theme
    }

    override fun getEditorFont(): com.notedown.markpad.model.AppFont = editorFont

    override fun setEditorFont(font: com.notedown.markpad.model.AppFont) {
        editorFont = font
    }

    override fun getUiFont(): com.notedown.markpad.model.AppFont = uiFont

    override fun setUiFont(font: com.notedown.markpad.model.AppFont) {
        uiFont = font
    }
}

class SharedPreferencesUserPreferencesRepository(
    context: Context,
    prefsName: String = PREFS_NAME
) : UserPreferencesRepository {
    private val prefs: SharedPreferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    override fun getLastOpenedNoteId(): String? {
        return prefs.getString(KEY_LAST_OPENED_NOTE_ID, null)
    }

    override fun setLastOpenedNoteId(noteId: String) {
        prefs.edit().putString(KEY_LAST_OPENED_NOTE_ID, noteId).apply()
    }

    override fun getLastCursorPosition(noteId: String): Int {
        return prefs.getInt(KEY_CURSOR_PREFIX + noteId, 0)
    }

    override fun setLastCursorPosition(noteId: String, position: Int) {
        prefs.edit().putInt(KEY_CURSOR_PREFIX + noteId, position).apply()
    }

    override fun getAppTheme(): com.notedown.markpad.model.AppTheme {
        val name = prefs.getString(KEY_APP_THEME, null)
        return com.notedown.markpad.model.AppTheme.fromNameOrDefault(name, com.notedown.markpad.model.AppTheme.DARK)
    }

    override fun setAppTheme(theme: com.notedown.markpad.model.AppTheme) {
        prefs.edit().putString(KEY_APP_THEME, theme.name).apply()
    }

    override fun getEditorFont(): com.notedown.markpad.model.AppFont {
        val name = prefs.getString(KEY_EDITOR_FONT, null)
        return com.notedown.markpad.model.AppFont.fromNameOrDefault(name, com.notedown.markpad.model.AppFont.MONOSPACE)
    }

    override fun setEditorFont(font: com.notedown.markpad.model.AppFont) {
        prefs.edit().putString(KEY_EDITOR_FONT, font.name).apply()
    }

    override fun getUiFont(): com.notedown.markpad.model.AppFont {
        val name = prefs.getString(KEY_UI_FONT, null)
        return com.notedown.markpad.model.AppFont.fromNameOrDefault(name, com.notedown.markpad.model.AppFont.SANS_SERIF)
    }

    override fun setUiFont(font: com.notedown.markpad.model.AppFont) {
        prefs.edit().putString(KEY_UI_FONT, font.name).apply()
    }

    companion object {
        private const val PREFS_NAME = "markpad_preferences"
        private const val KEY_LAST_OPENED_NOTE_ID = "last_opened_note_id"
        private const val KEY_CURSOR_PREFIX = "cursor_pos_"
        private const val KEY_APP_THEME = "app_theme"
        private const val KEY_EDITOR_FONT = "editor_font"
        private const val KEY_UI_FONT = "ui_font"
    }
}
