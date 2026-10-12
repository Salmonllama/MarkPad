package com.notedown.markpad.ui.markdown

sealed interface MarkdownNode {
    data class Header(val level: Int, val text: String) : MarkdownNode
    data class Paragraph(val text: String) : MarkdownNode
    data class BulletItem(val text: String) : MarkdownNode
    data class CodeBlock(val language: String, val code: String) : MarkdownNode
    data class Blockquote(val text: String) : MarkdownNode
    data object HorizontalRule : MarkdownNode
}

object MarkdownParser {
    fun parse(markdown: String): List<MarkdownNode> {
        val lines = markdown.lines()
        val nodes = mutableListOf<MarkdownNode>()
        var inCodeBlock = false
        var codeLang = ""
        val codeBuffer = StringBuilder()

        for (line in lines) {
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
                continue
            }

            if (inCodeBlock) {
                codeBuffer.append(line).append("\n")
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
        }

        if (inCodeBlock) {
            nodes.add(MarkdownNode.CodeBlock(codeLang, codeBuffer.toString().trimEnd()))
        }

        return nodes
    }
}
