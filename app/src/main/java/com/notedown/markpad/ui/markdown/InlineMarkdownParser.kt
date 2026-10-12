package com.notedown.markpad.ui.markdown

sealed interface InlineSpan {
    val text: String

    data class Normal(override val text: String) : InlineSpan
    data class Bold(override val text: String) : InlineSpan
    data class Italic(override val text: String) : InlineSpan
    data class BoldItalic(override val text: String) : InlineSpan
    data class InlineCode(override val text: String) : InlineSpan
}

object InlineMarkdownParser {

    private val INLINE_REGEX = Regex("""(\*\*\*(.*?)\*\*\*|\*\*(.*?)\*\*|\*(.*?)\*|`(.*?)`)""")

    fun parse(input: String): List<InlineSpan> {
        if (input.isEmpty()) return emptyList()

        val spans = mutableListOf<InlineSpan>()
        var cursor = 0
        val matches = INLINE_REGEX.findAll(input)

        for (match in matches) {
            if (match.range.first > cursor) {
                val plainText = input.substring(cursor, match.range.first)
                if (plainText.isNotEmpty()) {
                    spans.add(InlineSpan.Normal(plainText))
                }
            }

            val fullText = match.value
            when {
                fullText.startsWith("***") -> {
                    val content = match.groupValues[2]
                    if (content.isNotEmpty()) {
                        spans.add(InlineSpan.BoldItalic(content))
                    }
                }
                fullText.startsWith("**") -> {
                    val content = match.groupValues[3]
                    if (content.isNotEmpty()) {
                        spans.add(InlineSpan.Bold(content))
                    }
                }
                fullText.startsWith("*") -> {
                    val content = match.groupValues[4]
                    if (content.isNotEmpty()) {
                        spans.add(InlineSpan.Italic(content))
                    }
                }
                fullText.startsWith("`") -> {
                    val content = match.groupValues[5]
                    if (content.isNotEmpty()) {
                        spans.add(InlineSpan.InlineCode(content))
                    }
                }
            }
            cursor = match.range.last + 1
        }

        if (cursor < input.length) {
            val remaining = input.substring(cursor)
            if (remaining.isNotEmpty()) {
                spans.add(InlineSpan.Normal(remaining))
            }
        }

        return spans.ifEmpty { listOf(InlineSpan.Normal(input)) }
    }

    fun toPlainText(input: String): String {
        return parse(input).joinToString("") { it.text }
    }
}
