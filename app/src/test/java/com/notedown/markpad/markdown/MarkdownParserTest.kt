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

    @Test
    fun parseTable_basicHeaderAndRows() {
        val input = """
            | First Header | Second Header |
            | ------------ | ------------- |
            | Content Cell | Content Cell  |
            | Content Cell | Content Cell  |
        """.trimIndent()

        val nodes = MarkdownParser.parse(input)

        assertEquals(1, nodes.size)
        assertTrue(nodes[0] is MarkdownNode.Table)
        val table = nodes[0] as MarkdownNode.Table
        assertEquals(listOf("First Header", "Second Header"), table.headers)
        assertEquals(listOf(com.notedown.markpad.ui.markdown.TableAlignment.LEFT, com.notedown.markpad.ui.markdown.TableAlignment.LEFT), table.alignments)
        assertEquals(2, table.rows.size)
        assertEquals(listOf("Content Cell", "Content Cell"), table.rows[0].cells)
        assertEquals(listOf("Content Cell", "Content Cell"), table.rows[1].cells)
    }

    @Test
    fun parseTable_withAlignments() {
        val input = """
            | Left-aligned | Center-aligned | Right-aligned |
            | :---         | :---:          | ---:          |
            | git status   | git diff       | git commit    |
        """.trimIndent()

        val nodes = MarkdownParser.parse(input)

        assertEquals(1, nodes.size)
        val table = nodes[0] as MarkdownNode.Table
        assertEquals(listOf("Left-aligned", "Center-aligned", "Right-aligned"), table.headers)
        assertEquals(
            listOf(
                com.notedown.markpad.ui.markdown.TableAlignment.LEFT,
                com.notedown.markpad.ui.markdown.TableAlignment.CENTER,
                com.notedown.markpad.ui.markdown.TableAlignment.RIGHT
            ),
            table.alignments
        )
        assertEquals(1, table.rows.size)
        assertEquals(listOf("git status", "git diff", "git commit"), table.rows[0].cells)
    }

    @Test
    fun parseTable_withEscapedPipes() {
        val input = """
            | Symbol | Meaning |
            | --- | --- |
            | \| | Logical OR operator |
            | && | Logical AND operator |
        """.trimIndent()

        val nodes = MarkdownParser.parse(input)

        assertEquals(1, nodes.size)
        val table = nodes[0] as MarkdownNode.Table
        assertEquals(listOf("Symbol", "Meaning"), table.headers)
        assertEquals(2, table.rows.size)
        assertEquals(listOf("|", "Logical OR operator"), table.rows[0].cells)
        assertEquals(listOf("&&", "Logical AND operator"), table.rows[1].cells)
    }

    @Test
    fun parseTable_withoutOuterPipes() {
        val input = """
            Command | Description
            --- | ---
            ls -la | List directory contents
            pwd | Print working directory
        """.trimIndent()

        val nodes = MarkdownParser.parse(input)

        assertEquals(1, nodes.size)
        val table = nodes[0] as MarkdownNode.Table
        assertEquals(listOf("Command", "Description"), table.headers)
        assertEquals(2, table.rows.size)
        assertEquals(listOf("ls -la", "List directory contents"), table.rows[0].cells)
        assertEquals(listOf("pwd", "Print working directory"), table.rows[1].cells)
    }

    @Test
    fun parseTable_raggedRowsNormalizedToColumnCount() {
        val input = """
            | Col 1 | Col 2 | Col 3 |
            | --- | --- | --- |
            | only one |
            | one | two | three | four extra |
        """.trimIndent()

        val nodes = MarkdownParser.parse(input)

        assertEquals(1, nodes.size)
        val table = nodes[0] as MarkdownNode.Table
        assertEquals(3, table.headers.size)
        assertEquals(2, table.rows.size)
        assertEquals(listOf("only one", "", ""), table.rows[0].cells)
        assertEquals(listOf("one", "two", "three"), table.rows[1].cells)
    }

    @Test
    fun parseTable_embeddedInMarkdownDocument() {
        val input = """
            # Document Title

            Here is an introductory paragraph.

            | Setting | Value |
            | --- | --- |
            | Theme | Dark |
            | AutoSave | Enabled |

            - Trailing bullet 1
            - Trailing bullet 2
        """.trimIndent()

        val nodes = MarkdownParser.parse(input)

        assertEquals(5, nodes.size)
        assertEquals(MarkdownNode.Header(1, "Document Title"), nodes[0])
        assertEquals(MarkdownNode.Paragraph("Here is an introductory paragraph."), nodes[1])
        assertTrue(nodes[2] is MarkdownNode.Table)
        assertEquals(MarkdownNode.BulletItem("Trailing bullet 1"), nodes[3])
        assertEquals(MarkdownNode.BulletItem("Trailing bullet 2"), nodes[4])
    }
}
