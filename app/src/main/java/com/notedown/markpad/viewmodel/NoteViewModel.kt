package com.notedown.markpad.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notedown.markpad.data.InMemoryNoteRepository
import com.notedown.markpad.data.InMemoryUserPreferencesRepository
import com.notedown.markpad.data.NoteRepository
import com.notedown.markpad.data.SeedNotes
import com.notedown.markpad.data.UserPreferencesRepository
import com.notedown.markpad.export.ExportFormat
import com.notedown.markpad.export.ExportManager
import com.notedown.markpad.model.AppFont
import com.notedown.markpad.model.AppTheme
import com.notedown.markpad.model.Note
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.OutputStream

private data class WorkspaceControlState(
    val query: String,
    val isPreview: Boolean,
    val saveState: SaveStatus,
    val message: String?,
    val isExportDialogOpen: Boolean
)

private data class ActiveNoteDraft(
    val id: String,
    val title: String,
    val content: String
)

@OptIn(FlowPreview::class)
class NoteViewModel(
    private val repository: NoteRepository = InMemoryNoteRepository(),
    private val preferencesRepository: UserPreferencesRepository = InMemoryUserPreferencesRepository(),
    private val exportManager: ExportManager = ExportManager(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val defaultInitialNoteId = "meeting-retro"
    private val savedInitialNoteId = preferencesRepository.getLastOpenedNoteId()
    private val initialNoteId = savedInitialNoteId ?: defaultInitialNoteId
    private val _selectedNoteId = MutableStateFlow<String?>(initialNoteId)
    private val _cursorPosition = MutableStateFlow(preferencesRepository.getLastCursorPosition(initialNoteId))
    private val _searchQuery = MutableStateFlow("")
    private val _isPreviewVisible = MutableStateFlow(true)
    private val _saveStatus = MutableStateFlow<SaveStatus>(SaveStatus.Saved)
    private val _statusNotification = MutableStateFlow<String?>(null)
    private val _isExportDialogOpen = MutableStateFlow(false)
    private val _appTheme = MutableStateFlow(preferencesRepository.getAppTheme())
    private val _editorFont = MutableStateFlow(preferencesRepository.getEditorFont())
    private val _uiFont = MutableStateFlow(preferencesRepository.getUiFont())
    private val _isSettingsDialogOpen = MutableStateFlow(false)

    // Active in-memory draft to provide immediate UI updates without waiting for Room disk I/O
    private val _activeDraft = MutableStateFlow<ActiveNoteDraft?>(null)
    private val _saveDebounceFlow = MutableStateFlow<ActiveNoteDraft?>(null)

    // Debounced Markdown preview buffer
    private val _previewMarkdown = MutableStateFlow(
        SeedNotes.items.find { it.id == initialNoteId }?.content ?: ""
    )
    private val _previewDebounceFlow = MutableStateFlow<String?>(null)

    private val _controlState = combine(
        _searchQuery,
        _isPreviewVisible,
        _saveStatus,
        _statusNotification,
        _isExportDialogOpen
    ) { query, isPreview, saveState, message, isExportDialogOpen ->
        WorkspaceControlState(query, isPreview, saveState, message, isExportDialogOpen)
    }

    private data class EditorControlState(
        val cursorPosition: Int,
        val previewMarkdown: String,
        val activeDraft: ActiveNoteDraft?
    )

    private val _editorState = combine(
        _cursorPosition,
        _previewMarkdown,
        _activeDraft
    ) { cursor, preview, draft ->
        EditorControlState(cursor, preview, draft)
    }

    private data class SettingsControlState(
        val appTheme: AppTheme,
        val editorFont: AppFont,
        val uiFont: AppFont,
        val isSettingsDialogOpen: Boolean
    )

    private val _settingsState = combine(
        _appTheme,
        _editorFont,
        _uiFont,
        _isSettingsDialogOpen
    ) { theme, editorFont, uiFont, isSettingsDialogOpen ->
        SettingsControlState(theme, editorFont, uiFont, isSettingsDialogOpen)
    }

    val uiState: StateFlow<NoteUiState> = combine(
        repository.getNotes(),
        _selectedNoteId,
        _controlState,
        _editorState,
        _settingsState
    ) { notes, selectedId, controls, editor, settings ->
        val resolvedId = when {
            selectedId != null && notes.any { it.id == selectedId } -> selectedId
            notes.isNotEmpty() -> notes.first().id
            else -> null
        }
        val draft = editor.activeDraft
        val effectiveNotes = if (draft != null) {
            notes.map { note ->
                if (note.id == draft.id) {
                    note.copy(title = draft.title, content = draft.content)
                } else {
                    note
                }
            }
        } else {
            notes
        }
        val safeCursor = if (resolvedId != null) {
            val contentLen = effectiveNotes.find { it.id == resolvedId }?.content?.length ?: 0
            val pos = if (resolvedId == selectedId) editor.cursorPosition else preferencesRepository.getLastCursorPosition(resolvedId)
            pos.coerceIn(0, contentLen)
        } else 0

        NoteUiState.Ready(
            notes = effectiveNotes,
            selectedNoteId = resolvedId,
            previewMarkdown = editor.previewMarkdown,
            searchQuery = controls.query,
            isPreviewVisible = controls.isPreview,
            saveStatus = controls.saveState,
            statusMessage = controls.message,
            isExportDialogOpen = controls.isExportDialogOpen,
            cursorPosition = safeCursor,
            appTheme = settings.appTheme,
            editorFont = settings.editorFont,
            uiFont = settings.uiFont,
            isSettingsDialogOpen = settings.isSettingsDialogOpen
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NoteUiState.Loading
    )

    init {
        // Debounce specifically for the Markdown preview parsing pipeline and SaveStatus confirmation
        viewModelScope.launch {
            _previewDebounceFlow
                .debounce(150L)
                .distinctUntilChanged()
                .collect { debouncedText ->
                    if (debouncedText != null) {
                        _previewMarkdown.value = debouncedText
                        _saveStatus.value = SaveStatus.Saved
                    }
                }
        }

        // Debounce repository persistence to prevent flooding SQLite/Room on rapid keystrokes
        viewModelScope.launch {
            _saveDebounceFlow
                .debounce(150L)
                .distinctUntilChanged()
                .collect { draft ->
                    if (draft != null) {
                        saveDraftToRepository(draft)
                    }
                }
        }

        // Initialize preview buffer once notes arrive if not already matching the active note
        viewModelScope.launch {
            repository.getNotes().collect { notes ->
                if (_previewDebounceFlow.value == null) {
                    val activeId = _selectedNoteId.value
                    val currentNote = notes.find { it.id == activeId } ?: notes.firstOrNull()
                    if (currentNote != null && _previewMarkdown.value.isEmpty()) {
                        _previewMarkdown.value = currentNote.content
                    }
                }
            }
        }
    }

    private suspend fun saveDraftToRepository(draft: ActiveNoteDraft) {
        val currentNotes = (uiState.value as? NoteUiState.Ready)?.notes ?: emptyList()
        val existing = currentNotes.find { it.id == draft.id }
        if (existing != null) {
            repository.saveNote(existing.copy(title = draft.title, content = draft.content))
        }
    }

    private suspend fun flushPendingSave() {
        val draft = _activeDraft.value ?: return
        saveDraftToRepository(draft)
        _saveStatus.value = SaveStatus.Saved
    }

    fun onContentChanged(newContent: String) {
        val currentReady = uiState.value as? NoteUiState.Ready ?: return
        val currentId = currentReady.selectedNoteId ?: return

        preferencesRepository.setLastOpenedNoteId(currentId)

        // 1. Mark status as saving
        _saveStatus.value = SaveStatus.Saving

        val derivedTitle = newContent.lines()
            .firstOrNull { it.isNotBlank() }
            ?.removePrefix("#")
            ?.trim()
            ?: "Untitled Scratchpad"

        val draft = ActiveNoteDraft(currentId, derivedTitle, newContent)

        // 2. Persist to active in-memory draft immediately so word/char counts and titles update with 0ms latency
        _activeDraft.value = draft

        // 3. Send to debounced persistence and preview pipeline
        _saveDebounceFlow.value = draft
        _previewDebounceFlow.value = newContent
    }

    fun selectNote(id: String) {
        val previousDraft = _activeDraft.value
        if (previousDraft != null && previousDraft.id != id) {
            viewModelScope.launch {
                saveDraftToRepository(previousDraft)
            }
        }
        _activeDraft.value = null
        _saveDebounceFlow.value = null

        _selectedNoteId.value = id
        preferencesRepository.setLastOpenedNoteId(id)
        val restoredCursor = preferencesRepository.getLastCursorPosition(id)
        _cursorPosition.value = restoredCursor

        val currentNotes = (uiState.value as? NoteUiState.Ready)?.notes ?: emptyList()
        val target = currentNotes.find { it.id == id }
        val noteContent = target?.content ?: ""
        _previewMarkdown.value = noteContent
        _previewDebounceFlow.value = null
    }

    fun onCursorPositionChanged(position: Int) {
        val currentReady = uiState.value as? NoteUiState.Ready ?: return
        val currentId = currentReady.selectedNoteId ?: return
        _cursorPosition.value = position
        preferencesRepository.setLastCursorPosition(currentId, position)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun togglePreview() {
        _isPreviewVisible.update { !it }
    }

    fun createNewNote() {
        viewModelScope.launch {
            flushPendingSave()
            _activeDraft.value = null
            _saveDebounceFlow.value = null

            val newNote = repository.createNote(
                title = "New Scratchpad",
                content = "# New Scratchpad\n\nStart typing markdown here..."
            )
            _selectedNoteId.value = newNote.id
            preferencesRepository.setLastOpenedNoteId(newNote.id)
            _cursorPosition.value = 0
            preferencesRepository.setLastCursorPosition(newNote.id, 0)
            _previewMarkdown.value = newNote.content
            _previewDebounceFlow.value = null
            triggerStatusNotification("Created new scratchpad")
        }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch {
            if (_activeDraft.value?.id == id) {
                _activeDraft.value = null
                _saveDebounceFlow.value = null
            } else {
                flushPendingSave()
            }

            val currentNotes = (uiState.value as? NoteUiState.Ready)?.notes ?: emptyList()
            val noteToDelete = currentNotes.find { it.id == id }
            val remainingNotes = currentNotes.filterNot { it.id == id }

            if (_selectedNoteId.value == id) {
                val nextNote = remainingNotes.firstOrNull()
                if (nextNote != null) {
                    _selectedNoteId.value = nextNote.id
                    preferencesRepository.setLastOpenedNoteId(nextNote.id)
                    val restoredCursor = preferencesRepository.getLastCursorPosition(nextNote.id)
                    _cursorPosition.value = restoredCursor
                    _previewMarkdown.value = nextNote.content
                    _previewDebounceFlow.value = null
                } else {
                    _selectedNoteId.value = null
                    preferencesRepository.setLastOpenedNoteId("")
                    _cursorPosition.value = 0
                    _previewMarkdown.value = ""
                    _previewDebounceFlow.value = null
                }
            }

            repository.deleteNote(id)
            val title = noteToDelete?.title?.ifBlank { "Untitled Note" } ?: "Note"
            triggerStatusNotification("Deleted \"$title\"")
        }
    }

    fun triggerSave() {
        viewModelScope.launch {
            _saveStatus.value = SaveStatus.Saving
            flushPendingSave()
            delay(100)
            _saveStatus.value = SaveStatus.Saved
            triggerStatusNotification("Note saved successfully")
        }
    }

    fun openExportDialog() {
        _isExportDialogOpen.value = true
    }

    fun dismissExportDialog() {
        _isExportDialogOpen.value = false
    }

    fun openSettingsDialog() {
        _isSettingsDialogOpen.value = true
    }

    fun dismissSettingsDialog() {
        _isSettingsDialogOpen.value = false
    }

    fun setAppTheme(theme: AppTheme) {
        _appTheme.value = theme
        preferencesRepository.setAppTheme(theme)
    }

    fun setEditorFont(font: AppFont) {
        _editorFont.value = font
        preferencesRepository.setEditorFont(font)
    }

    fun setUiFont(font: AppFont) {
        _uiFont.value = font
        preferencesRepository.setUiFont(font)
    }

    fun exportNoteToStream(
        note: Note,
        format: ExportFormat,
        outputStream: OutputStream
    ) {
        viewModelScope.launch(ioDispatcher) {
            try {
                flushPendingSave()
                val draft = _activeDraft.value
                val noteToExport = if (draft != null && draft.id == note.id) {
                    note.copy(title = draft.title, content = draft.content)
                } else note
                exportManager.export(noteToExport, format, outputStream)
                _isExportDialogOpen.value = false
                triggerStatusNotification("Exported '${noteToExport.title}' as ${format.displayName} successfully")
            } catch (e: Exception) {
                triggerStatusNotification("Failed to export: ${e.message ?: "Unknown error"}")
            }
        }
    }

    fun exportNoteToUri(
        context: Context,
        note: Note,
        format: ExportFormat,
        uri: Uri
    ) {
        viewModelScope.launch(ioDispatcher) {
            try {
                flushPendingSave()
                val draft = _activeDraft.value
                val noteToExport = if (draft != null && draft.id == note.id) {
                    note.copy(title = draft.title, content = draft.content)
                } else note
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    exportManager.export(noteToExport, format, outputStream)
                } ?: throw IllegalStateException("Could not open output stream for destination")
                _isExportDialogOpen.value = false
                triggerStatusNotification("Exported '${noteToExport.title}' as ${format.displayName} successfully")
            } catch (e: Exception) {
                triggerStatusNotification("Failed to export: ${e.message ?: "Unknown error"}")
            }
        }
    }

    private fun triggerStatusNotification(message: String) {
        viewModelScope.launch {
            _statusNotification.value = message
            delay(2500)
            _statusNotification.value = null
        }
    }
}
