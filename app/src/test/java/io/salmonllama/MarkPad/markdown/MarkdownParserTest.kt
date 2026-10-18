package io.salmonllama.MarkPad.markdown

import io.salmonllama.MarkPad.ui.markdown.AlertType
import io.salmonllama.MarkPad.ui.markdown.MarkdownNode
import io.salmonllama.MarkPad.ui.markdown.MarkdownParser
import io.salmonllama.MarkPad.ui.markdown.TableAlignment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun parseHeaders_supportsLevels4To6AndClosingHashesAndSetext() {
        val input = """
            #### Heading 4
            ##### Heading 5 #####
            ###### Heading 6
            Setext Heading 1
            ================
            Setext Heading 2
            ----------------
        """.trimIndent()

        val nodes = MarkdownParser.parse(input)

        assertEquals(5, nodes.size)
        assertEquals(MarkdownNode.Header(4, "Heading 4"), nodes[0])
        assertEquals(MarkdownNode.Header(5, "Heading 5"), nodes[1])
        assertEquals(MarkdownNode.Header(6, "Heading 6"), nodes[2])
        assertEquals(MarkdownNode.Header(1, "Setext Heading 1"), nodes[3])
        assertEquals(MarkdownNode.Header(2, "Setext Heading 2"), nodes[4])
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
    fun parseCodeBlock_supportsTildeFencesAndIndentedCodeBlocks() {
        val input = """
            ~~~python
            def greet():
                return "hello"
            ~~~

            Paragraph before indented code:

                val indented = true
                println(indented)
        """.trimIndent()

        val nodes = MarkdownParser.parse(input)

        assertEquals(3, nodes.size)
        assertEquals(MarkdownNode.CodeBlock("python", "def greet():\n    return \"hello\""), nodes[0])
        assertEquals(MarkdownNode.Paragraph("Paragraph before indented code:"), nodes[1])
        assertEquals(MarkdownNode.CodeBlock("", "val indented = true\nprintln(indented)"), nodes[2])
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
    fun parseLists_supportsPlusMarkersOrderedListsAndNesting() {
        val input = """
            + Plus item root
              - Nested level 1
                * Nested level 2
            1. First step
            2) Second step
              1. Sub-step A
        """.trimIndent()

        val nodes = MarkdownParser.parse(input)

        assertEquals(6, nodes.size)
        assertEquals(MarkdownNode.BulletItem("Plus item root", indentLevel = 0), nodes[0])
        assertEquals(MarkdownNode.BulletItem("Nested level 1", indentLevel = 1), nodes[1])
        assertEquals(MarkdownNode.BulletItem("Nested level 2", indentLevel = 2), nodes[2])
        assertEquals(MarkdownNode.OrderedItem(1, "First step", indentLevel = 0), nodes[3])
        assertEquals(MarkdownNode.OrderedItem(2, "Second step", indentLevel = 0), nodes[4])
        assertEquals(MarkdownNode.OrderedItem(1, "Sub-step A", indentLevel = 1), nodes[5])
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
    fun parseNestedBlockquotesAndExtendedHorizontalRules() {
        val input = """
            > Level 1 quote
            >> Level 2 quote
            > > > Level 3 quote
            ___
            - - -
            * * *
        """.trimIndent()

        val nodes = MarkdownParser.parse(input)

        assertEquals(6, nodes.size)
        assertEquals(MarkdownNode.Blockquote("Level 1 quote", depth = 1), nodes[0])
        assertEquals(MarkdownNode.Blockquote("Level 2 quote", depth = 2), nodes[1])
        assertEquals(MarkdownNode.Blockquote("Level 3 quote", depth = 3), nodes[2])
        assertEquals(MarkdownNode.HorizontalRule, nodes[3])
        assertEquals(MarkdownNode.HorizontalRule, nodes[4])
        assertEquals(MarkdownNode.HorizontalRule, nodes[5])
    }

    @Test
    fun parseGitHubAlerts_parsesAllFiveAlertTypes() {
        val input = """
            > [!NOTE]
            > Useful information that users should know.

            > [!TIP]
            > Helpful advice for doing things better.

            > [!IMPORTANT]
            > Key information users need to know to achieve their goal.

            > [!WARNING]
            > Urgent info that needs immediate user attention.

            > [!CAUTION]
            > Advises about risks or negative outcomes.
        """.trimIndent()

        val nodes = MarkdownParser.parse(input)

        assertEquals(5, nodes.size)
        assertEquals(MarkdownNode.AlertBlock(AlertType.NOTE, "Useful information that users should know."), nodes[0])
        assertEquals(MarkdownNode.AlertBlock(AlertType.TIP, "Helpful advice for doing things better."), nodes[1])
        assertEquals(MarkdownNode.AlertBlock(AlertType.IMPORTANT, "Key information users need to know to achieve their goal."), nodes[2])
        assertEquals(MarkdownNode.AlertBlock(AlertType.WARNING, "Urgent info that needs immediate user attention."), nodes[3])
        assertEquals(MarkdownNode.AlertBlock(AlertType.CAUTION, "Advises about risks or negative outcomes."), nodes[4])
    }

    @Test
    fun parseDetailsAndFootnotesAndReferenceLinks() {
        val input = """
            Check out the [GitHub Spec][gfm] and [MarkPad][] with footnote[^1].

            <details open>
            <summary>Release Notes</summary>
            New GFM parser enabled.
            </details>

            [^1]: GitHub Flavored Markdown specification.
            [gfm]: https://github.github.com/gfm/ "GFM Spec"
            [markpad]: https://example.com/markpad
        """.trimIndent()

        val nodes = MarkdownParser.parse(input)

        assertEquals(3, nodes.size)
        assertEquals(
            MarkdownNode.Paragraph(
                "Check out the [GitHub Spec](https://github.github.com/gfm/ \"GFM Spec\") and [MarkPad](https://example.com/markpad) with footnote[^1]."
            ),
            nodes[0]
        )
        val details = nodes[1] as MarkdownNode.DetailsBlock
        assertEquals("Release Notes", details.summary)
        assertEquals("New GFM parser enabled.", details.body)
        assertTrue(details.isOpenByDefault)

        assertEquals(MarkdownNode.FootnoteDefinition("1", "GitHub Flavored Markdown specification."), nodes[2])
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
        assertEquals(listOf(TableAlignment.LEFT, TableAlignment.LEFT), table.alignments)
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
                TableAlignment.LEFT,
                TableAlignment.CENTER,
                TableAlignment.RIGHT
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

    @Test
    fun parseTaskItems_handlesUncheckedAndCheckedDashesAndAsterisks() {
        val input = """
            - [ ] Unchecked dash task
            - [x] Checked lowercase task
            - [X] Checked uppercase task
            * [ ] Unchecked asterisk task
            * [x] Checked asterisk task
            - [ ]
        """.trimIndent()

        val nodes = MarkdownParser.parse(input)

        assertEquals(6, nodes.size)
        assertEquals(MarkdownNode.TaskItem(isChecked = false, text = "Unchecked dash task", lineIndex = 0), nodes[0])
        assertEquals(MarkdownNode.TaskItem(isChecked = true, text = "Checked lowercase task", lineIndex = 1), nodes[1])
        assertEquals(MarkdownNode.TaskItem(isChecked = true, text = "Checked uppercase task", lineIndex = 2), nodes[2])
        assertEquals(MarkdownNode.TaskItem(isChecked = false, text = "Unchecked asterisk task", lineIndex = 3), nodes[3])
        assertEquals(MarkdownNode.TaskItem(isChecked = true, text = "Checked asterisk task", lineIndex = 4), nodes[4])
        assertEquals(MarkdownNode.TaskItem(isChecked = false, text = "", lineIndex = 5), nodes[5])
    }

    @Test
    fun parseMixedBulletsAndTaskItems_tracksAccurateLineIndices() {
        val input = """
            # Sprint Tasks

            - Standard bullet item
            - [ ] First actionable task
            * [x] Completed second task
        """.trimIndent()

        val nodes = MarkdownParser.parse(input)

        assertEquals(4, nodes.size)
        assertEquals(MarkdownNode.Header(1, "Sprint Tasks"), nodes[0])
        assertEquals(MarkdownNode.BulletItem("Standard bullet item"), nodes[1])
        assertEquals(MarkdownNode.TaskItem(isChecked = false, text = "First actionable task", lineIndex = 3), nodes[2])
        assertEquals(MarkdownNode.TaskItem(isChecked = true, text = "Completed second task", lineIndex = 4), nodes[3])
    }
}
