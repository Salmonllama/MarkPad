package com.notedown.markpad.ui.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownRenderer(
    markdown: String,
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
