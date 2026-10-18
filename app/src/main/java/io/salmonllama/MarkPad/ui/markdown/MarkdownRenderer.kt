package io.salmonllama.MarkPad.ui.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout. IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownRenderer(
    markdown: String,
    onTaskToggle: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val nodes = remember(markdown) { MarkdownParser.parse(markdown) }
    val linkColor = MaterialTheme.colorScheme.primary

    Column(modifier = modifier) {
        nodes.forEach { node ->
            when (node) {
                is MarkdownNode.Header -> {
                    val style = when (node.level) {
                        1 -> MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
                        2 -> MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                        3 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                        4 -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        5 -> MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        else -> MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium)
                    }
                    val headerColor = if (node.level >= 6) {
                        MaterialTheme.colorScheme.outline
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                    Column(modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)) {
                        Text(
                            text = formatInlineMarkdown(node.text, linkColor),
                            style = style,
                            color = headerColor
                        )
                        if (node.level == 1 || node.level == 2) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
                is MarkdownNode.Paragraph -> {
                    Text(
                        text = formatInlineMarkdown(node.text, linkColor),
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                is MarkdownNode.BulletItem -> {
                    val bulletGlyph = when (node.indentLevel % 3) {
                        0 -> "•"
                        1 -> "◦"
                        else -> "▪"
                    }
                    val startPad = (8 + node.indentLevel * 16).dp
                    Row(modifier = Modifier.padding(start = startPad, top = 2.dp, bottom = 2.dp)) {
                        Text(
                            text = bulletGlyph,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = formatInlineMarkdown(node.text, linkColor),
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is MarkdownNode.OrderedItem -> {
                    val startPad = (8 + node.indentLevel * 16).dp
                    Row(modifier = Modifier.padding(start = startPad, top = 2.dp, bottom = 2.dp)) {
                        Text(
                            text = "${node.number}.",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = formatInlineMarkdown(node.text, linkColor),
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is MarkdownNode.TaskItem -> {
                    val startPad = (4 + node.indentLevel * 16).dp
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = startPad)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onTaskToggle(node.lineIndex) }
                            .padding(start = 4.dp, top = 4.dp, bottom = 4.dp, end = 8.dp)
                    ) {
                        Checkbox(
                            checked = node.isChecked,
                            onCheckedChange = { onTaskToggle(node.lineIndex) },
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatInlineMarkdown(node.text, linkColor),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                lineHeight = 22.sp,
                                textDecoration = if (node.isChecked) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            color = if (node.isChecked) {
                                MaterialTheme.colorScheme.outline
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
                is MarkdownNode.CodeBlock -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                    ) {
                        Column {
                            if (node.language.isNotBlank()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = node.language,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    thickness = 0.5.dp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = node.code,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
                is MarkdownNode.Blockquote -> {
                    Row(
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                    ) {
                        repeat(node.depth.coerceIn(1, 5)) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.75f))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatInlineMarkdown(node.text, linkColor),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontStyle = FontStyle.Italic,
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
                is MarkdownNode.AlertBlock -> {
                    MarkdownAlertCard(node = node, linkColor = linkColor)
                }
                is MarkdownNode.DetailsBlock -> {
                    MarkdownDetailsCard(
                        node = node,
                        linkColor = linkColor,
                        onTaskToggle = onTaskToggle
                    )
                }
                is MarkdownNode.FootnoteDefinition -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 2.dp)
                    ) {
                        Text(
                            text = "[${node.id}]: ",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = formatInlineMarkdown(node.text, linkColor),
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is MarkdownNode.HorizontalRule -> {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                }
                is MarkdownNode.Table -> {
                    MarkdownTable(node = node, linkColor = linkColor)
                }
            }
        }
    }
}

@Composable
private fun MarkdownAlertCard(
    node: MarkdownNode.AlertBlock,
    linkColor: Color
) {
    val (accentColor, iconPrefix) = when (node.type) {
        AlertType.NOTE -> Color(0xFF2F81F7) to "ℹ️"
        AlertType.TIP -> Color(0xFF3FB950) to "💡"
        AlertType.IMPORTANT -> Color(0xFFA371F7) to "💬"
        AlertType.WARNING -> Color(0xFFD29922) to "⚠️"
        AlertType.CAUTION -> Color(0xFFF85149) to "🛑"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(accentColor.copy(alpha = 0.10f))
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .height(IntrinsicSize.Min)
    ) {
        Box(
            modifier = Modifier
                .width(5.dp)
                .fillMaxHeight()
                .background(accentColor)
        )
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = "$iconPrefix ${node.type.displayName}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = accentColor
            )
            if (node.text.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatInlineMarkdown(node.text, linkColor),
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MarkdownDetailsCard(
    node: MarkdownNode.DetailsBlock,
    linkColor: Color,
    onTaskToggle: (Int) -> Unit
) {
    var expanded by remember(node) { mutableStateOf(node.isOpenByDefault) }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = if (expanded) "▾ " else "▸ ",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formatInlineMarkdown(node.summary, linkColor),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (expanded && node.body.isNotBlank()) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    thickness = 0.5.dp
                )
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    MarkdownRenderer(
                        markdown = node.body,
                        onTaskToggle = onTaskToggle
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkdownTable(
    node: MarkdownNode.Table,
    linkColor: Color,
    modifier: Modifier = Modifier
) {
    if (node.headers.isEmpty()) return

    // Calculate consistent column widths using plain-text length so markdown syntax doesn't inflate columns
    val colWidths = remember(node) {
        node.headers.indices.map { colIdx ->
            val headerLen = InlineMarkdownParser.toPlainText(node.headers[colIdx]).length
            val maxRowLen = node.rows.maxOfOrNull {
                InlineMarkdownParser.toPlainText(it.cells.getOrElse(colIdx) { "" }).length
            } ?: 0
            val maxChars = maxOf(headerLen, maxRowLen)
            (maxChars * 9 + 48).coerceIn(110, 340).dp
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .horizontalScroll(rememberScrollState())
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp)
            )
        ) {
            Column {
                // Table Header
                Row(
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    node.headers.forEachIndexed { colIdx, headerText ->
                        val alignment = node.alignments.getOrElse(colIdx) { TableAlignment.LEFT }
                        val textAlign = when (alignment) {
                            TableAlignment.LEFT -> TextAlign.Start
                            TableAlignment.CENTER -> TextAlign.Center
                            TableAlignment.RIGHT -> TextAlign.End
                        }

                        Box(
                            modifier = Modifier
                                .width(colWidths[colIdx])
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = formatInlineMarkdown(headerText, linkColor),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = textAlign,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

                // Table Body Rows
                node.rows.forEachIndexed { rowIndex, row ->
                    val rowBg = if (rowIndex % 2 == 1) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }

                    Row(
                        modifier = Modifier.background(rowBg)
                    ) {
                        node.headers.indices.forEach { colIdx ->
                            val cellText = row.cells.getOrElse(colIdx) { "" }
                            val alignment = node.alignments.getOrElse(colIdx) { TableAlignment.LEFT }
                            val textAlign = when (alignment) {
                                TableAlignment.LEFT -> TextAlign.Start
                                TableAlignment.CENTER -> TextAlign.Center
                                TableAlignment.RIGHT -> TextAlign.End
                            }

                            Box(
                                modifier = Modifier
                                    .width(colWidths[colIdx])
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = formatInlineMarkdown(cellText, linkColor),
                                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = textAlign,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    if (rowIndex < node.rows.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}

internal fun formatInlineMarkdown(
    input: String,
    linkColor: Color = Color(0xFF2F81F7)
): AnnotatedString {
    return buildAnnotatedString {
        val spans = InlineMarkdownParser.parse(input)
        for (span in spans) {
            when (span) {
                is InlineSpan.Bold -> {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(span.text)
                    pop()
                }
                is InlineSpan.Italic -> {
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(span.text)
                    pop()
                }
                is InlineSpan.BoldItalic -> {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic))
                    append(span.text)
                    pop()
                }
                is InlineSpan.Strikethrough -> {
                    pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                    append(span.text)
                    pop()
                }
                is InlineSpan.Underline -> {
                    pushStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                    append(span.text)
                    pop()
                }
                is InlineSpan.Highlight -> {
                    pushStyle(
                        SpanStyle(
                            background = Color(0x44FFD600)
                        )
                    )
                    append(span.text)
                    pop()
                }
                is InlineSpan.InlineCode -> {
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0x22888888),
                            fontSize = 13.sp
                        )
                    )
                    append(" ${span.text} ")
                    pop()
                }
                is InlineSpan.KeyboardKey -> {
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0x33888888),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    append(" [${span.text}] ")
                    pop()
                }
                is InlineSpan.Superscript -> {
                    pushStyle(
                        SpanStyle(
                            baselineShift = BaselineShift.Superscript,
                            fontSize = 11.sp
                        )
                    )
                    append(span.text)
                    pop()
                }
                is InlineSpan.Subscript -> {
                    pushStyle(
                        SpanStyle(
                            baselineShift = BaselineShift.Subscript,
                            fontSize = 11.sp
                        )
                    )
                    append(span.text)
                    pop()
                }
                is InlineSpan.FootnoteReference -> {
                    pushStyle(
                        SpanStyle(
                            baselineShift = BaselineShift.Superscript,
                            fontSize = 11.sp,
                            color = linkColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    append(span.text)
                    pop()
                }
                is InlineSpan.Link -> {
                    pushLink(
                        LinkAnnotation.Url(
                            url = span.url,
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    color = linkColor,
                                    textDecoration = TextDecoration.Underline
                                )
                            )
                        )
                    )
                    append(span.text)
                    pop()
                }
                is InlineSpan.Image -> {
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            color = linkColor,
                            background = Color(0x18888888),
                            fontSize = 12.sp
                        )
                    )
                    val label = span.text.ifBlank { span.url }
                    append(" [🖼 $label] ")
                    pop()
                }
                is InlineSpan.Normal -> {
                    append(span.text)
                }
            }
        }
    }
}
