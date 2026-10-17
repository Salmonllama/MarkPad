package com.notedown.markpad.ui.components

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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.notedown.markpad.model.FileSource
import com.notedown.markpad.model.Note
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
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
    modifier: Modifier = Modifier
) {
    var contextMenuNoteId by remember { mutableStateOf<String?>(null) }
    var isConfirmingDelete by remember { mutableStateOf(false) }

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
                items(notes, key = { it.id }) { note ->
                    val isSelected = note.id == selectedNoteId
                    val isMenuExpanded = contextMenuNoteId == note.id
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
                                        contextMenuNoteId = note.id
                                        isConfirmingDelete = false
                                    }
                                }
                            }
                            .combinedClickable(
                                onClick = { onNoteSelected(note.id) },
                                onLongClick = {
                                    contextMenuNoteId = note.id
                                    isConfirmingDelete = false
                                }
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = note.title.ifBlank { "Untitled Note" },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                       else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
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
                            onDismissRequest = {
                                contextMenuNoteId = null
                                isConfirmingDelete = false
                            }
                        ) {
                            val actionLabel = if (note.isExternalFile) "Close Linked File" else "Delete Note"
                            val confirmLabel = if (note.isExternalFile) "Confirm Unlink" else "Confirm Delete"
                            val actionIcon = if (note.isExternalFile) Icons.Default.LinkOff else Icons.Default.DeleteOutline

                            if (!isConfirmingDelete) {
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
                                    onClick = {
                                        isConfirmingDelete = true
                                    }
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
                                    onClick = {
                                        val targetId = note.id
                                        contextMenuNoteId = null
                                        isConfirmingDelete = false
                                        onDeleteNote(targetId)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Cancel") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Cancel"
                                        )
                                    },
                                    onClick = {
                                        contextMenuNoteId = null
                                        isConfirmingDelete = false
                                    }
                                )
                            }
                        }
                    }
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
