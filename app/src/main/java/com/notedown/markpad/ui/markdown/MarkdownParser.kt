package com.notedown.markpad.ui.markdown

enum class TableAlignment {
    LEFT,
    CENTER,
    RIGHT
}

data class TableRow(
    val cells: List<String>
)

sealed interface MarkdownNode {
    data class Header(val level: Int, val text: String) : MarkdownNode
    data class Paragraph(val text: String) : MarkdownNode
    data class BulletItem(val text: String) : MarkdownNode
    data class CodeBlock(val language: String, val code: String) : MarkdownNode
    data class Blockquote(val text: String) : MarkdownNode
    data object HorizontalRule : MarkdownNode
    data class Table(
        val headers: List<String>,
        val alignments: List<TableAlignment>,
        val rows: List<TableRow>
    ) : MarkdownNode
}

object MarkdownParser {
    private val DELIMITER_CELL_REGEX = Regex("""^:?-+:?$""")
    private const val ESCAPED_PIPE_PLACEHOLDER = "\u0000_ESCAPED_PIPE_\u0000"

    fun parse(markdown: String): List<MarkdownNode> {
        val lines = markdown.lines()
        val nodes = mutableListOf<MarkdownNode>()
        var inCodeBlock = false
        var codeLang = ""
        val codeBuffer = StringBuilder()

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    nodes.add(MarkdownNode.CodeBlock(codeLang, codeBuffer.toString().trimEnd()))
                    codeBuffer.clear()
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                    codeLang = trimmed.removePrefix("```").trim()
                }
                i++
                continue
            }

            if (inCodeBlock) {
                codeBuffer.append(line).append("\n")
                i++
                continue
            }

            // Check for Table: Header row followed by delimiter row
            if (i + 1 < lines.size && trimmed.contains("|") && isDelimiterRow(lines[i + 1])) {
                val delimiterLine = lines[i + 1]
                val alignments = parseAlignments(delimiterLine)
                val columnCount = alignments.size

                val rawHeaders = splitTableCells(trimmed)
                val headers = (0 until columnCount).map { idx -> rawHeaders.getOrElse(idx) { "" } }
                val rows = mutableListOf<TableRow>()

                i += 2 // Move past header and delimiter

                while (i < lines.size) {
                    val rowLine = lines[i]
                    val rowTrimmed = rowLine.trim()

                    if (rowTrimmed.isBlank() ||
                        rowTrimmed.startsWith("```") ||
                        rowTrimmed.startsWith("# ") ||
                        rowTrimmed.startsWith("## ") ||
                        rowTrimmed.startsWith("### ") ||
                        rowTrimmed == "---" ||
                        rowTrimmed == "***"
                    ) {
                        break
                    }

                    if (!rowTrimmed.contains("|")) {
                        break
                    }

                    val rawCells = splitTableCells(rowTrimmed)
                    val normalizedCells = (0 until columnCount).map { idx -> rawCells.getOrElse(idx) { "" } }
                    rows.add(TableRow(normalizedCells))
                    i++
                }

                nodes.add(MarkdownNode.Table(headers, alignments, rows))
                continue
            }

            when {
                trimmed == "---" || trimmed == "***" -> nodes.add(MarkdownNode.HorizontalRule)
                trimmed.startsWith("### ") -> nodes.add(MarkdownNode.Header(3, trimmed.removePrefix("### ").trim()))
                trimmed.startsWith("## ") -> nodes.add(MarkdownNode.Header(2, trimmed.removePrefix("## ").trim()))
                trimmed.startsWith("# ") -> nodes.add(MarkdownNode.Header(1, trimmed.removePrefix("# ").trim()))
                trimmed.startsWith("- ") -> nodes.add(MarkdownNode.BulletItem(trimmed.removePrefix("- ").trim()))
                trimmed.startsWith("* ") -> nodes.add(MarkdownNode.BulletItem(trimmed.removePrefix("* ").trim()))
                trimmed.startsWith("> ") -> nodes.add(MarkdownNode.Blockquote(trimmed.removePrefix("> ").trim()))
                trimmed.isNotBlank() -> nodes.add(MarkdownNode.Paragraph(trimmed))
            }
            i++
        }

        if (inCodeBlock) {
            nodes.add(MarkdownNode.CodeBlock(codeLang, codeBuffer.toString().trimEnd()))
        }

        return nodes
    }

    private fun isDelimiterRow(line: String): Boolean {
        val trimmed = line.trim()
        if (!trimmed.contains("-") || !trimmed.contains("|")) return false
        val cells = splitTableCells(trimmed)
        if (cells.isEmpty()) return false
        return cells.all { cell -> DELIMITER_CELL_REGEX.matches(cell.trim()) }
    }

    private fun parseAlignments(delimiterLine: String): List<TableAlignment> {
        val cells = splitTableCells(delimiterLine.trim())
        return cells.map { cell ->
            val trimmed = cell.trim()
            val leftColon = trimmed.startsWith(":")
            val rightColon = trimmed.endsWith(":")
            when {
                leftColon && rightColon -> TableAlignment.CENTER
                rightColon -> TableAlignment.RIGHT
                else -> TableAlignment.LEFT
            }
        }
    }

    private fun splitTableCells(line: String): List<String> {
        var trimmed = line.trim()
        if (trimmed.startsWith("|")) {
            trimmed = trimmed.substring(1)
        }
        if (trimmed.endsWith("|") && !trimmed.endsWith("\\|")) {
            trimmed = trimmed.substring(0, trimmed.length - 1)
        }

        val safeLine = trimmed.replace("\\|", ESCAPED_PIPE_PLACEHOLDER)
        val tokens = safeLine.split("|")
        return tokens.map { it.replace(ESCAPED_PIPE_PLACEHOLDER, "|").trim() }
    }
}
