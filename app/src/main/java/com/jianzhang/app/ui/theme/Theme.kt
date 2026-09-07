package com.jianzhang.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

private val LightColorScheme = lightColorScheme(
    primary = LightPalette.accent,
    onPrimary = Color(0xFF04202A),
    secondary = LightPalette.text2,
    background = LightPalette.bg,
    onBackground = LightPalette.text,
    surface = LightPalette.card,
    onSurface = LightPalette.text,
    surfaceVariant = LightPalette.bg1,
    onSurfaceVariant = LightPalette.text2,
    outline = LightPalette.border,
    outlineVariant = LightPalette.borderStrong,
    error = LightPalette.danger,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPalette.accent,
    onPrimary = Color(0xFF00171D),
    secondary = DarkPalette.text2,
    background = DarkPalette.bg,
    onBackground = DarkPalette.text,
    surface = DarkPalette.card,
    onSurface = DarkPalette.text,
    surfaceVariant = DarkPalette.bg1,
    onSurfaceVariant = DarkPalette.text2,
    outline = DarkPalette.border,
    outlineVariant = DarkPalette.borderStrong,
    error = DarkPalette.danger,
)

val LocalUiStyle = staticCompositionLocalOf { "hacker" }

val LocalFontFamily = staticCompositionLocalOf<FontFamily> { FontFamily.Monospace }

@Composable
fun JzTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val scheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val palette = if (darkTheme) DarkPalette else LightPalette
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
