package io.salmonllama.MarkPad.ui.markdown

sealed interface InlineSpan {
    val text: String

    data class Normal(override val text: String) : InlineSpan
    data class Bold(override val text: String) : InlineSpan
    data class Italic(override val text: String) : InlineSpan
    data class BoldItalic(override val text: String) : InlineSpan
    data class Strikethrough(override val text: String) : InlineSpan
    data class InlineCode(override val text: String) : InlineSpan
    data class Highlight(override val text: String) : InlineSpan
    data class Underline(override val text: String) : InlineSpan
    data class KeyboardKey(override val text: String) : InlineSpan
    data class Superscript(override val text: String) : InlineSpan
    data class Subscript(override val text: String) : InlineSpan
    data class Link(
        override val text: String,
        val url: String,
        val title: String? = null
    ) : InlineSpan
    data class Image(
        override val text: String,
        val url: String,
        val title: String? = null
    ) : InlineSpan
    data class FootnoteReference(
        override val text: String,
        val id: String
    ) : InlineSpan
}

object InlineMarkdownParser {

    private val ASCII_PUNCTUATION = setOf(
        '!', '"', '#', '$', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.', '/',
        ':', ';', '<', '=', '>', '?', '@', '[', '\\', ']', '^', '_', '`', '{', '|', '}', '~'
    )

    private val EMOJI_SHORTCODES = mapOf(
        "rocket" to "🚀",
        "sparkles" to "✨",
        "star" to "⭐",
        "star2" to "🌟",
        "fire" to "🔥",
        "warning" to "⚠️",
        "bulb" to "💡",
        "memo" to "📝",
        "pencil" to "📝",
        " pencil2" to "✏️",
        "white_check_mark" to "✅",
        "heavy_check_mark" to "✔️",
        "ballot_box_with_check" to "☑️",
        "x" to "❌",
        "thumbsup" to "👍",
        "+1" to "👍",
        "thumbsdown" to "👎",
        "-1" to "👎",
        "heart" to "❤️",
        "bug" to "🐛",
        "tada" to "🎉",
        "lock" to "🔒",
        "unlock" to "🔓",
        "key" to "🔑",
        "link" to "🔗",
        "eyes" to "👀",
        "construction" to "🚧",
        "zap" to "⚡",
        "wrench" to "🔧",
        "hammer" to "🔨",
        "gear" to "⚙️",
        "package" to "📦",
        "book" to "📖",
        "bookmark" to "🔖",
        "Pushpin" to "📌",
        "pushpin" to "📌",
        "bulb" to "💡",
        "mag" to "🔍",
        "bell" to "🔔",
        "info" to "ℹ️",
        "information_source" to "ℹ️",
        "question" to "❓",
        "exclamation" to "❗",
        "recycle" to "♻️",
        "trash" to "🗑️",
        "wastebasket" to "🗑️",
        "calendar" to "📅",
        "clock" to "🕒",
        "hourglass" to "⏳",
        "checkered_flag" to "🏁",
        "trophy" to "🏆",
        "100" to "💯",
        "clap" to "👏",
        "wave" to "👋",
        "pray" to "🙏",
        "muscle" to "💪",
        "brain" to "🧠",
        "computer" to "💻",
        "iphone" to "📱",
        "Shield" to "🛡️",
        "shield" to "🛡️",
        "art" to "🎨",
        "globe_with_meridians" to "🌐",
        "folder" to "📁",
        "file_folder" to "📁",
        "page_facing_up" to "📄",
        "chart_with_upwards_trend" to "📈",
        "chart_with_downwards_trend" to "📉",
        "bar_chart" to "📊",
        "smile" to "😄",
        "grin" to "😁",
        "joy" to "😂",
        "wink" to "😉",
        "thinking" to "🤔",
        "sunglasses" to "😎",
        "sob" to "😭",
        "sweat_smile" to "😅",
        "party_popper" to "🎉"
    )

    private val IMAGE_REGEX = Regex(
        """^!\[((?:\\.|[^\[\]\\])*)\]\(\s*<?([^\s>)]+)>?(?:\s+(?:["']([^"']*)["']|\(([^)]*)\)))?\s*\)"""
    )

    private val FOOTNOTE_REF_REGEX = Regex("""^\[\^([a-zA-Z0-9_-]+)\](?!:)""")

    private val LINK_REGEX = Regex(
        """^\[((?:\\.|[^\[\]\\])*)\]\(\s*<?([^\s>)]+)>?(?:\s+(?:["']([^"']*)["']|\(([^)]*)\)))?\s*\)"""
    )

