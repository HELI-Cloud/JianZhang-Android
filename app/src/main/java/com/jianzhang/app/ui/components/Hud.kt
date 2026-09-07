package com.jianzhang.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.jianzhang.app.model.UiStyle
import com.jianzhang.app.ui.theme.LocalPalette
import com.jianzhang.app.ui.theme.LocalUiStyle

/**
 * 切角形状：对应 CSS clip-path
 * `polygon(0 0, calc(100% - c) 0, 100% c, 100% 100%, c 100%, 0 calc(100% - c))`
 */
class ChamferShape(private val cut: Dp) : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val c = with(density) { cut.toPx() }
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width - c, 0f)
            lineTo(size.width, c)
            lineTo(size.width, size.height)
            lineTo(c, size.height)
            lineTo(0f, size.height - c)
            close()
        }
        return Outline.Generic(path)
    }
}

/** 文本辉光：军团 HUD 常见的克制青色光晕 */
fun hudGlow(color: Color, alpha: Float = 0.35f, blur: Float = 14f): Shadow =
    Shadow(color = color.copy(alpha = alpha), offset = Offset.Zero, blurRadius = blur)

/**
 * 全局背景：
 * - 黑客终端：暗角光晕 + 细网格 + 扫描线 + 每 12 秒一条青色光带。
 * - 圆角玻璃：去掉扫描线和切角纹理，只保留柔和大面积渐变。
 */
@Composable
fun HudBackground(modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val glass = LocalUiStyle.current == UiStyle.GLASS.key
    if (glass) {
        Box(modifier.background(palette.bg)) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(
                            palette.accent.copy(alpha = if (palette.isDark) 0.10f else 0.07f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.12f, size.height * 0.10f),
                        radius = size.width * 0.95f,
                    ),
                )
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(
                            palette.expense.copy(alpha = if (palette.isDark) 0.06f else 0.045f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.90f, size.height * 0.88f),
                        radius = size.width * 0.82f,
                    ),
                )
                drawRect(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            palette.bg.copy(alpha = 0.12f),
                            Color.Transparent,
                        ),
                        startY = size.height * 0.02f,
                        endY = size.height * 0.98f,
                    ),
                )
            }
        }
        return
    }

    val infinite = rememberInfiniteTransition(label = "hudSweep")
    val sweep by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Restart),
        label = "sweep",
    )
    Box(modifier.background(palette.bg)) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                Brush.radialGradient(
                    colors = listOf(palette.accent.copy(alpha = if (palette.isDark) 0.07f else 0.055f), Color.Transparent),
                    center = Offset(size.width * 0.15f, size.height * 0.08f),
                    radius = size.width * 0.9f,
                ),
            )
            drawRect(
                Brush.radialGradient(
                    colors = listOf(palette.expense.copy(alpha = if (palette.isDark) 0.05f else 0.045f), Color.Transparent),
                    center = Offset(size.width * 0.92f, size.height * 0.88f),
                    radius = size.width * 0.85f,
                ),
            )

            val grid = 28.dp.toPx()
            val gridColor = palette.border.copy(alpha = 0.045f)
            var gx = 0f
            while (gx <= size.width) {
                drawLine(gridColor, Offset(gx, 0f), Offset(gx, size.height), 1f)
                gx += grid
            }
            var gy = 0f
            while (gy <= size.height) {
                drawLine(gridColor, Offset(0f, gy), Offset(size.width, gy), 1f)
                gy += grid
            }

            val scanColor = if (palette.isDark) Color(0x0D00E5FF) else Color(0x0A007A8C)
            var sy = 0f
            val scanStep = 4.dp.toPx()
            while (sy <= size.height) {
                drawLine(scanColor, Offset(0f, sy), Offset(size.width, sy), 1f)
                sy += scanStep
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            val bandH = size.height * 0.24f
            val top = sweep * (size.height + bandH) - bandH
            drawRect(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        palette.accent.copy(alpha = if (palette.isDark) 0.04f else 0.05f),
                        Color.Transparent,
                    ),
                    startY = top,
                    endY = top + bandH,
                ),
                topLeft = Offset(0f, top),
                size = Size(size.width, bandH),
            )
        }
    }
}
