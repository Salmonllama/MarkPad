package com.notedown.markpad.model

enum class AppTheme(val displayName: String, val description: String) {
    LIGHT("Light", "Clean daylight theme with high contrast"),
    DARK("Dark", "Default slate dark theme"),
    PAPERGLOW("Paperglow", "Warm parchment and sepia tones"),
    OLED("OLED", "Pitch black background optimized for OLED displays");

    companion object {
        fun fromNameOrDefault(name: String?, default: AppTheme = DARK): AppTheme {
            if (name == null) return default
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: default
        }
    }
}
