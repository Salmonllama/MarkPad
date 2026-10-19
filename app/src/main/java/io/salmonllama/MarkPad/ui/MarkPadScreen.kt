package io.salmonllama.MarkPad.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderOpen
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.salmonllama.MarkPad.R
import io.salmonllama.MarkPad.ui.components.EditorPane
import io.salmonllama.MarkPad.ui.components.ExportNoteDialog
import io.salmonllama.MarkPad.ui.components.OpenFileDialog
import io.salmonllama.MarkPad.ui.components.PreviewPane
import io.salmonllama.MarkPad.ui.components.SettingsDialog
import io.salmonllama.MarkPad.ui.components.SidebarPane
import io.salmonllama.MarkPad.ui.components.StatusFooter
import io.salmonllama.MarkPad.ui.shortcuts.handleMarkPadShortcuts
import io.salmonllama.MarkPad.viewmodel.NoteUiState
import io.salmonllama.MarkPad.viewmodel.NoteViewModel

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
                        onToggleFullPreview = viewModel::toggleFullPreview,
                        onOpenFile = viewModel::openFileDialog,
                        onExport = viewModel::openExportDialog,
                        onOpenSettings = viewModel::openSettingsDialog
                    )
            ) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.app_logo),
                                contentDescription = "MarkPad Logo",
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "MarkPad — Markdown Notes Manager",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    actions = {
                        IconButton(onClick = viewModel::openFileDialog) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "Open File (Ctrl+O)")
                        }
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
                        IconButton(onClick = viewModel::toggleFullPreview) {
                            Icon(
                                Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = "Full Preview (Ctrl+R)",
                                tint = if (!currentState.isEditorVisible && currentState.isPreviewVisible) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
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
                        onOpenFile = viewModel::openFileDialog,
                        onDeleteNote = viewModel::deleteNote,
                        canPinMore = currentState.canPinMore,
                        onTogglePinNote = viewModel::togglePinNote,
                        modifier = Modifier.width(280.dp)
                    )

                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Middle Pane: Editor
                    if (currentState.isEditorVisible) {
                        EditorPane(
                            note = currentState.selectedNote,
                            initialCursorPosition = currentState.cursorPosition,
                            editorFont = currentState.editorFont,
                            onContentChanged = viewModel::onContentChanged,
                            onCursorPositionChanged = viewModel::onCursorPositionChanged,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Right Pane: Preview (Collapsible)
                    if (currentState.isPreviewVisible) {
                        if (currentState.isEditorVisible) {
                            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }

                        PreviewPane(
                            markdown = currentState.previewMarkdown,
                            onTaskToggle = viewModel::toggleTaskItem,
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
                    statusMessage = currentState.statusMessage,
                    activeNote = currentState.selectedNote,
                    completedTasks = currentState.completedTaskCount,
                    totalTasks = currentState.totalTaskCount
                )

                // Open Markdown File Dialog
                if (currentState.isOpenFileDialogOpen) {
                    OpenFileDialog(
                        onDismiss = viewModel::dismissOpenFileDialog,
                        onFileSelected = { uri, source ->
                            viewModel.openExternalFile(uri.toString(), source)
                        }
                    )
                }

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