    private val ANGLE_URL_AUTOLINK_REGEX = Regex("""^<((?:https?|ftp)://[^\s>]+)>""", RegexOption.IGNORE_CASE)
    private val ANGLE_EMAIL_AUTOLINK_REGEX = Regex("""^<([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,})>""")
    private val BARE_AUTOLINK_REGEX = Regex("""^(?:https?://|www\.)[^\s<]+""", RegexOption.IGNORE_CASE)

    private val HTML_TAG_REGEX = Regex(
        """^<(kbd|sup|sub|mark|ins|u|del|s)>(.*?)</\1>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val HTML_BR_REGEX = Regex("""^<br\s*/?>""", RegexOption.IGNORE_CASE)

    private val BOLD_ITALIC_ASTERISK_REGEX = Regex("""^\*\*\*(?=\S)(.+?)(?<=\S)\*\*\*|^\*\*\*(\S)\*\*\*""")
    private val BOLD_ITALIC_MIXED_1_REGEX = Regex("""^\*\*_(?=\S)(.+?)(?<=\S)_\*\*|^\*\*_(\S)_\*\*""")
    private val BOLD_ITALIC_MIXED_2_REGEX = Regex("""^__\*(?=\S)(.+?)(?<=\S)\*__|^__\*(\S)\*__""")
    private val BOLD_ITALIC_UNDERSCORE_REGEX = Regex("""^___(?=\S)(.+?)(?<=\S)___|^___(\S)___""")

    private val BOLD_ASTERISK_REGEX = Regex("""^\*\*(?=\S)(.+?)(?<=\S)\*\*|^\*\*(\S)\*\*""")
    private val BOLD_UNDERSCORE_REGEX = Regex("""^__(?=\S)(.+?)(?<=\S)__|^__(\S)__""")

    private val STRIKETHROUGH_DOUBLE_REGEX = Regex("""^~~(?=\S)(.+?)(?<=\S)~~|^~~(\S)~~""")
    private val STRIKETHROUGH_SINGLE_REGEX = Regex("""^~(?=\S)([^~\s][^~]*?)(?<=\S)~(?!~)|^~([^~\s])~(?!~)""")

    private val HIGHLIGHT_REGEX = Regex("""^==(?=\S)(.+?)(?<=\S)==|^==(\S)===""")

    private val ITALIC_ASTERISK_REGEX = Regex("""^\*(?=\S)([^*]+?)(?<=\S)\*|^\*(\S)\*""")
    private val ITALIC_UNDERSCORE_REGEX = Regex("""^_(?=\S)([^_]+?)(?<=\S)_|^_(\S)_""")

    private val EMOJI_REGEX = Regex("""^:([a-zA-Z0-9_+-]+):""")

    fun parse(input: String): List<InlineSpan> {
        if (input.isEmpty()) return emptyList()

        val spans = mutableListOf<InlineSpan>()
        val normalBuffer = StringBuilder()
        var cursor = 0
        val length = input.length

        fun flushNormal() {
            if (normalBuffer.isNotEmpty()) {
                spans.add(InlineSpan.Normal(normalBuffer.toString()))
                normalBuffer.clear()
            }
        }

        while (cursor < length) {
            val ch = input[cursor]
            val prevChar = if (cursor > 0) input[cursor - 1] else ' '
            val slice = input.substring(cursor)

            // 1. Backslash escape
            if (ch == '\\' && cursor + 1 < length && input[cursor + 1] in ASCII_PUNCTUATION) {
                normalBuffer.append(input[cursor + 1])
                cursor += 2
                continue
            }

            // 2. Inline code (`...` or ``...``)
            if (ch == '`') {
                var tickCount = 0
                while (cursor + tickCount < length && input[cursor + tickCount] == '`') {
                    tickCount++
                }
                val delimiter = "`".repeat(tickCount)
                val contentStart = cursor + tickCount
                val closeIdx = findClosingBackticks(input, contentStart, tickCount)
                if (closeIdx != -1) {
                    var codeContent = input.substring(contentStart, closeIdx)
                    if (tickCount >= 2 &&
                        codeContent.length >= 2 &&
                        codeContent.startsWith(' ') &&
                        codeContent.endsWith(' ') &&
                        codeContent.any { it != ' ' }
                    ) {
                        codeContent = codeContent.substring(1, codeContent.length - 1)
                    }
                    if (codeContent.isNotEmpty()) {
                        flushNormal()
                        spans.add(InlineSpan.InlineCode(codeContent))
                        cursor = closeIdx + tickCount
                        continue
                    } else {
                        normalBuffer.append(delimiter).append(delimiter)
                        cursor = closeIdx + tickCount
                        continue
                    }
                } else {
                    normalBuffer.append(delimiter)
                    cursor += tickCount
                    continue
                }
            }

            // 3. Image ![alt](url "title")
            if (ch == '!' && cursor + 1 < length && input[cursor + 1] == '[') {
                val match = IMAGE_REGEX.find(slice)
                if (match != null) {
                    val altRaw = match.groupValues[1]
                    val url = match.groupValues[2]
                    val title = match.groupValues[3].ifEmpty { match.groupValues[4] }.takeIf { it.isNotEmpty() }
                    flushNormal()
                    spans.add(InlineSpan.Image(text = cleanNestedText(altRaw), url = url, title = title))
                    cursor += match.value.length
                    continue
                }
            }

            // 4. Footnote Reference [^id] and Inline Link [text](url "title")
            if (ch == '[') {
                val fnMatch = FOOTNOTE_REF_REGEX.find(slice)
                if (fnMatch != null) {
                    val fnId = fnMatch.groupValues[1]
                    flushNormal()
                    spans.add(InlineSpan.FootnoteReference(text = "[$fnId]", id = fnId))
                    cursor += fnMatch.value.length
                    continue
                }

                val linkMatch = LINK_REGEX.find(slice)
                if (linkMatch != null) {
                    val rawText = linkMatch.groupValues[1]
                    val url = linkMatch.groupValues[2]
                    val title = linkMatch.groupValues[3].ifEmpty { linkMatch.groupValues[4] }.takeIf { it.isNotEmpty() }
                    flushNormal()
                    val display = cleanNestedText(rawText).ifEmpty { url }
                    spans.add(InlineSpan.Link(text = display, url = url, title = title))
                    cursor += linkMatch.value.length
                    continue
                }
            }

            // 5. Angle-bracket Autolinks & Inline HTML GFM tags
            if (ch == '<') {
                val brMatch = HTML_BR_REGEX.find(slice)
                if (brMatch != null) {
                    normalBuffer.append("\n")
                    cursor += brMatch.value.length
                    continue
                }

                val urlAutoMatch = ANGLE_URL_AUTOLINK_REGEX.find(slice)
                if (urlAutoMatch != null) {
                    val url = urlAutoMatch.groupValues[1]
                    flushNormal()
                    spans.add(InlineSpan.Link(text = url, url = url))
                    cursor += urlAutoMatch.value.length
                    continue
                }

                val emailAutoMatch = ANGLE_EMAIL_AUTOLINK_REGEX.find(slice)
                if (emailAutoMatch != null) {
                    val email = emailAutoMatch.groupValues[1]
                    flushNormal()
                    spans.add(InlineSpan.Link(text = email, url = "mailto:$email"))
                    cursor += emailAutoMatch.value.length
                    continue
                }

                val htmlMatch = HTML_TAG_REGEX.find(slice)
                if (htmlMatch != null) {
                    val tag = htmlMatch.groupValues[1].lowercase()
                    val inner = cleanNestedText(htmlMatch.groupValues[2])
                    if (inner.isNotEmpty()) {
                        flushNormal()
                        val span = when (tag) {
                            "kbd" -> InlineSpan.KeyboardKey(inner)
                            "sup" -> InlineSpan.Superscript(inner)
                            "sub" -> InlineSpan.Subscript(inner)
                            "mark" -> InlineSpan.Highlight(inner)
                            "ins", "u" -> InlineSpan.Underline(inner)
                            "del", "s" -> InlineSpan.Strikethrough(inner)
                            else -> InlineSpan.Normal(inner)
                        }
                        spans.add(span)
                        cursor += htmlMatch.value.length
                        continue
                    }
                }
            }

            // 6. Bare GFM Autolinks (https://..., http://..., www....)
            if ((ch == 'h' || ch == 'H' || ch == 'w' || ch == 'W') &&
                (cursor == 0 || prevChar.isWhitespace() || prevChar in charArrayOf('(', '[', '*', '_', '~', '>'))
            ) {
                val bareMatch = BARE_AUTOLINK_REGEX.find(slice)
                if (bareMatch != null) {
                    val cleanUrl = trimAutolinkTrailingPunctuation(bareMatch.value)
                    if (cleanUrl.isNotEmpty()) {
                        val href = if (cleanUrl.startsWith("www.", ignoreCase = true)) {
                            "https://$cleanUrl"
                        } else {
                            cleanUrl
                        }
                        flushNormal()
                        spans.add(InlineSpan.Link(text = cleanUrl, url = href))
                        cursor += cleanUrl.length
                        continue
                    }
                }
            }

            // 7. Asterisk Emphasis (***bold italic***, **_bold italic_**, **bold**, *italic*)
            if (ch == '*') {
                val biMatch = BOLD_ITALIC_ASTERISK_REGEX.find(slice)
                    ?: BOLD_ITALIC_MIXED_1_REGEX.find(slice)
                if (biMatch != null) {
                    val content = biMatch.groupValues[1].ifEmpty { biMatch.groupValues[2] }
                    if (content.isNotEmpty()) {
                        flushNormal()
                        spans.add(InlineSpan.BoldItalic(cleanNestedText(content)))
                        cursor += biMatch.value.length
                        continue
                    }
                }

                val boldMatch = BOLD_ASTERISK_REGEX.find(slice)
                if (boldMatch != null) {
                    val content = boldMatch.groupValues[1].ifEmpty { boldMatch.groupValues[2] }
                    if (content.isNotEmpty()) {
                        flushNormal()
                        spans.add(InlineSpan.Bold(cleanNestedText(content)))
                        cursor += boldMatch.value.length
                        continue
                    }
                }

                val italicMatch = ITALIC_ASTERISK_REGEX.find(slice)
                if (italicMatch != null) {
                    val content = italicMatch.groupValues[1].ifEmpty { italicMatch.groupValues[2] }
                    if (content.isNotEmpty()) {
                        flushNormal()
                        spans.add(InlineSpan.Italic(cleanNestedText(content)))
                        cursor += italicMatch.value.length
                        continue
                    }
                }
            }

            // 8. Underscore Emphasis (___bold italic___, __*bold italic*__, __bold__, _italic_)
            // Enforces GFM intraword underscore rule: cannot open after or close before alphanumeric characters
            if (ch == '_' && !prevChar.isLetterOrDigit()) {
                val biUnderMatch = BOLD_ITALIC_UNDERSCORE_REGEX.find(slice)
                    ?: BOLD_ITALIC_MIXED_2_REGEX.find(slice)
                if (biUnderMatch != null) {
                    val afterIdx = cursor + biUnderMatch.value.length
                    val nextChar = if (afterIdx < length) input[afterIdx] else ' '
                    if (!nextChar.isLetterOrDigit()) {
                        val content = biUnderMatch.groupValues[1].ifEmpty { biUnderMatch.groupValues[2] }
                        if (content.isNotEmpty()) {
                            flushNormal()
                            spans.add(InlineSpan.BoldItalic(cleanNestedText(content)))
                            cursor += biUnderMatch.value.length
                            continue
                        }
                    }
                }

                val boldUnderMatch = BOLD_UNDERSCORE_REGEX.find(slice)
                if (boldUnderMatch != null) {
                    val afterIdx = cursor + boldUnderMatch.value.length
                    val nextChar = if (afterIdx < length) input[afterIdx] else ' '
                    if (!nextChar.isLetterOrDigit()) {
                        val content = boldUnderMatch.groupValues[1].ifEmpty { boldUnderMatch.groupValues[2] }
                        if (content.isNotEmpty()) {
                            flushNormal()
                            spans.add(InlineSpan.Bold(cleanNestedText(content)))
                            cursor += boldUnderMatch.value.length
                            continue
                        }
                    }
                }

                val italicUnderMatch = ITALIC_UNDERSCORE_REGEX.find(slice)
                if (italicUnderMatch != null) {
                    val afterIdx = cursor + italicUnderMatch.value.length
                    val nextChar = if (afterIdx < length) input[afterIdx] else ' '
                    if (!nextChar.isLetterOrDigit()) {
                        val content = italicUnderMatch.groupValues[1].ifEmpty { italicUnderMatch.groupValues[2] }
                        if (content.isNotEmpty()) {
                            flushNormal()
                            spans.add(InlineSpan.Italic(cleanNestedText(content)))
                            cursor += italicUnderMatch.value.length
                            continue
                        }
                    }
                }
            }

            // 9. Strikethrough (~~text~~ or ~text~)
            if (ch == '~') {
                val doubleStrike = STRIKETHROUGH_DOUBLE_REGEX.find(slice)
                if (doubleStrike != null) {
                    val content = doubleStrike.groupValues[1].ifEmpty { doubleStrike.groupValues[2] }
                    if (content.isNotEmpty()) {
                        flushNormal()
                        spans.add(InlineSpan.Strikethrough(cleanNestedText(content)))
                        cursor += doubleStrike.value.length
                        continue
                    }
                }

                if (prevChar != '~') {
                    val singleStrike = STRIKETHROUGH_SINGLE_REGEX.find(slice)
                    if (singleStrike != null) {
                        val content = singleStrike.groupValues[1].ifEmpty { singleStrike.groupValues[2] }
                        if (content.isNotEmpty()) {
                            flushNormal()
                            spans.add(InlineSpan.Strikethrough(cleanNestedText(content)))
                            cursor += singleStrike.value.length
                            continue
                        }
                    }
                }
            }

            // 10. Highlight (==text==)
            if (ch == '=') {
                val highlightMatch = HIGHLIGHT_REGEX.find(slice)
                if (highlightMatch != null) {
                    val content = highlightMatch.groupValues[1].ifEmpty { highlightMatch.groupValues[2] }
                    if (content.isNotEmpty()) {
                        flushNormal()
                        spans.add(InlineSpan.Highlight(cleanNestedText(content)))
                        cursor += highlightMatch.value.length
                        continue
                    }
                }
            }

            // 11. GFM Emoji Shortcodes (:rocket:, :warning:, etc.)
            if (ch == ':') {
                val emojiMatch = EMOJI_REGEX.find(slice)
                if (emojiMatch != null) {
                    val code = emojiMatch.groupValues[1].lowercase()
                    val emoji = EMOJI_SHORTCODES[code]
                    if (emoji != null) {
                        normalBuffer.append(emoji)
                        cursor += emojiMatch.value.length
                        continue
                    }
                }
            }

            normalBuffer.append(ch)
            cursor++
        }

        flushNormal()
        return spans.ifEmpty { listOf(InlineSpan.Normal(input)) }
    }

    fun toPlainText(input: String): String {
        return parse(input).joinToString("") { it.text }
    }

    private fun findClosingBackticks(input: String, startIndex: Int, tickCount: Int): Int {
        var i = startIndex
        val len = input.length
        while (i < len) {
            if (input[i] == '`') {
                var run = 0
                while (i + run < len && input[i + run] == '`') {
                    run++
                }
                if (run == tickCount) {
                    return i
                }
                i += run
            } else {
                i++
            }
        }
        return -1
    }

    private fun trimAutolinkTrailingPunctuation(rawUrl: String): String {
        var url = rawUrl
        while (url.isNotEmpty()) {
            val last = url.last()
            if (last in charArrayOf('.', ',', ';', ':', '!', '?', '\'', '"')) {
                url = url.dropLast(1)
            } else if (last == ')') {
                val openCount = url.count { it == '(' }
                val closeCount = url.count { it == ')' }
                if (closeCount > openCount) {
                    url = url.dropLast(1)
                } else {
                    break
                }
            } else {
                break
            }
        }
        return url
    }

    private fun cleanNestedText(raw: String): String {
        if (raw.isEmpty()) return raw
        val unescaped = buildString {
            var i = 0
            while (i < raw.length) {
                if (raw[i] == '\\' && i + 1 < raw.length && raw[i + 1] in ASCII_PUNCTUATION) {
                    append(raw[i + 1])
                    i += 2
                } else if (raw[i] == ':') {
                    val m = EMOJI_REGEX.find(raw.substring(i))
                    val emoji = m?.let { EMOJI_SHORTCODES[it.groupValues[1].lowercase()] }
                    if (m != null && emoji != null) {
                        append(emoji)
                        i += m.value.length
                    } else {
                        append(raw[i])
                        i++
                    }
                } else {
                    append(raw[i])
                    i++
                }
            }
        }
        // Strip nested emphasis markers if present inside link/heading/outer spans
        return unescaped
            .replace(Regex("""\*\*\*(?=\S)(.+?)(?<=\S)\*\*\*"""), "$1")
            .replace(Regex("""\*\*(?=\S)(.+?)(?<=\S)\*\*"""), "$1")
            .replace(Regex("""__(?=\S)(.+?)(?<=\S)__"""), "$1")
            .replace(Regex("""~~(?=\S)(.+?)(?<=\S)~~"""), "$1")
            .replace(Regex("""\*(?=\S)([^*]+?)(?<=\S)\*"""), "$1")
            .replace(Regex("""`([^`]+)`"""), "$1")
    }
}
