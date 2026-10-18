package io.salmonllama.MarkPad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.salmonllama.MarkPad.model.Note
import io.salmonllama.MarkPad.viewmodel.SaveStatus

@Composable
fun StatusFooter(
    totalNotes: Int,
    wordCount: Int,
    charCount: Int,
    saveStatus: SaveStatus,
    statusMessage: String?,
    activeNote: Note? = null,
    completedTasks: Int = 0,
    totalTasks: Int = 0,
    modifier: Modifier = Modifier
) {
    val statusLabel = when (saveStatus) {
        SaveStatus.Saved -> "All changes saved"
        SaveStatus.Saving -> "Saving changes..."
        SaveStatus.UnsavedChanges -> "Unsaved edits"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("$totalNotes scratchpads", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$wordCount words", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$charCount characters", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (totalTasks > 0) {
                Text(
                    "$completedTasks/$totalTasks tasks",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (activeNote?.isExternalFile == true) {
                val fileLabel = activeNote.sourceFileName ?: activeNote.title
                Text(
                    "Linked: ${activeNote.fileSource.shortBadge} • $fileLabel",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (statusMessage != null) {
                Text(statusMessage, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
            Text(statusLabel, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
        }
    }
}
