package io.salmonllama.MarkPad.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.salmonllama.MarkPad.model.FileSource

class OpenMarkdownDocumentContract : ActivityResultContract<FileSource, Uri?>() {
    override fun createIntent(context: Context, input: FileSource): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(
                Intent.EXTRA_MIME_TYPES,
                arrayOf(
                    "text/markdown",
                    "text/x-markdown",
                    "text/plain",
                    "application/octet-stream"
                )
            )
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
            buildInitialUriForSource(input)?.let { initialUri ->
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri)
            }
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return intent?.data?.takeIf { resultCode == Activity.RESULT_OK }
    }

    companion object {
        fun buildInitialUriForSource(source: FileSource): Uri? {
            return when (source) {
                FileSource.LOCAL_COMPUTER -> DocumentsContract.buildDocumentUri(
                    "com.android.externalstorage.documents",
                    "primary:Documents"
                )
                FileSource.LINUX_ENVIRONMENT -> DocumentsContract.buildDocumentUri(
                    "com.android.externalstorage.documents",
                    "primary:Documents/source"
                )
                FileSource.GOOGLE_DRIVE -> DocumentsContract.buildRootsUri(
                    "com.google.android.apps.docs.storage"
                )
                FileSource.INTERNAL -> null
            }
        }
    }
}

@Composable
fun OpenFileDialog(
    onDismiss: () -> Unit,
    onFileSelected: (Uri, FileSource) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSource by remember { mutableStateOf(FileSource.LOCAL_COMPUTER) }

    val openFileLauncher = rememberLauncherForActivityResult(
        contract = OpenMarkdownDocumentContract()
    ) { uri ->
        if (uri != null) {
            onFileSelected(uri, selectedSource)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.width(460.dp),
        icon = {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "Open Markdown File",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Select File Source:",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SourceOptionCard(
                        source = FileSource.LOCAL_COMPUTER,
                        icon = Icons.Default.Computer,
                        isSelected = selectedSource == FileSource.LOCAL_COMPUTER,
                        onSelect = { selectedSource = FileSource.LOCAL_COMPUTER }
                    )

                    SourceOptionCard(
                        source = FileSource.LINUX_ENVIRONMENT,
                        icon = Icons.Default.Terminal,
                        isSelected = selectedSource == FileSource.LINUX_ENVIRONMENT,
                        onSelect = { selectedSource = FileSource.LINUX_ENVIRONMENT }
                    )

                    SourceOptionCard(
                        source = FileSource.GOOGLE_DRIVE,
                        icon = Icons.Default.CloudDownload,
                        isSelected = selectedSource == FileSource.GOOGLE_DRIVE,
                        onSelect = { selectedSource = FileSource.GOOGLE_DRIVE }
                    )
                }

                Text(
                    text = "Opened files remain live-linked to their source so saving (Ctrl+S) writes changes directly back to the file.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    openFileLauncher.launch(selectedSource)
                }
            ) {
                Text("Browse ${selectedSource.displayName}")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SourceOptionCard(
    source: FileSource,
    icon: ImageVector,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onSelect),
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = source.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = source.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 11.sp
                )
            }
        }
    }
}
