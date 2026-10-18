package io.salmonllama.MarkPad.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.salmonllama.MarkPad.model.FileSource
import io.salmonllama.MarkPad.model.Note
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SidebarPane(
    notes: List<Note>,
    selectedNoteId: String?,
    searchQuery: String,
    onQueryChanged: (String) -> Unit,
    onNoteSelected: (String) -> Unit,
    onNewNote: () -> Unit,
    onOpenFile: () -> Unit = {},
    onDeleteNote: (String) -> Unit = {},
    canPinMore: Boolean = true,
    onTogglePinNote: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var contextMenuNoteId by remember { mutableStateOf<String?>(null) }
    var isConfirmingDelete by remember { mutableStateOf(false) }

    val pinnedNotes = remember(notes) { notes.filter { it.isPinned } }
    val unpinnedNotes = remember(notes) { notes.filterNot { it.isPinned } }

    Surface(
        modifier = modifier.fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChanged,
                placeholder = { Text("Search scratchpads...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(pinnedNotes, key = { it.id }) { note ->
                    NoteSidebarCard(
                        note = note,
                        isSelected = note.id == selectedNoteId,
                        isMenuExpanded = contextMenuNoteId == note.id,
                        isConfirmingDelete = isConfirmingDelete,
                        canPinMore = canPinMore,
                        onSelect = { onNoteSelected(note.id) },
                        onOpenContextMenu = {
                            contextMenuNoteId = note.id
                            isConfirmingDelete = false
                        },
                        onDismissContextMenu = {
                            contextMenuNoteId = null
                            isConfirmingDelete = false
                        },
                        onTogglePin = {
                            val targetId = note.id
                            contextMenuNoteId = null
                            isConfirmingDelete = false
                            onTogglePinNote(targetId)
                        },
                        onRequestDeleteConfirmation = {
                            isConfirmingDelete = true
                        },
                        onConfirmDelete = {
                            val targetId = note.id
                            contextMenuNoteId = null
                            isConfirmingDelete = false
                            onDeleteNote(targetId)
                        }
                    )
                }

                if (pinnedNotes.isNotEmpty() && unpinnedNotes.isNotEmpty()) {
                    item(key = "pinned-unpinned-divider") {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }

                items(unpinnedNotes, key = { it.id }) { note ->
                    NoteSidebarCard(
                        note = note,
                        isSelected = note.id == selectedNoteId,
                        isMenuExpanded = contextMenuNoteId == note.id,
                        isConfirmingDelete = isConfirmingDelete,
                        canPinMore = canPinMore,
                        onSelect = { onNoteSelected(note.id) },
                        onOpenContextMenu = {
                            contextMenuNoteId = note.id
                            isConfirmingDelete = false
                        },
                        onDismissContextMenu = {
                            contextMenuNoteId = null
                            isConfirmingDelete = false
                        },
                        onTogglePin = {
                            val targetId = note.id
                            contextMenuNoteId = null
                            isConfirmingDelete = false
                            onTogglePinNote(targetId)
                        },
                        onRequestDeleteConfirmation = {
                            isConfirmingDelete = true
                        },
                        onConfirmDelete = {
                            val targetId = note.id
                            contextMenuNoteId = null
                            isConfirmingDelete = false
                            onDeleteNote(targetId)
                        }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onOpenFile,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = "Open Markdown File",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open", fontWeight = FontWeight.Medium, maxLines = 1)
                }

                FloatingActionButton(
                    onClick = onNewNote,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New Scratchpad")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun NoteSidebarCard(
    note: Note,
    isSelected: Boolean,
    isMenuExpanded: Boolean,
    isConfirmingDelete: Boolean,
    canPinMore: Boolean,
    onSelect: () -> Unit,
    onOpenContextMenu: () -> Unit,
    onDismissContextMenu: () -> Unit,
    onTogglePin: () -> Unit,
    onRequestDeleteConfirmation: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    val dateFormatted = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(note.updatedAt))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .pointerInput(note.id) {
                awaitEachGesture {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                        event.changes.forEach { it.consume() }
                        onOpenContextMenu()
                    }
                }
            }
            .combinedClickable(
                onClick = onSelect,
                onLongClick = onOpenContextMenu
            )
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = note.title.ifBlank { "Untitled Note" },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                           else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (note.isPinned) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pinned note",
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                               else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (note.isExternalFile) {
                    FileSourceBadge(source = note.fileSource)
                }
                Text(
                    text = dateFormatted,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        DropdownMenu(
            expanded = isMenuExpanded,
            onDismissRequest = onDismissContextMenu
        ) {
            val actionLabel = if (note.isExternalFile) "Close Linked File" else "Delete Note"
            val confirmLabel = if (note.isExternalFile) "Confirm Unlink" else "Confirm Delete"
            val actionIcon = if (note.isExternalFile) Icons.Default.LinkOff else Icons.Default.DeleteOutline

            if (!isConfirmingDelete) {
                if (note.isPinned) {
                    DropdownMenuItem(
                        text = { Text("Unpin Note") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.PushPin,
                                contentDescription = "Unpin Note"
                            )
                        },
                        onClick = onTogglePin
                    )
                } else if (canPinMore) {
                    DropdownMenuItem(
                        text = { Text("Pin to Top") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.PushPin,
                                contentDescription = "Pin to Top"
                            )
                        },
                        onClick = onTogglePin
                    )
                } else {
                    val limitTooltipText = "Maximum of ${Note.MAX_PINNED_NOTES} pinned notes allowed"
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = {
                            PlainTooltip {
                                Text(limitTooltipText)
                            }
                        },
                        state = rememberTooltipState()
                    ) {
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text("Pin to Top")
                                    Text(
                                        text = limitTooltipText,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.PushPin,
                                    contentDescription = limitTooltipText
                                )
                            },
                            enabled = false,
                            onClick = {}
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                DropdownMenuItem(
                    text = {
                        Text(
                            text = actionLabel,
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    leadingIcon = {
                        Icon(
                            actionIcon,
                            contentDescription = actionLabel,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = onRequestDeleteConfirmation
                )
            } else {
                DropdownMenuItem(
                    text = {
                        val prompt = if (note.isExternalFile) {
                            "Close \"${note.title.ifBlank { "Untitled Note" }}\" in MarkPad? (Original file stays intact)"
                        } else {
                            "Delete \"${note.title.ifBlank { "Untitled Note" }}\"?"
                        }
                        Text(
                            text = prompt,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    enabled = false,
                    onClick = {}
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                DropdownMenuItem(
                    text = {
                        Text(
                            text = confirmLabel,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            if (note.isExternalFile) Icons.Default.LinkOff else Icons.Default.Delete,
                            contentDescription = confirmLabel,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = onConfirmDelete
                )
                DropdownMenuItem(
                    text = { Text("Cancel") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cancel"
                        )
                    },
                    onClick = onDismissContextMenu
                )
            }
        }
    }
}

@Composable
private fun FileSourceBadge(
    source: FileSource,
    modifier: Modifier = Modifier
) {
    val icon = when (source) {
        FileSource.LOCAL_COMPUTER -> Icons.Default.Computer
        FileSource.LINUX_ENVIRONMENT -> Icons.Default.Terminal
        FileSource.GOOGLE_DRIVE -> Icons.Default.CloudDownload
        FileSource.INTERNAL -> Icons.Default.FolderOpen
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = source.displayName,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = source.shortBadge,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
