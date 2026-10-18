package io.salmonllama.MarkPad.markdown

import io.salmonllama.MarkPad.ui.markdown.InlineMarkdownParser
import io.salmonllama.MarkPad.ui.markdown.InlineSpan
import org.junit.Assert.assertEquals
import org.junit.Test

class InlineMarkdownParserTest {

    @Test
    fun parse_plainTextReturnsSingleNormalSpan() {
        val input = "Simple unformatted text"
        val spans = InlineMarkdownParser.parse(input)

        assertEquals(1, spans.size)
        assertEquals(InlineSpan.Normal("Simple unformatted text"), spans[0])
    }

    @Test
    fun parse_boldTextExtractsBoldSpanWithoutAsterisks() {
        val input = "This is **bold** text"
        val spans = InlineMarkdownParser.parse(input)

        assertEquals(3, spans.size)
        assertEquals(InlineSpan.Normal("This is "), spans[0])
        assertEquals(InlineSpan.Bold("bold"), spans[1])
        assertEquals(InlineSpan.Normal(" text"), spans[2])
    }

    @Test
    fun parse_italicTextExtractsItalicSpanWithoutAsterisks() {
        val input = "An *italicized* word"
        val spans = InlineMarkdownParser.parse(input)

        assertEquals(3, spans.size)
        assertEquals(InlineSpan.Normal("An "), spans[0])
        assertEquals(InlineSpan.Italic("italicized"), spans[1])
        assertEquals(InlineSpan.Normal(" word"), spans[2])
    }

    @Test
    fun parse_inlineCodeExtractsCodeSpan() {
        val input = "Execute `git status` now"
        val spans = InlineMarkdownParser.parse(input)

        assertEquals(3, spans.size)
        assertEquals(InlineSpan.Normal("Execute "), spans[0])
        assertEquals(InlineSpan.InlineCode("git status"), spans[1])
        assertEquals(InlineSpan.Normal(" now"), spans[2])
    }

    @Test
    fun parse_multipleSpansInSingleLine() {
        val input = "**Important**: check `README.md` and *verify* output"
        val spans = InlineMarkdownParser.parse(input)

        assertEquals(6, spans.size)
        assertEquals(InlineSpan.Bold("Important"), spans[0])
        assertEquals(InlineSpan.Normal(": check "), spans[1])
        assertEquals(InlineSpan.InlineCode("README.md"), spans[2])
        assertEquals(InlineSpan.Normal(" and "), spans[3])
        assertEquals(InlineSpan.Italic("verify"), spans[4])
        assertEquals(InlineSpan.Normal(" output"), spans[5])
    }

    @Test
    fun parse_boldItalicSpan() {
        val input = "This is ***critical notice*** here"
        val spans = InlineMarkdownParser.parse(input)

        assertEquals(3, spans.size)
        assertEquals(InlineSpan.Normal("This is "), spans[0])
        assertEquals(InlineSpan.BoldItalic("critical notice"), spans[1])
        assertEquals(InlineSpan.Normal(" here"), spans[2])
    }

    @Test
    fun toPlainText_stripsMarkdownMarkers() {
        val input = "Header with **bold** and `code` formatting"
        val plain = InlineMarkdownParser.toPlainText(input)

        assertEquals("Header with bold and code formatting", plain)
    }
}
