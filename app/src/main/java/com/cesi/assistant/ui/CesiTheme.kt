package com.cesi.assistant.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CesiDarkColors = darkColorScheme(
    primary = Color(0xFF66E3FF),
    onPrimary = Color(0xFF001A20),
    secondary = Color(0xFF8EA7FF),
    onSecondary = Color(0xFF0A1026),
    background = Color(0xFF050A14),
    onBackground = Color(0xFFF4F7FB),
    surface = Color(0xFF0B1220),
    onSurface = Color(0xFFEAF2F8),
    surfaceVariant = Color(0xFF121D2D),
    onSurfaceVariant = Color(0xFFB7C5D6),
    error = Color(0xFFFF8A8A)
)

@Composable
fun CesiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CesiDarkColors,
        content = content
    )
}
