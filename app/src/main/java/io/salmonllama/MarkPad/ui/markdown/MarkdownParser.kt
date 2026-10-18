package io.salmonllama.MarkPad.ui.markdown

enum class TableAlignment {
    LEFT,
    CENTER,
    RIGHT
}

data class TableRow(
    val cells: List<String>
)

enum class AlertType(val displayName: String) {
    NOTE("Note"),
    TIP("Tip"),
    IMPORTANT("Important"),
    WARNING("Warning"),
    CAUTION("Caution")
}

sealed interface MarkdownNode {
    data class Header(val level: Int, val text: String) : MarkdownNode
    data class Paragraph(val text: String) : MarkdownNode
    data class BulletItem(
        val text: String,
        val indentLevel: Int = 0
    ) : MarkdownNode
    data class OrderedItem(
        val number: Int,
        val text: String,
        val indentLevel: Int = 0
    ) : MarkdownNode
    data class TaskItem(
        val isChecked: Boolean,
        val text: String,
        val lineIndex: Int,
        val indentLevel: Int = 0
    ) : MarkdownNode
    data class CodeBlock(val language: String, val code: String) : MarkdownNode
    data class Blockquote(
        val text: String,
        val depth: Int = 1
    ) : MarkdownNode
    data class AlertBlock(
        val type: AlertType,
        val text: String
    ) : MarkdownNode
    data class DetailsBlock(
        val summary: String,
        val body: String,
        val isOpenByDefault: Boolean = false
    ) : MarkdownNode
    data class FootnoteDefinition(
        val id: String,
        val text: String
    ) : MarkdownNode
    data object HorizontalRule : MarkdownNode
    data class Table(
        val headers: List<String>,
        val alignments: List<TableAlignment>,
        val rows: List<TableRow>
    ) : MarkdownNode
}

