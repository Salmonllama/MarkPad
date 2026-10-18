package io.salmonllama.MarkPad.ui.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
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
    val nodes = MarkdownParser.parse(markdown)

    Column(modifier = modifier) {
        nodes.forEach { node ->
            when (node) {
                is MarkdownNode.Header -> {
                    val style = when (node.level) {
                        1 -> MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
                        2 -> MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                        else -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium)
                    }
                    Text(
                        text = node.text,
                        style = style,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                is MarkdownNode.Paragraph -> {
                    Text(
                        text = formatInlineMarkdown(node.text),
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                is MarkdownNode.BulletItem -> {
                    Row(modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp)) {
                        Text(
                            text = "•",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = formatInlineMarkdown(node.text),
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is MarkdownNode.TaskItem -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
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
                            text = formatInlineMarkdown(node.text),
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
                is MarkdownNode.Blockquote -> {
                    Row(
                        modifier = Modifier
                            .padding(vertical = 6.dp)
                            .fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(28.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = node.text,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
                is MarkdownNode.HorizontalRule -> {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                }
                is MarkdownNode.Table -> {
                    MarkdownTable(node = node)
                }
            }
        }
    }
}

@Composable
private fun MarkdownTable(
    node: MarkdownNode.Table,
    modifier: Modifier = Modifier
) {
    if (node.headers.isEmpty()) return

    // Calculate consistent column widths so all rows align
    val colWidths = remember(node) {
        node.headers.indices.map { colIdx ->
            val headerLen = node.headers[colIdx].length
            val maxRowLen = node.rows.maxOfOrNull { it.cells.getOrElse(colIdx) { "" }.length } ?: 0
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
                                text = formatInlineMarkdown(headerText),
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
                                    text = formatInlineMarkdown(cellText),
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

private fun formatInlineMarkdown(input: String): AnnotatedString {
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
                is InlineSpan.Normal -> {
                    append(span.text)
                }
            }
        }
    }
}
