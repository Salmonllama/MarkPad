package com.notedown.markpad.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.notedown.markpad.model.AppFont

fun createAppTypography(appFont: AppFont = AppFont.SANS_SERIF): Typography {
    val family = appFont.fontFamily
    return Typography(
        headlineLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 30.sp,
            letterSpacing = 0.sp
        ),
        titleLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 26.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        titleSmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = if (appFont == AppFont.MONOSPACE) FontFamily.Monospace else family,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.sp
        ),
        labelSmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        ),
        labelMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        )
    )
}

val Typography = createAppTypography(AppFont.SANS_SERIF)
