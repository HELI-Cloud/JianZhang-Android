package com.jianzhang.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/** 全局 UI 缩放（对应桌面版 CSS zoom） */
val LocalUiScale = staticCompositionLocalOf { 1f }

fun fontSizeScale(fontSize: String): Float = when (fontSize) {
    "small" -> 0.92f
    "large" -> 1.10f
    "xlarge" -> 1.22f
    else -> 1f
}

@Composable
fun ProvideUiScale(scale: Float, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalUiScale provides scale,
        LocalDensity provides Density(density.density * scale, density.fontScale * scale),
        content = content,
    )
}