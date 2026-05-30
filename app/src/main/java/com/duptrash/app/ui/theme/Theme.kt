package com.duptrash.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Background = Color(0xFF0D0F14)
private val Surface = Color(0xFF161A22)
private val SurfaceVariant = Color(0xFF1E2430)
private val Border = Color(0xFF2A3040)
private val Accent = Color(0xFF00E5B4)
private val AccentDim = Color(0xFF00A884)
private val Danger = Color(0xFFFF4757)
private val TextPrimary = Color(0xFFE8ECF4)

private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = Background,
    secondary = AccentDim,
    background = Background,
    surface = Surface,
    surfaceVariant = SurfaceVariant,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = Border,
    error = Danger,
)

@Composable
fun DupTrashTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
