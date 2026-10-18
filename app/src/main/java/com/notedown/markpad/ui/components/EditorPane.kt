package com.notedown.markpad.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notedown.markpad.model.AppFont
import com.notedown.markpad.model.Note

@Composable
fun EditorPane(
    note: Note?,
    initialCursorPosition: Int = 0,
    editorFont: AppFont = AppFont.MONOSPACE,
    onContentChanged: (String) -> Unit,
    onCursorPositionChanged: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            val noteId = note?.id
            val initialContent = note?.content ?: ""
            val safeInitialCursor = initialCursorPosition.coerceIn(0, initialContent.length)

            var textFieldValue by remember(noteId) {
                mutableStateOf(
                    TextFieldValue(
                        text = initialContent,
                        selection = TextRange(safeInitialCursor)
                    )
                )
            }

            var textLayoutResult by remember(noteId) { mutableStateOf<TextLayoutResult?>(null) }
            var hasScrolledToInitialCursor by remember(noteId) { mutableStateOf(false) }

            // Track contents emitted locally to filter out lagging echoes from the persistence pipeline
            val emittedContents = remember(noteId) { mutableSetOf<String>() }

            val applyEditorValue: (TextFieldValue) -> Unit = { newValue ->
                val textChanged = newValue.text != textFieldValue.text
                val selectionChanged = newValue.selection != textFieldValue.selection
                textFieldValue = newValue
                if (textChanged) {
                    emittedContents.add(newValue.text)
                    onContentChanged(newValue.text)
                }
                if (selectionChanged || textChanged) {
                    onCursorPositionChanged(newValue.selection.end)
                }
            }

            // Sync with external content changes if they differ from current editor text and aren't an echo of local typing
            LaunchedEffect(note?.content) {
                val incoming = note?.content ?: ""
                if (incoming == textFieldValue.text) {
                    // Persistence caught up with current editor text
                    emittedContents.clear()
                    return@LaunchedEffect
                }

                if (incoming in emittedContents) {
                    // Stale intermediate echo from an in-flight keystroke -> ignore
                    return@LaunchedEffect
                }

                // Legitimate external content change (e.g. external sync or reset)
                val wasAtEnd = textFieldValue.selection.end >= textFieldValue.text.length
                val newCursor = if (wasAtEnd) {
                    incoming.length
                } else {
                    textFieldValue.selection.end.coerceIn(0, incoming.length)
                }
                emittedContents.clear()
                textFieldValue = textFieldValue.copy(
                    text = incoming,
                    selection = TextRange(newCursor)
                )
            }

            // Scroll to initial cursor position once text layout and scroll bounds are measured
            LaunchedEffect(noteId, textLayoutResult, scrollState.maxValue) {
                val layout = textLayoutResult ?: return@LaunchedEffect
                if (!hasScrolledToInitialCursor) {
                    if (scrollState.maxValue > 0) {
                        val cursorTarget = safeInitialCursor.coerceIn(0, textFieldValue.text.length)
                        val cursorRect = layout.getCursorRect(cursorTarget)
                        val viewportHeight = scrollState.viewportSize
                        val targetScroll = if (viewportHeight > 0) {
                            (cursorRect.top - viewportHeight / 3f)
                                .coerceIn(0f, scrollState.maxValue.toFloat())
                                .toInt()
                        } else {
                            cursorRect.top
                                .coerceIn(0f, scrollState.maxValue.toFloat())
                                .toInt()
                        }
                        scrollState.scrollTo(targetScroll)
                        hasScrolledToInitialCursor = true
                    } else if (layout.lineCount <= 1 || scrollState.maxValue == 0) {
                        hasScrolledToInitialCursor = true
                    }
                }
            }

            BasicTextField(
                value = textFieldValue,
                onValueChange = applyEditorValue,
                onTextLayout = { layout ->
                    textLayoutResult = layout
                },
                textStyle = TextStyle(
                    fontFamily = editorFont.fontFamily,
                    fontSize = 14.sp,
                    lineHeight = 24.sp,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .onPreviewKeyEvent { event ->
                        if (
                            event.type == KeyEventType.KeyDown &&
                            event.key == Key.Delete &&
                            event.isShiftPressed &&
                            !event.isCtrlPressed &&
                            !event.isAltPressed &&
                            !event.isMetaPressed
                        ) {
                            applyEditorValue(deleteLineAtCursor(textFieldValue))
                            true
                        } else {
                            false
                        }
                    }
            )
        }
    }
}

/**
 * Deletes the entire line where the cursor (`selection.end`) is currently positioned,
 * preserving the horizontal column offset (clamped to the resulting current line's length).
 */
internal fun deleteLineAtCursor(value: TextFieldValue): TextFieldValue {
    val text = value.text
    if (text.isEmpty()) {
        return TextFieldValue(text = "", selection = TextRange(0))
    }

    val cursor = value.selection.end.coerceIn(0, text.length)
    val lastNewline = if (cursor > 0) text.lastIndexOf('\n', startIndex = cursor - 1) else -1
    val lineStart = if (lastNewline == -1) 0 else lastNewline + 1

    val nextNewline = text.indexOf('\n', startIndex = cursor)
    val lineEnd = if (nextNewline == -1) text.length else nextNewline

    val column = cursor - lineStart

    return when {
        // Single-line document
        lineStart == 0 && lineEnd == text.length -> {
            TextFieldValue(text = "", selection = TextRange(0))
        }
        // First or middle line (followed by '\n')
        lineEnd < text.length -> {
            val newText = text.removeRange(lineStart, lineEnd + 1)
            val nextLineEnd = newText.indexOf('\n', startIndex = lineStart).let {
                if (it == -1) newText.length else it
            }
            val nextLineLength = (nextLineEnd - lineStart).coerceAtLeast(0)
            val newCursor = lineStart + minOf(column, nextLineLength)
            TextFieldValue(text = newText, selection = TextRange(newCursor))
        }
        // Last line of a multi-line document (preceded by '\n' at lineStart - 1)
        else -> {
            val newText = text.removeRange(lineStart - 1, lineEnd)
            val prevLineStart = if (lineStart - 1 > 0) {
                newText.lastIndexOf('\n', startIndex = lineStart - 2).let {
                    if (it == -1) 0 else it + 1
                }
            } else {
                0
            }
            val prevLineLength = (newText.length - prevLineStart).coerceAtLeast(0)
            val newCursor = prevLineStart + minOf(column, prevLineLength)
            TextFieldValue(text = newText, selection = TextRange(newCursor))
        }
    }
}

