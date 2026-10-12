package com.notedown.markpad.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.notedown.markpad.ui.components.EditorPane
import com.notedown.markpad.ui.components.ExportNoteDialog
import com.notedown.markpad.ui.components.PreviewPane
import com.notedown.markpad.ui.components.SettingsDialog
import com.notedown.markpad.ui.components.SidebarPane
import com.notedown.markpad.ui.components.StatusFooter
import com.notedown.markpad.ui.shortcuts.handleMarkPadShortcuts
import com.notedown.markpad.viewmodel.NoteUiState
import com.notedown.markpad.viewmodel.NoteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkPadScreen(
    viewModel: NoteViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    when (val currentState = state) {
        is NoteUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is NoteUiState.Ready -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .handleMarkPadShortcuts(
                        onNewNote = viewModel::createNewNote,
                        onSave = viewModel::triggerSave,
                        onTogglePreview = viewModel::togglePreview,
                        onExport = viewModel::openExportDialog,
                        onOpenSettings = viewModel::openSettingsDialog
                    )
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = "MarkPad — Markdown Notes Manager",
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    actions = {
                        IconButton(onClick = viewModel::triggerSave) {
                            Icon(Icons.Default.Done, contentDescription = "Save (Ctrl+S)")
                        }
                        IconButton(onClick = viewModel::openExportDialog) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Export Note (Ctrl+E)")
                        }
                        IconButton(onClick = viewModel::togglePreview) {
                            Icon(
                                if (currentState.isPreviewVisible) Icons.Default.Visibility
                                else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle Preview (Ctrl+P)"
                            )
                        }
                        IconButton(onClick = viewModel::openSettingsDialog) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings (Ctrl+,)")
                        }
                    }
                )

                // 3-Pane Horizontal Layout
                Row(modifier = Modifier.weight(1f)) {
                    // Left Pane: Sidebar
                    SidebarPane(
                        notes = currentState.filteredNotes,
                        selectedNoteId = currentState.selectedNoteId,
                        searchQuery = currentState.searchQuery,
                        onQueryChanged = viewModel::updateSearchQuery,
                        onNoteSelected = viewModel::selectNote,
                        onNewNote = viewModel::createNewNote,
                        onDeleteNote = viewModel::deleteNote,
                        modifier = Modifier.width(280.dp)
                    )

                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Middle Pane: Editor
                    EditorPane(
                        note = currentState.selectedNote,
                        initialCursorPosition = currentState.cursorPosition,
                        editorFont = currentState.editorFont,
                        onContentChanged = viewModel::onContentChanged,
                        onCursorPositionChanged = viewModel::onCursorPositionChanged,
                        modifier = Modifier.weight(1f)
                    )

                    // Right Pane: Preview (Collapsible)
                    if (currentState.isPreviewVisible) {
                        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        PreviewPane(
                            markdown = currentState.previewMarkdown,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Bottom Status Footer
                StatusFooter(
                    totalNotes = currentState.notes.size,
                    wordCount = currentState.wordCount,
                    charCount = currentState.charCount,
                    saveStatus = currentState.saveStatus,
                    statusMessage = currentState.statusMessage
                )

                // Export Note Dialog
                if (currentState.isExportDialogOpen && currentState.selectedNote != null) {
                    val context = LocalContext.current
                    ExportNoteDialog(
                        note = currentState.selectedNote!!,
                        onDismiss = viewModel::dismissExportDialog,
                        onExportConfirmed = { format, uri ->
                            viewModel.exportNoteToUri(context, currentState.selectedNote!!, format, uri)
                        }
                    )
                }

                // Aesthetic Settings Dialog
                if (currentState.isSettingsDialogOpen) {
                    SettingsDialog(
                        currentTheme = currentState.appTheme,
                        currentEditorFont = currentState.editorFont,
                        currentUiFont = currentState.uiFont,
                        onThemeSelected = viewModel::setAppTheme,
                        onEditorFontSelected = viewModel::setEditorFont,
                        onUiFontSelected = viewModel::setUiFont,
                        onDismiss = viewModel::dismissSettingsDialog
                    )
                }
            }
        }
    }
}
