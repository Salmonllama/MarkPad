package io.salmonllama.MarkPad.ui.components

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class EditorBackspaceTest {

    @Test
    fun backspaceAtCursor_emptyDocument_returnsUnchanged() {
        val initial = TextFieldValue(text = "", selection = TextRange(0))
        val result = backspaceAtCursor(initial)

        assertEquals("", result.text)
        assertEquals(TextRange(0), result.selection)
    }

    @Test
    fun backspaceAtCursor_cursorAtZero_returnsUnchanged() {
        val initial = TextFieldValue(text = "Hello world", selection = TextRange(0))
        val result = backspaceAtCursor(initial)

        assertEquals("Hello world", result.text)
        assertEquals(TextRange(0), result.selection)
    }

    @Test
    fun backspaceAtCursor_singleCharacter_clearsDocument() {
        val initial = TextFieldValue(text = "A", selection = TextRange(1))
        val result = backspaceAtCursor(initial)

        assertEquals("", result.text)
        assertEquals(TextRange(0), result.selection)
    }

    @Test
    fun backspaceAtCursor_middleOfText_deletesSinglePrecedingCharacter() {
        val initial = TextFieldValue(text = "Hello", selection = TextRange(3)) // Hel|lo
        val result = backspaceAtCursor(initial)

        assertEquals("Helo", result.text)
        assertEquals(TextRange(2), result.selection)
    }

    @Test
    fun backspaceAtCursor_endOfText_deletesLastCharacter() {
        val initial = TextFieldValue(text = "Hello", selection = TextRange(5)) // Hello|
        val result = backspaceAtCursor(initial)

        assertEquals("Hell", result.text)
        assertEquals(TextRange(4), result.selection)
    }

    @Test
    fun backspaceAtCursor_forwardSelection_deletesSelectedRange() {
        val initial = TextFieldValue(text = "Hello world", selection = TextRange(0, 5)) // [Hello]| world
        val result = backspaceAtCursor(initial)

        assertEquals(" world", result.text)
        assertEquals(TextRange(0), result.selection)
    }

    @Test
    fun backspaceAtCursor_reversedSelection_deletesSelectedRange() {
        val initial = TextFieldValue(text = "Hello world", selection = TextRange(5, 0)) // reversed selection
        val result = backspaceAtCursor(initial)

        assertEquals(" world", result.text)
        assertEquals(TextRange(0), result.selection)
    }

    @Test
    fun backspaceAtCursor_selectionSpanningEntireDocument_clearsText() {
        val initial = TextFieldValue(text = "Hello", selection = TextRange(0, 5))
        val result = backspaceAtCursor(initial)

        assertEquals("", result.text)
        assertEquals(TextRange(0), result.selection)
    }

    @Test
    fun backspaceAtCursor_surrogatePairEmoji_deletesFullSurrogatePair() {
        // "A🚀B" -> 'A' (index 0), '🚀' (\uD83D\uDE80, index 1..2), 'B' (index 3)
        val initial = TextFieldValue(text = "A\uD83D\uDE80B", selection = TextRange(3)) // cursor right after 🚀
        val result = backspaceAtCursor(initial)

        assertEquals("AB", result.text)
        assertEquals(TextRange(1), result.selection)
    }

    @Test
    fun backspaceAtCursor_crlfNewline_deletesBothCrAndLf() {
        val text = "Line1\r\nLine2"
        // "Line1" is 5 chars, \r is 5, \n is 6, cursor at 7 ("Line1\r\n|Line2")
        val initial = TextFieldValue(text = text, selection = TextRange(7))
        val result = backspaceAtCursor(initial)

        assertEquals("Line1Line2", result.text)
        assertEquals(TextRange(5), result.selection)
    }

    @Test
    fun backspaceAtCursor_lfNewline_deletesNewline() {
        val text = "Line1\nLine2"
        val initial = TextFieldValue(text = text, selection = TextRange(6)) // right after \n
        val result = backspaceAtCursor(initial)

        assertEquals("Line1Line2", result.text)
        assertEquals(TextRange(5), result.selection)
    }
}
