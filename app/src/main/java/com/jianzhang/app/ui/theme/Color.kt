package com.jianzhang.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 看门狗：军团（Watch Dogs: Legion）黑客 HUD 配色系统。
 * 深色为默认形态：军团青主强调 + 橙警示支出 + 极光绿收入 + 危险红。
 * 浅色保留同一套 HUD 语言，仅换底色并加深边框。
 */
data class JzPalette(
    val bg: Color,
    val bg1: Color,
    val card: Color,
    val text: Color,
    val text2: Color,
    val text3: Color,
    val accent: Color,
    val accentHover: Color,
    val accentWeak: Color,
    val income: Color,
    val expense: Color,
    val danger: Color,
    val gold: Color,
    val purple: Color,
    val blue: Color,
    val pink: Color,
    val border: Color,
    val borderStrong: Color,
    val isDark: Boolean,
)

/** 浅色：同一套 HUD 语言，浅灰蓝底 + 深蓝文字 */
val LightPalette = JzPalette(
    bg = Color(0xFFDCE7EA),
    bg1 = Color(0xFFE9F1F3),
    card = Color(0xFFF0F7F9),
    text = Color(0xFF0C1D25),
    text2 = Color(0xFF3E5A66),
    text3 = Color(0xFF6E8A96),
    accent = Color(0xFF00A8C6),
    accentHover = Color(0xFF00C2E2),
    accentWeak = Color(0x2600A8C6),
    income = Color(0xFF00A67E),
    expense = Color(0xFFE84A12),
    danger = Color(0xFFE0223F),
    gold = Color(0xFFE8A020),
    purple = Color(0xFF8F4FE8),
    blue = Color(0xFF2F80ED),
    pink = Color(0xFFE8226A),
    border = Color(0x33007A8C),
    borderStrong = Color(0x7300A8C6),
    isDark = false,
)

/** 深色：军团 HUD 主形态 */
val DarkPalette = JzPalette(
    bg = Color(0xFF020508),
    bg1 = Color(0xFF050A10),
    card = Color(0xFF0A1219),
    text = Color(0xFFE6F4F7),
    text2 = Color(0xFF90AAB4),
    text3 = Color(0xFF52707D),
    accent = Color(0xFF00E5FF),
    accentHover = Color(0xFF33EAFF),
    accentWeak = Color(0x1F00E5FF),
    income = Color(0xFF00D6A3),
    expense = Color(0xFFFF5A1F),
    danger = Color(0xFFFF3B5C),
    gold = Color(0xFFFFB020),
    purple = Color(0xFFA35CFF),
    blue = Color(0xFF2F80ED),
    pink = Color(0xFFFF2D78),
    border = Color(0x2400E5FF),
    borderStrong = Color(0x6600E5FF),
    isDark = true,
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

/** 分类图表配色（军团 HUD 色板：青 / 绿 / 橙 / 金 / 紫 / 蓝 / 粉 / 红） */
val CategoryColors = listOf(
    Color(0xFF00E5FF), Color(0xFF00D6A3), Color(0xFFFF5A1F), Color(0xFFFFB020),
    Color(0xFFA35CFF), Color(0xFF2F80ED), Color(0xFFFF2D78), Color(0xFFFF3B5C),
    Color(0xFF00C7BE), Color(0xFF9CE63F), Color(0xFF6E8A96), Color(0xFFFF8FA3),
)