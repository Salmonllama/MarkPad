package com.notedown.markpad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notedown.markpad.model.AppFont
import com.notedown.markpad.model.AppTheme
import com.notedown.markpad.ui.theme.DarkBackground
import com.notedown.markpad.ui.theme.DarkPrimary
import com.notedown.markpad.ui.theme.LightBackground
import com.notedown.markpad.ui.theme.LightPrimary
import com.notedown.markpad.ui.theme.OledBackground
import com.notedown.markpad.ui.theme.OledPrimary
import com.notedown.markpad.ui.theme.PaperBackground
import com.notedown.markpad.ui.theme.PaperPrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsDialog(
    currentTheme: AppTheme,
    currentEditorFont: AppFont,
    currentUiFont: AppFont,
    onThemeSelected: (AppTheme) -> Unit,
    onEditorFontSelected: (AppFont) -> Unit,
    onUiFontSelected: (AppFont) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.width(520.dp),
        icon = {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "Aesthetic & Visual Settings",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Section 1: Themes
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Application Theme",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "Applies full-application styling with WCAG accessible contrast",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    ThemeOptionCard(
                        theme = AppTheme.DARK,
                        previewBg = DarkBackground,
                        previewPrimary = DarkPrimary,
                        isSelected = currentTheme == AppTheme.DARK,
                        onSelect = { onThemeSelected(AppTheme.DARK) }
                    )

                    ThemeOptionCard(
                        theme = AppTheme.LIGHT,
                        previewBg = LightBackground,
                        previewPrimary = LightPrimary,
                        isSelected = currentTheme == AppTheme.LIGHT,
                        onSelect = { onThemeSelected(AppTheme.LIGHT) }
                    )

                    ThemeOptionCard(
                        theme = AppTheme.PAPERGLOW,
                        previewBg = PaperBackground,
                        previewPrimary = PaperPrimary,
                        isSelected = currentTheme == AppTheme.PAPERGLOW,
                        onSelect = { onThemeSelected(AppTheme.PAPERGLOW) }
                    )

                    ThemeOptionCard(
                        theme = AppTheme.OLED,
                        previewBg = OledBackground,
                        previewPrimary = OledPrimary,
                        isSelected = currentTheme == AppTheme.OLED,
                        onSelect = { onThemeSelected(AppTheme.OLED) }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Section 2: Editor Font
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FontDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Editor Font",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "Select typography specifically for writing and editing markdown notes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AppFont.entries.forEach { font ->
                            FilterChip(
                                selected = currentEditorFont == font,
                                onClick = { onEditorFontSelected(font) },
                                label = {
                                    Text(
                                        text = font.displayName,
                                        style = TextStyle(fontFamily = font.fontFamily)
                                    )
                                },
                                leadingIcon = if (currentEditorFont == font) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    // Editor Preview Sample
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Text(
                            text = "The quick brown fox jumps over the lazy dog. 12345",
                            style = TextStyle(
                                fontFamily = currentEditorFont.fontFamily,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Section 3: Interface Font (App UI)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Interface Font (App Shell & Navigation)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Select typography for toolbars, menus, sidebars, and dialog labels",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(AppFont.SANS_SERIF, AppFont.SERIF, AppFont.MONOSPACE).forEach { font ->
                            FilterChip(
                                selected = currentUiFont == font,
                                onClick = { onUiFontSelected(font) },
                                label = {
                                    Text(
                                        text = font.displayName,
                                        style = TextStyle(fontFamily = font.fontFamily)
                                    )
                                },
                                leadingIcon = if (currentUiFont == font) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun ThemeOptionCard(
    theme: AppTheme,
    previewBg: Color,
    previewPrimary: Color,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onSelect),
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect
            )
            Spacer(modifier = Modifier.width(6.dp))

            // Swatch representation
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(previewBg)
                    .border(1.5.dp, previewPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(previewPrimary)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = theme.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = theme.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 11.sp
                )
            }
        }
    }
}
