package com.notedown.markpad.markdown

import com.notedown.markpad.ui.markdown.MarkdownNode
import com.notedown.markpad.ui.markdown.MarkdownParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {

    @Test
    fun parseHeaders_correctlyIdentifiesLevels() {
        val input = """
            # Heading 1
            ## Heading 2
            ### Heading 3
        """.trimIndent()

        val nodes = MarkdownParser.parse(input)

        assertEquals(3, nodes.size)
        assertEquals(MarkdownNode.Header(1, "Heading 1"), nodes[0])
        assertEquals(MarkdownNode.Header(2, "Heading 2"), nodes[1])
        assertEquals(MarkdownNode.Header(3, "Heading 3"), nodes[2])
    }

    @Test
    fun parseCodeBlock_correctlyCapturesLanguageAndCode() {
        val input = """
            ```kotlin
            val x = 42
            println(x)
            ```
        """.trimIndent()

        val nodes = MarkdownParser.parse(input)

        assertEquals(1, nodes.size)
        assertTrue(nodes[0] is MarkdownNode.CodeBlock)
        val codeBlock = nodes[0] as MarkdownNode.CodeBlock
        assertEquals("kotlin", codeBlock.language)
        assertEquals("val x = 42\nprintln(x)", codeBlock.code)
    }

    @Test
    fun parseBulletItems_handlesDashesAndAsterisks() {
        val input = """
            - Item Alpha
            * Item Beta
        """.trimIndent()

        val nodes = MarkdownParser.parse(input)

        assertEquals(2, nodes.size)
        assertEquals(MarkdownNode.BulletItem("Item Alpha"), nodes[0])
        assertEquals(MarkdownNode.BulletItem("Item Beta"), nodes[1])
    }

    @Test
    fun parseBlockquoteAndHorizontalRule() {
        val input = """
            > Notable developer quotation
            ---
        """.trimIndent()

        val nodes = MarkdownParser.parse(input)

        assertEquals(2, nodes.size)
        assertEquals(MarkdownNode.Blockquote("Notable developer quotation"), nodes[0])
        assertEquals(MarkdownNode.HorizontalRule, nodes[1])
    }
}
