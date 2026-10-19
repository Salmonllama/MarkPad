package io.salmonllama.MarkPad.viewmodel

import io.salmonllama.MarkPad.model.Note

sealed interface SaveStatus {
    data object Saved : SaveStatus
    data object Saving : SaveStatus
    data object UnsavedChanges : SaveStatus
}

sealed interface NoteUiState {
    data object Loading : NoteUiState

    data class Ready(
        val notes: List<Note>,
        val selectedNoteId: String?,
        val previewMarkdown: String = "",
        val searchQuery: String = "",
        val isPreviewVisible: Boolean = true,
        val isEditorVisible: Boolean = true,
        val saveStatus: SaveStatus = SaveStatus.Saved,
        val statusMessage: String? = null,
        val isExportDialogOpen: Boolean = false,
        val cursorPosition: Int = 0,
        val appTheme: io.salmonllama.MarkPad.model.AppTheme = io.salmonllama.MarkPad.model.AppTheme.DARK,
        val editorFont: io.salmonllama.MarkPad.model.AppFont = io.salmonllama.MarkPad.model.AppFont.MONOSPACE,
        val uiFont: io.salmonllama.MarkPad.model.AppFont = io.salmonllama.MarkPad.model.AppFont.SANS_SERIF,
        val isSettingsDialogOpen: Boolean = false
    ) : NoteUiState {
        val selectedNote: Note?
            get() = notes.find { it.id == selectedNoteId } ?: notes.firstOrNull()

        val pinnedNotesCount: Int
            get() = notes.count { it.isPinned }

        val canPinMore: Boolean
            get() = pinnedNotesCount < Note.MAX_PINNED_NOTES

        val filteredNotes: List<Note>
            get() = if (searchQuery.isBlank()) {
                notes
            } else {
                notes.filter {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                    it.content.contains(searchQuery, ignoreCase = true) ||
                    (it.sourceFileName?.contains(searchQuery, ignoreCase = true) == true)
                }
            }

        val wordCount: Int
            get() = selectedNote?.content
                ?.split(Regex("\\s+"))
                ?.count { it.isNotBlank() } ?: 0

        val charCount: Int
            get() = selectedNote?.content?.length ?: 0

        private val taskItems: List<io.salmonllama.MarkPad.ui.markdown.MarkdownNode.TaskItem>
            get() = selectedNote?.content
                ?.let { io.salmonllama.MarkPad.ui.markdown.MarkdownParser.parse(it) }
                ?.filterIsInstance<io.salmonllama.MarkPad.ui.markdown.MarkdownNode.TaskItem>()
                .orEmpty()

        val totalTaskCount: Int
            get() = taskItems.size

        val completedTaskCount: Int
            get() = taskItems.count { it.isChecked }
    }
}