object MarkdownParser {
    private val DELIMITER_CELL_REGEX = Regex("""^:?-+:?$""")
    private val FENCE_OPEN_REGEX = Regex("""^\s{0,3}(`{3,}|~{3,})(.*)$""")
    private val ATX_HEADER_REGEX = Regex("""^(#{1,6})(?:\s+(.*?)(?:\s+#+\s*)?|\s*)$""")
    private val SETEXT_H1_REGEX = Regex("""^={3,}\s*$""")
    private val SETEXT_H2_REGEX = Regex("""^-{3,}\s*$""")
    private val HORIZONTAL_RULE_REGEX = Regex("""^(?:(?:-\s*){3,}|(?:\*\s*){3,}|(?:_\s*){3,})$""")
    private val TASK_ITEM_REGEX = Regex("""^(\s*)(?:[-*+]|\d+[.)])\s+\[([ xX])](\s+(.*))?$""")
    private val BULLET_ITEM_REGEX = Regex("""^(\s*)[-*+]\s+(.*)$""")
    private val ORDERED_ITEM_REGEX = Regex("""^(\s*)(\d+)[.)]\s+(.*)$""")
    private val ALERT_HEADER_REGEX = Regex(
        """^>\s*\[!(NOTE|TIP|IMPORTANT|WARNING|CAUTION)](?:\s+(.*))?$""",
        RegexOption.IGNORE_CASE
    )
    private val BLOCKQUOTE_LINE_REGEX = Regex("""^((?:>\s*)+)(.*)$""")
    private val FOOTNOTE_DEF_REGEX = Regex("""^\[\^([a-zA-Z0-9_-]+)]:\s*(.*)$""")
    private val REF_LINK_DEF_REGEX = Regex(
        """^\s{0,3}\[([^\^\]]+)]:\s*<?([^\s>]+)>?(?:\s+(?:["']([^"']*)["']|\(([^)]*)\)))?\s*$"""
    )
    private val DETAILS_SINGLE_LINE_REGEX = Regex(
        """^<details(\s+open)?\s*>\s*<summary>(.*?)</summary>(.*?)</details>\s*$""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val DETAILS_OPEN_TAG_REGEX = Regex(
        """^<details(\s+open)?\s*>(.*)$""",
        RegexOption.IGNORE_CASE
    )
    private val SUMMARY_INLINE_REGEX = Regex(
        """<summary>(.*?)</summary>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    private const val ESCAPED_PIPE_PLACEHOLDER = "\u0000_ESCAPED_PIPE_\u0000"
    private const val CODE_PIPE_PLACEHOLDER = "\u0000_CODE_PIPE_\u0000"

    private data class ReferenceLinkTarget(
        val url: String,
        val title: String?
    )

    fun parse(markdown: String): List<MarkdownNode> {
        val lines = markdown.lines()
        val nodes = mutableListOf<MarkdownNode>()

        // Pre-pass: collect reference link definitions outside fenced code blocks
        val references = mutableMapOf<String, ReferenceLinkTarget>()
        val refDefLineIndices = mutableSetOf<Int>()
        run {
            var inFence = false
            var fenceChar = ' '
            var fenceLen = 0
            for (idx in lines.indices) {
                val raw = lines[idx]
                val fenceMatch = FENCE_OPEN_REGEX.matchEntire(raw)
                if (fenceMatch != null) {
                    val marker = fenceMatch.groupValues[1]
                    val rest = fenceMatch.groupValues[2]
                    if (!inFence) {
                        inFence = true
                        fenceChar = marker[0]
                        fenceLen = marker.length
                        continue
                    } else if (marker[0] == fenceChar && marker.length >= fenceLen && rest.isBlank()) {
                        inFence = false
                        continue
                    }
                }
                if (!inFence) {
                    val refMatch = REF_LINK_DEF_REGEX.matchEntire(raw)
                    if (refMatch != null) {
                        val key = refMatch.groupValues[1].trim().lowercase()
                        val url = refMatch.groupValues[2].trim()
                        val title = refMatch.groupValues[3].ifEmpty { refMatch.groupValues[4] }.takeIf { it.isNotEmpty() }
                        references[key] = ReferenceLinkTarget(url, title)
                        refDefLineIndices.add(idx)
                    }
                }
            }
        }

        var inCodeBlock = false
        var codeFenceChar = '`'
        var codeFenceLength = 3
        var codeLang = ""
        val codeBuffer = StringBuilder()

        var i = 0
        while (i < lines.size) {
            if (!inCodeBlock && i in refDefLineIndices) {
                i++
                continue
            }

            val line = lines[i]
            val trimmed = line.trim()

            // 1. Fenced Code Blocks (``` or ~~~)
            val fenceMatch = FENCE_OPEN_REGEX.matchEntire(line)
            if (fenceMatch != null) {
                val marker = fenceMatch.groupValues[1]
                val infoString = fenceMatch.groupValues[2].trim()
                if (inCodeBlock) {
                    if (marker[0] == codeFenceChar && marker.length >= codeFenceLength && infoString.isEmpty()) {
                        nodes.add(MarkdownNode.CodeBlock(codeLang, codeBuffer.toString().trimEnd()))
                        codeBuffer.clear()
                        inCodeBlock = false
                        i++
                        continue
                    }
                } else {
                    inCodeBlock = true
                    codeFenceChar = marker[0]
                    codeFenceLength = marker.length
                    codeLang = infoString
                    i++
                    continue
                }
            }

            if (inCodeBlock) {
                codeBuffer.append(line).append("\n")
                i++
                continue
            }

            // 2. Collapsible <details> blocks
            val singleDetailsMatch = DETAILS_SINGLE_LINE_REGEX.matchEntire(trimmed)
            if (singleDetailsMatch != null) {
                val isOpen = singleDetailsMatch.groupValues[1].isNotBlank()
                val summary = resolveReferenceLinks(singleDetailsMatch.groupValues[2].trim(), references)
                val body = resolveReferenceLinks(singleDetailsMatch.groupValues[3].trim(), references)
                nodes.add(MarkdownNode.DetailsBlock(summary = summary, body = body, isOpenByDefault = isOpen))
                i++
                continue
            }

            val openDetailsMatch = DETAILS_OPEN_TAG_REGEX.matchEntire(trimmed)
            if (openDetailsMatch != null && !trimmed.contains("</details>", ignoreCase = true)) {
                val isOpen = openDetailsMatch.groupValues[1].isNotBlank()
                val detailBuffer = StringBuilder()
                val trailingOnOpen = openDetailsMatch.groupValues[2].trim()
                if (trailingOnOpen.isNotEmpty()) {
                    detailBuffer.append(trailingOnOpen).append("\n")
                }
                i++
                while (i < lines.size) {
                    val dLine = lines[i]
                    val closeIdx = dLine.indexOf("</details>", ignoreCase = true)
                    if (closeIdx != -1) {
                        val beforeClose = dLine.substring(0, closeIdx)
                        if (beforeClose.isNotBlank()) {
                            detailBuffer.append(beforeClose).append("\n")
                        }
                        i++
                        break
                    } else {
                        detailBuffer.append(dLine).append("\n")
                        i++
                    }
                }
                val rawDetailsContent = detailBuffer.toString()
                val summaryMatch = SUMMARY_INLINE_REGEX.find(rawDetailsContent)
                val summary = if (summaryMatch != null) {
                    resolveReferenceLinks(summaryMatch.groupValues[1].trim(), references)
                } else {
                    "Details"
                }
                val bodyRaw = if (summaryMatch != null) {
                    rawDetailsContent.removeRange(summaryMatch.range).trim()
                } else {
                    rawDetailsContent.trim()
                }
                nodes.add(
                    MarkdownNode.DetailsBlock(
                        summary = summary,
                        body = resolveReferenceLinks(bodyRaw, references),
                        isOpenByDefault = isOpen
                    )
                )
                continue
            }

            // 3. GFM Tables: Header row followed by delimiter row
            if (i + 1 < lines.size && trimmed.contains("|") && isDelimiterRow(lines[i + 1])) {
                val delimiterLine = lines[i + 1]
                val alignments = parseAlignments(delimiterLine)
                val columnCount = alignments.size

                val rawHeaders = splitTableCells(trimmed)
                val headers = (0 until columnCount).map { idx ->
                    resolveReferenceLinks(rawHeaders.getOrElse(idx) { "" }, references)
                }
                val rows = mutableListOf<TableRow>()

                i += 2 // Move past header and delimiter

                while (i < lines.size) {
                    val rowLine = lines[i]
                    val rowTrimmed = rowLine.trim()

                    if (rowTrimmed.isBlank() ||
                        FENCE_OPEN_REGEX.matches(rowLine) ||
                        ATX_HEADER_REGEX.matches(rowTrimmed) ||
                        HORIZONTAL_RULE_REGEX.matches(rowTrimmed)
                    ) {
                        break
                    }

                    if (!rowTrimmed.contains("|")) {
                        break
                    }

                    val rawCells = splitTableCells(rowTrimmed)
                    val normalizedCells = (0 until columnCount).map { idx ->
                        resolveReferenceLinks(rawCells.getOrElse(idx) { "" }, references)
                    }
                    rows.add(TableRow(normalizedCells))
                    i++
                }

                nodes.add(MarkdownNode.Table(headers, alignments, rows))
                continue
            }

            // 4. GitHub Alerts: > [!NOTE], > [!TIP], > [!IMPORTANT], > [!WARNING], > [!CAUTION]
            val alertMatch = ALERT_HEADER_REGEX.matchEntire(trimmed)
            if (alertMatch != null) {
                val typeName = alertMatch.groupValues[1].uppercase()
                val alertType = AlertType.valueOf(typeName)
                val bodyLines = mutableListOf<String>()
                val inlineRemainder = alertMatch.groupValues[2].trim()
                if (inlineRemainder.isNotEmpty()) {
                    bodyLines.add(resolveReferenceLinks(inlineRemainder, references))
                }
                i++
                while (i < lines.size) {
                    val nextTrimmed = lines[i].trim()
                    if (ALERT_HEADER_REGEX.matches(nextTrimmed)) {
                        break
                    }
                    val bqMatch = BLOCKQUOTE_LINE_REGEX.matchEntire(nextTrimmed)
                    if (bqMatch != null && bqMatch.groupValues[1].count { it == '>' } == 1) {
                        val content = bqMatch.groupValues[2].trim()
                        if (content.isNotEmpty()) {
                            bodyLines.add(resolveReferenceLinks(content, references))
                        }
                        i++
                    } else {
                        break
                    }
                }
                nodes.add(MarkdownNode.AlertBlock(type = alertType, text = bodyLines.joinToString("\n")))
                continue
            }

            // 5. Blockquotes (> and nested >>)
            val bqMatch = BLOCKQUOTE_LINE_REGEX.matchEntire(trimmed)
            if (bqMatch != null) {
                val markers = bqMatch.groupValues[1]
                val depth = markers.count { it == '>' }.coerceAtLeast(1)
                val quoteText = resolveReferenceLinks(bqMatch.groupValues[2].trim(), references)
                nodes.add(MarkdownNode.Blockquote(text = quoteText, depth = depth))
                i++
                continue
            }

            // 6. ATX Headings (# to ######)
            val atxMatch = ATX_HEADER_REGEX.matchEntire(trimmed)
            if (atxMatch != null) {
                val level = atxMatch.groupValues[1].length.coerceIn(1, 6)
                val headerText = resolveReferenceLinks(atxMatch.groupValues[2].trim(), references)
                nodes.add(MarkdownNode.Header(level, headerText))
                i++
                continue
            }

            // 7. Task Items (- [ ], * [x], + [X], 1. [ ])
            val taskMatch = TASK_ITEM_REGEX.matchEntire(line)
            if (taskMatch != null) {
                val indentLevel = computeIndentLevel(taskMatch.groupValues[1])
                val isChecked = taskMatch.groupValues[2].equals("x", ignoreCase = true)
                val taskText = resolveReferenceLinks(taskMatch.groupValues[4].trim(), references)
                nodes.add(
                    MarkdownNode.TaskItem(
                        isChecked = isChecked,
                        text = taskText,
                        lineIndex = i,
                        indentLevel = indentLevel
                    )
                )
                i++
                continue
            }

            // 8. Thematic Breaks / Horizontal Rules (---, ***, ___, - - -, * * *, _ _ _)
            if (HORIZONTAL_RULE_REGEX.matches(trimmed)) {
                nodes.add(MarkdownNode.HorizontalRule)
                i++
                continue
            }

            // 9. Bullet List Items (-, *, +)
            val bulletMatch = BULLET_ITEM_REGEX.matchEntire(line)
            if (bulletMatch != null) {
                val indentLevel = computeIndentLevel(bulletMatch.groupValues[1])
                val itemText = resolveReferenceLinks(bulletMatch.groupValues[2].trim(), references)
                nodes.add(MarkdownNode.BulletItem(text = itemText, indentLevel = indentLevel))
                i++
                continue
            }

            // 10. Ordered List Items (1. or 1))
            val orderedMatch = ORDERED_ITEM_REGEX.matchEntire(line)
            if (orderedMatch != null) {
                val indentLevel = computeIndentLevel(orderedMatch.groupValues[1])
                val number = orderedMatch.groupValues[2].toIntOrNull() ?: 1
                val itemText = resolveReferenceLinks(orderedMatch.groupValues[3].trim(), references)
                nodes.add(MarkdownNode.OrderedItem(number = number, text = itemText, indentLevel = indentLevel))
                i++
                continue
            }

            // 11. Footnote Definitions ([^id]: text)
            val footnoteDefMatch = FOOTNOTE_DEF_REGEX.matchEntire(trimmed)
            if (footnoteDefMatch != null) {
                val fnId = footnoteDefMatch.groupValues[1]
                val fnText = resolveReferenceLinks(footnoteDefMatch.groupValues[2].trim(), references)
                nodes.add(MarkdownNode.FootnoteDefinition(id = fnId, text = fnText))
                i++
                continue
            }

            // 12. Indented Code Blocks (4+ leading spaces or 1+ leading tab after blank line or start)
            val prevLineBlank = i == 0 || lines[i - 1].isBlank() || (i - 1 in refDefLineIndices)
            if (prevLineBlank && trimmed.isNotEmpty() && (line.startsWith("    ") || line.startsWith("\t"))) {
                val indentedBuffer = StringBuilder()
                while (i < lines.size) {
                    val curLine = lines[i]
                    if (curLine.startsWith("    ")) {
                        indentedBuffer.append(curLine.removePrefix("    ")).append("\n")
                        i++
                    } else if (curLine.startsWith("\t")) {
                        indentedBuffer.append(curLine.removePrefix("\t")).append("\n")
                        i++
                    } else if (curLine.isBlank()) {
                        indentedBuffer.append("\n")
                        i++
                    } else {
                        break
                    }
                }
                nodes.add(MarkdownNode.CodeBlock(language = "", code = indentedBuffer.toString().trimEnd()))
                continue
            }

            // 13. Setext Headings (Paragraph line followed immediately by === or ---)
            if (trimmed.isNotBlank() && i + 1 < lines.size && (i + 1 !in refDefLineIndices)) {
                val nextTrimmed = lines[i + 1].trim()
                if (SETEXT_H1_REGEX.matches(nextTrimmed)) {
                    nodes.add(MarkdownNode.Header(1, resolveReferenceLinks(trimmed, references)))
                    i += 2
                    continue
                } else if (SETEXT_H2_REGEX.matches(nextTrimmed)) {
                    nodes.add(MarkdownNode.Header(2, resolveReferenceLinks(trimmed, references)))
                    i += 2
                    continue
                }
            }

            // 14. Standard Paragraph
            if (trimmed.isNotBlank()) {
                val cleanedParagraph = if (trimmed.endsWith("\\") && !trimmed.endsWith("\\\\")) {
                    trimmed.dropLast(1).trimEnd()
                } else {
                    trimmed
                }
                nodes.add(MarkdownNode.Paragraph(resolveReferenceLinks(cleanedParagraph, references)))
            }
            i++
        }

        if (inCodeBlock) {
            nodes.add(MarkdownNode.CodeBlock(codeLang, codeBuffer.toString().trimEnd()))
        }

        return nodes
    }

    private fun computeIndentLevel(leadingWhitespace: String): Int {
        if (leadingWhitespace.isEmpty()) return 0
        var spaces = 0
        for (ch in leadingWhitespace) {
            spaces += if (ch == '\t') 4 else 1
        }
        return spaces / 2
    }

    private fun resolveReferenceLinks(
        input: String,
        references: Map<String, ReferenceLinkTarget>
    ): String {
        if (references.isEmpty() || !input.contains('[')) return input

        // 1. Resolve full & collapsed reference images: ![alt][ref] or ![alt][]
        var result = Regex("""!\[((?:\\.|[^\[\]\\])*)]\[([^\]]*)]""").replace(input) { m ->
            val alt = m.groupValues[1]
            val refKey = m.groupValues[2].ifEmpty { alt }.trim().lowercase()
            val target = references[refKey] ?: return@replace m.value
            val titlePart = target.title?.let { " \"$it\"" } ?: ""
            "![$alt](${target.url}$titlePart)"
        }

        // 2. Resolve full & collapsed reference links: [text][ref] or [text][]
        result = Regex("""(?<!!)\[((?:\\.|[^\[\]\\])+)]\[([^\]]*)]""").replace(result) { m ->
            val text = m.groupValues[1]
            val refKey = m.groupValues[2].ifEmpty { text }.trim().lowercase()
            val target = references[refKey] ?: return@replace m.value
            val titlePart = target.title?.let { " \"$it\"" } ?: ""
            "[$text](${target.url}$titlePart)"
        }

        // 3. Resolve shortcut reference links: [ref] (not preceded by ! or ], not followed by ( or [)
        result = Regex("""(?<![!\]])\[([^\^\[\]]+)](?!\(|\[)""").replace(result) { m ->
            val text = m.groupValues[1]
            val refKey = text.trim().lowercase()
            val target = references[refKey] ?: return@replace m.value
            val titlePart = target.title?.let { " \"$it\"" } ?: ""
            "[$text](${target.url}$titlePart)"
        }

        return result
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

        // Protect escaped pipes \| and pipes inside inline code spans `...`
        val safeLine = protectCodePipes(trimmed.replace("\\|", ESCAPED_PIPE_PLACEHOLDER))
        val tokens = safeLine.split("|")
        return tokens.map {
            it.replace(CODE_PIPE_PLACEHOLDER, "|")
                .replace(ESCAPED_PIPE_PLACEHOLDER, "|")
                .trim()
        }
    }

    private fun protectCodePipes(input: String): String {
        if (!input.contains('`')) return input
        val sb = StringBuilder(input.length)
        var inTicks = false
        for (ch in input) {
            if (ch == '`') {
                inTicks = !inTicks
                sb.append(ch)
            } else if (ch == '|' && inTicks) {
                sb.append(CODE_PIPE_PLACEHOLDER)
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }
}
