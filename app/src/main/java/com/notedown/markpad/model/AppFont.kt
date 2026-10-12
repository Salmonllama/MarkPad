package com.notedown.markpad.model

import androidx.compose.ui.text.font.FontFamily

enum class AppFont(val displayName: String, val fontFamily: FontFamily) {
    SANS_SERIF("Sans-Serif (Modern)", FontFamily.SansSerif),
    SERIF("Serif (Editorial)", FontFamily.Serif),
    MONOSPACE("Monospace (Code)", FontFamily.Monospace),
    CURSIVE("Cursive (Script)", FontFamily.Cursive);

    companion object {
        fun fromNameOrDefault(name: String?, default: AppFont = SANS_SERIF): AppFont {
            if (name == null) return default
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: default
        }
    }
}
