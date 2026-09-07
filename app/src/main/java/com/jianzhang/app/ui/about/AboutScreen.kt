package com.jianzhang.app.ui.about

import com.jianzhang.app.ui.components.appShape
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jianzhang.app.BuildConfig
import com.jianzhang.app.ui.components.ChamferShape
import com.jianzhang.app.ui.components.HudBackground
import com.jianzhang.app.ui.components.hudGlow
import com.jianzhang.app.ui.theme.LocalPalette

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val palette = LocalPalette.current
    val dbPath = context.getDatabasePath("jianzhang.db").absolutePath

    // 背景脉冲（军团青光晕呼吸）
    val infinite = rememberInfiniteTransition(label = "hudPulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse",
    )

    Box(Modifier.fillMaxSize()) {
        HudBackground(Modifier.fillMaxSize())

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 英雄卡
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(appShape(16.dp))
                        .background(palette.card.copy(alpha = 0.9f))
                        .border(1.dp, palette.border, appShape(16.dp))
                        .shadow(10.dp, appShape(16.dp), ambientColor = palette.accent.copy(alpha = 0.18f))
                        .padding(vertical = 36.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FloatingLogo()
                    Text(
                        "简账",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Transparent,
                        letterSpacing = 2.sp,
                        style = TextStyle(
                            brush = Brush.linearGradient(listOf(palette.accent, Color(0xFF00C7BE), Color(0xFF90AAB4))),
                            shadow = hudGlow(palette.accent, 0.4f, 18f),
                        ),
                    )
                    Text(
                        "v${BuildConfig.VERSION_NAME}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.accent,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier
                            .clip(appShape(6.dp))
                            .background(palette.accentWeak)
                            .border(1.dp, palette.border, appShape(6.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = palette.accent, modifier = Modifier.size(14.dp))
                        Text(
                            "极简 · 本地 · 私密",
                            fontSize = 15.sp,
                            color = palette.text2,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                        Icon(Icons.Filled.Star, contentDescription = null, tint = palette.accent, modifier = Modifier.size(14.dp))
                    }
                }
            }

            // 信息卡
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoCard("开发者", "HELISEC", Icons.Filled.Person, palette.accent)
                    InfoCard("技术栈", "Kotlin · Jetpack Compose · SQLite", Icons.Filled.Code, palette.purple)
                    InfoCard("开发时间", "2026 年 8 月", Icons.Filled.CalendarMonth, palette.blue)
                    InfoCard("数据存储", dbPath, Icons.Filled.Storage, palette.income)
                }
            }

            // 安全徽章
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(appShape(12.dp))
                        .background(palette.card.copy(alpha = 0.9f))
                        .border(1.dp, palette.border, appShape(12.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        Modifier
                            .clip(appShape(6.dp))
                            .background(palette.income.copy(alpha = 0.12f))
                            .border(1.dp, palette.income.copy(alpha = 0.3f), appShape(6.dp))
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Security, contentDescription = null, tint = palette.income, modifier = Modifier.size(15.dp))
                        Text(
                            "100% 本地存储",
                            color = palette.income,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    Text(
                        "所有数据仅保存在本机，永不上传云端",
                        fontSize = 13.sp,
                        color = palette.text2,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingLogo() {
    val palette = LocalPalette.current
    val infinite = rememberInfiniteTransition(label = "logo")
    val floatY by infinite.animateFloat(
        initialValue = -8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(3000), RepeatMode.Reverse),
        label = "float",
    )
    val ringAlpha by infinite.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "ring",
    )
    Box(contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(110.dp)
                .graphicsLayer { alpha = ringAlpha }
                .background(
                    Brush.radialGradient(listOf(palette.accent.copy(alpha = 0.28f), Color.Transparent)),
                    RoundedCornerShape(20.dp),
                ),
        )
        Box(
            Modifier
                .size(88.dp)
                .graphicsLayer { translationY = floatY }
                .clip(appShape(18.dp))
                .shadow(14.dp, appShape(18.dp), ambientColor = palette.accent.copy(alpha = 0.5f))
                .background(Brush.linearGradient(listOf(palette.accent, Color(0xFF00859C)))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = Color(0xFF00171D), modifier = Modifier.size(40.dp))
        }
    }
}

@Composable
private fun InfoCard(label: String, value: String, icon: ImageVector, accent: Color) {
    val palette = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(appShape(10.dp))
            .background(palette.card.copy(alpha = 0.9f))
            .border(1.dp, palette.border, appShape(10.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(appShape(10.dp))
                .background(palette.bg1)
                .border(1.dp, accent.copy(alpha = 0.35f), appShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.padding(start = 14.dp)) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = palette.text3, letterSpacing = 0.5.sp)
            Text(
                value,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}