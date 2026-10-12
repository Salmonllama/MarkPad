package com.notedown.markpad.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.notedown.markpad.model.AppTheme
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class ThemeAccessibilityTest {

    private fun calculateLuminance(color: Color): Double {
        val argb = color.toArgb()
        val r = ((argb shr 16) and 0xFF) / 255.0
        val g = ((argb shr 8) and 0xFF) / 255.0
        val b = (argb and 0xFF) / 255.0

        fun linearize(channel: Double): Double {
            return if (channel <= 0.04045) {
                channel / 12.92
            } else {
                ((channel + 0.055) / 1.055).pow(2.4)
            }
        }

        return 0.2126 * linearize(r) + 0.7152 * linearize(g) + 0.0722 * linearize(b)
    }

    private fun calculateContrastRatio(colorA: Color, colorB: Color): Double {
        val lumA = calculateLuminance(colorA)
        val lumB = calculateLuminance(colorB)
        val lighter = max(lumA, lumB)
        val darker = min(lumA, lumB)
        return (lighter + 0.05) / (darker + 0.05)
    }

    @Test
    fun allThemes_meetWcagAccessibilityContrastStandards() {
        for (theme in AppTheme.entries) {
            val scheme = getColorSchemeForTheme(theme)

            // Text on Surface: Must be >= 4.5:1 (WCAG AA normal text)
            val textOnSurfaceContrast = calculateContrastRatio(scheme.onSurface, scheme.surface)
            assertTrue(
                "Theme ${theme.name} onSurface ($textOnSurfaceContrast:1) must meet WCAG AA >= 4.5:1",
                textOnSurfaceContrast >= 4.5
            )

            // Text on Background: Must be >= 4.5:1
            val textOnBackgroundContrast = calculateContrastRatio(scheme.onBackground, scheme.background)
            assertTrue(
                "Theme ${theme.name} onBackground ($textOnBackgroundContrast:1) must meet WCAG AA >= 4.5:1",
                textOnBackgroundContrast >= 4.5
            )

            // Text on Primary: Must be >= 4.5:1
            val textOnPrimaryContrast = calculateContrastRatio(scheme.onPrimary, scheme.primary)
            assertTrue(
                "Theme ${theme.name} onPrimary ($textOnPrimaryContrast:1) must meet WCAG AA >= 4.5:1",
                textOnPrimaryContrast >= 4.5
            )

            // Outlines and icons on Surface: Must be >= 3.0:1 (WCAG UI component requirement)
            val outlineOnSurfaceContrast = calculateContrastRatio(scheme.outline, scheme.surface)
            assertTrue(
                "Theme ${theme.name} outline ($outlineOnSurfaceContrast:1) must meet WCAG UI component >= 3.0:1",
                outlineOnSurfaceContrast >= 3.0
            )
        }
    }
}
