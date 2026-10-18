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

    @Test
    fun parse_strikethroughDoubleAndSingleTilde() {
        val input = "Remove ~~deprecated~~ and ~legacy~ APIs"
        val spans = InlineMarkdownParser.parse(input)

        assertEquals(5, spans.size)
        assertEquals(InlineSpan.Normal("Remove "), spans[0])
        assertEquals(InlineSpan.Strikethrough("deprecated"), spans[1])
        assertEquals(InlineSpan.Normal(" and "), spans[2])
        assertEquals(InlineSpan.Strikethrough("legacy"), spans[3])
        assertEquals(InlineSpan.Normal(" APIs"), spans[4])
    }

    @Test
    fun parse_underscoreEmphasisRespectsGfmIntrawordRule() {
        val input = "Use __strong__ and _emphasis_ but keep foo_bar_baz intact"
        val spans = InlineMarkdownParser.parse(input)

        assertEquals(5, spans.size)
        assertEquals(InlineSpan.Normal("Use "), spans[0])
        assertEquals(InlineSpan.Bold("strong"), spans[1])
        assertEquals(InlineSpan.Normal(" and "), spans[2])
        assertEquals(InlineSpan.Italic("emphasis"), spans[3])
        assertEquals(InlineSpan.Normal(" but keep foo_bar_baz intact"), spans[4])
    }

    @Test
    fun parse_linksAutolinksAndImages() {
        val input = "Visit [GitHub](https://github.com \"Homepage\"), https://example.com/docs, <dev@example.com>, and ![Logo](https://example.com/logo.png \"Brand\")"
        val spans = InlineMarkdownParser.parse(input)

        assertEquals(8, spans.size)
        assertEquals(InlineSpan.Normal("Visit "), spans[0])
        assertEquals(InlineSpan.Link("GitHub", "https://github.com", "Homepage"), spans[1])
        assertEquals(InlineSpan.Normal(", "), spans[2])
        assertEquals(InlineSpan.Link("https://example.com/docs", "https://example.com/docs"), spans[3])
        assertEquals(InlineSpan.Normal(", "), spans[4])
        assertEquals(InlineSpan.Link("dev@example.com", "mailto:dev@example.com"), spans[5])
        assertEquals(InlineSpan.Normal(", and "), spans[6])
        assertEquals(InlineSpan.Image("Logo", "https://example.com/logo.png", "Brand"), spans[7])
    }

    @Test
    fun parse_doubleBackticksHtmlTagsHighlightAndFootnotes() {
        val input = "Press <kbd>Ctrl + S</kbd> for `` `literal` `` with H<sub>2</sub>O, x<sup>2</sup>, ==marked==, <ins>inserted</ins>, and ref[^1]"
        val spans = InlineMarkdownParser.parse(input)

        assertEquals(
            listOf(
                InlineSpan.Normal("Press "),
                InlineSpan.KeyboardKey("Ctrl + S"),
                InlineSpan.Normal(" for "),
                InlineSpan.InlineCode("`literal`"),
                InlineSpan.Normal(" with H"),
                InlineSpan.Subscript("2"),
                InlineSpan.Normal("O, x"),
                InlineSpan.Superscript("2"),
                InlineSpan.Normal(", "),
                InlineSpan.Highlight("marked"),
                InlineSpan.Normal(", "),
                InlineSpan.Underline("inserted"),
                InlineSpan.Normal(", and ref"),
                InlineSpan.FootnoteReference("[1]", "1")
            ),
            spans
        )
    }

    @Test
    fun parse_backslashEscapesAndEmojiShortcodes() {
        val input = """Escaped \*not italic\* and :rocket: launch :white_check_mark:"""
        val spans = InlineMarkdownParser.parse(input)

        assertEquals(1, spans.size)
        assertEquals(InlineSpan.Normal("Escaped *not italic* and 🚀 launch ✅"), spans[0])
    }
}
