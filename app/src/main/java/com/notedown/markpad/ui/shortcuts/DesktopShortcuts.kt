package com.notedown.markpad.ui.shortcuts

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

fun Modifier.handleMarkPadShortcuts(
    onNewNote: () -> Unit,
    onSave: () -> Unit,
    onTogglePreview: () -> Unit,
    onExport: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
): Modifier = this.onPreviewKeyEvent { event ->
    if (event.type == KeyEventType.KeyDown && event.isCtrlPressed) {
        when (event.key) {
            Key.N -> {
                onNewNote()
                true
            }
            Key.S -> {
                onSave()
                true
            }
            Key.P -> {
                onTogglePreview()
                true
            }
            Key.E -> {
                onExport()
                true
            }
            Key.Comma -> {
                onOpenSettings()
                true
            }
            else -> false
        }
    } else {
        false
    }
}
