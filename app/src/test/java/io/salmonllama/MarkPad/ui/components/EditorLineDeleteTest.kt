package io.salmonllama.MarkPad.ui.components

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class EditorLineDeleteTest {

    @Test
    fun deleteLineAtCursor_emptyDocument_returnsEmptyAtZero() {
        val initial = TextFieldValue(text = "", selection = TextRange(0))
        val result = deleteLineAtCursor(initial)

        assertEquals("", result.text)
        assertEquals(TextRange(0), result.selection)
    }

    @Test
    fun deleteLineAtCursor_singleLineDocument_clearsTextAndResetsCursor() {
        val initial = TextFieldValue(text = "Hello world", selection = TextRange(5))
        val result = deleteLineAtCursor(initial)

        assertEquals("", result.text)
        assertEquals(TextRange(0), result.selection)
    }

    @Test
    fun deleteLineAtCursor_firstLineOfMultiLineDocument_deletesFirstLineAndPreservesColumn() {
        val text = "First\nSecond\nThird"
        // Cursor at index 2 ("Fi|rst", column = 2)
        val initial = TextFieldValue(text = text, selection = TextRange(2))
        val result = deleteLineAtCursor(initial)

        assertEquals("Second\nThird", result.text)
        // Column 2 on "Second" -> index 2 ("Se|cond\nThird")
        assertEquals(TextRange(2), result.selection)
    }

    @Test
    fun deleteLineAtCursor_middleLineOfMultiLineDocument_deletesMiddleLineAndClampsColumn() {
        val text = "First\nSecondLongLine\nThird"
        // "First\n" is 6 chars; cursor at index 18 ("First\nSecondLongLi|ne", column = 12)
        val initial = TextFieldValue(text = text, selection = TextRange(18))
        val result = deleteLineAtCursor(initial)

        assertEquals("First\nThird", result.text)
        // Next line "Third" has length 5, so column 12 clamps to 5 -> index 6 + 5 = 11
        assertEquals(TextRange(11), result.selection)
    }

    @Test
    fun deleteLineAtCursor_lastLineOfMultiLineDocument_deletesLastLineAndPrecedingNewline() {
        val text = "First\nSecond\nThird"
        // "First\nSecond\n" is 13 chars; cursor at index 16 ("Thi|rd", column = 3)
        val initial = TextFieldValue(text = text, selection = TextRange(16))
        val result = deleteLineAtCursor(initial)

        assertEquals("First\nSecond", result.text)
        // Moves to "Second" (starts at 6) at column 3 -> index 9 ("First\nSec|ond")
        assertEquals(TextRange(9), result.selection)
    }

    @Test
    fun deleteLineAtCursor_blankMiddleLine_removesEmptyLineAndPositionsAtNextLineStart() {
        val text = "First\n\nThird"
        // Cursor on the blank middle line at index 6
        val initial = TextFieldValue(text = text, selection = TextRange(6))
        val result = deleteLineAtCursor(initial)

        assertEquals("First\nThird", result.text)
        assertEquals(TextRange(6), result.selection)
    }

    @Test
    fun deleteLineAtCursor_trailingEmptyLine_removesTrailingNewline() {
        val text = "First\n"
        // Cursor on the trailing empty line at index 6
        val initial = TextFieldValue(text = text, selection = TextRange(6))
        val result = deleteLineAtCursor(initial)

        assertEquals("First", result.text)
        assertEquals(TextRange(0), result.selection)
    }
}
