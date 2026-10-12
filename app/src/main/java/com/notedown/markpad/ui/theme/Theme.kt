package com.notedown.markpad.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.notedown.markpad.model.AppFont
import com.notedown.markpad.model.AppTheme

val LightColorScheme: ColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant
)

val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant
)

val PaperglowColorScheme: ColorScheme = lightColorScheme(
    primary = PaperPrimary,
    onPrimary = PaperOnPrimary,
    primaryContainer = PaperPrimaryContainer,
    onPrimaryContainer = PaperOnPrimaryContainer,
    background = PaperBackground,
    onBackground = PaperOnBackground,
    surface = PaperSurface,
    onSurface = PaperOnSurface,
    surfaceVariant = PaperSurfaceVariant,
    onSurfaceVariant = PaperOnSurfaceVariant,
    outline = PaperOutline,
    outlineVariant = PaperOutlineVariant
)

val OledColorScheme: ColorScheme = darkColorScheme(
    primary = OledPrimary,
    onPrimary = OledOnPrimary,
    primaryContainer = OledPrimaryContainer,
    onPrimaryContainer = OledOnPrimaryContainer,
    background = OledBackground,
    onBackground = OledOnBackground,
    surface = OledSurface,
    onSurface = OledOnSurface,
    surfaceVariant = OledSurfaceVariant,
    onSurfaceVariant = OledOnSurfaceVariant,
    outline = OledOutline,
    outlineVariant = OledOutlineVariant
)

fun getColorSchemeForTheme(appTheme: AppTheme): ColorScheme = when (appTheme) {
    AppTheme.LIGHT -> LightColorScheme
    AppTheme.DARK -> DarkColorScheme
    AppTheme.PAPERGLOW -> PaperglowColorScheme
    AppTheme.OLED -> OledColorScheme
}

@Composable
fun MarkPadTheme(
    appTheme: AppTheme = AppTheme.DARK,
    uiFont: AppFont = AppFont.SANS_SERIF,
    content: @Composable () -> Unit
) {
    val colorScheme = getColorSchemeForTheme(appTheme)
    val typography = createAppTypography(uiFont)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}

@Composable
fun MarkPadTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MarkPadTheme(
        appTheme = if (darkTheme) AppTheme.DARK else AppTheme.LIGHT,
        uiFont = AppFont.SANS_SERIF,
        content = content
    )
}
