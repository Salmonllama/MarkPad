package io.salmonllama.MarkPad.data

import android.content.Context
import android.content.SharedPreferences

interface UserPreferencesRepository {
    fun getLastOpenedNoteId(): String?
    fun setLastOpenedNoteId(noteId: String)
    fun getLastCursorPosition(noteId: String): Int
    fun setLastCursorPosition(noteId: String, position: Int)
    fun getAppTheme(): io.salmonllama.MarkPad.model.AppTheme
    fun setAppTheme(theme: io.salmonllama.MarkPad.model.AppTheme)
    fun getEditorFont(): io.salmonllama.MarkPad.model.AppFont
    fun setEditorFont(font: io.salmonllama.MarkPad.model.AppFont)
    fun getUiFont(): io.salmonllama.MarkPad.model.AppFont
    fun setUiFont(font: io.salmonllama.MarkPad.model.AppFont)
}

class InMemoryUserPreferencesRepository(
    private var lastOpenedNoteId: String? = null,
    private val cursorPositions: MutableMap<String, Int> = mutableMapOf(),
    private var appTheme: io.salmonllama.MarkPad.model.AppTheme = io.salmonllama.MarkPad.model.AppTheme.DARK,
    private var editorFont: io.salmonllama.MarkPad.model.AppFont = io.salmonllama.MarkPad.model.AppFont.MONOSPACE,
    private var uiFont: io.salmonllama.MarkPad.model.AppFont = io.salmonllama.MarkPad.model.AppFont.SANS_SERIF
) : UserPreferencesRepository {
    override fun getLastOpenedNoteId(): String? = lastOpenedNoteId

    override fun setLastOpenedNoteId(noteId: String) {
        lastOpenedNoteId = noteId
    }

    override fun getLastCursorPosition(noteId: String): Int = cursorPositions[noteId] ?: 0

    override fun setLastCursorPosition(noteId: String, position: Int) {
        cursorPositions[noteId] = position
    }

    override fun getAppTheme(): io.salmonllama.MarkPad.model.AppTheme = appTheme

    override fun setAppTheme(theme: io.salmonllama.MarkPad.model.AppTheme) {
        appTheme = theme
    }

    override fun getEditorFont(): io.salmonllama.MarkPad.model.AppFont = editorFont

    override fun setEditorFont(font: io.salmonllama.MarkPad.model.AppFont) {
        editorFont = font
    }

    override fun getUiFont(): io.salmonllama.MarkPad.model.AppFont = uiFont

    override fun setUiFont(font: io.salmonllama.MarkPad.model.AppFont) {
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

    override fun getAppTheme(): io.salmonllama.MarkPad.model.AppTheme {
        val name = prefs.getString(KEY_APP_THEME, null)
        return io.salmonllama.MarkPad.model.AppTheme.fromNameOrDefault(name, io.salmonllama.MarkPad.model.AppTheme.DARK)
    }

    override fun setAppTheme(theme: io.salmonllama.MarkPad.model.AppTheme) {
        prefs.edit().putString(KEY_APP_THEME, theme.name).apply()
    }

    override fun getEditorFont(): io.salmonllama.MarkPad.model.AppFont {
        val name = prefs.getString(KEY_EDITOR_FONT, null)
        return io.salmonllama.MarkPad.model.AppFont.fromNameOrDefault(name, io.salmonllama.MarkPad.model.AppFont.MONOSPACE)
    }

    override fun setEditorFont(font: io.salmonllama.MarkPad.model.AppFont) {
        prefs.edit().putString(KEY_EDITOR_FONT, font.name).apply()
    }

    override fun getUiFont(): io.salmonllama.MarkPad.model.AppFont {
        val name = prefs.getString(KEY_UI_FONT, null)
        return io.salmonllama.MarkPad.model.AppFont.fromNameOrDefault(name, io.salmonllama.MarkPad.model.AppFont.SANS_SERIF)
    }

    override fun setUiFont(font: io.salmonllama.MarkPad.model.AppFont) {
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
