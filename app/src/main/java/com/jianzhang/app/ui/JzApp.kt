package com.jianzhang.app.ui

import com.jianzhang.app.ui.components.appShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jianzhang.app.JzApplication
import com.jianzhang.app.model.AuthStatus
import com.jianzhang.app.ui.about.AboutScreen
import com.jianzhang.app.ui.analysis.AnalysisScreen
import com.jianzhang.app.ui.budget.BudgetScreen
import com.jianzhang.app.ui.components.ChamferShape
import com.jianzhang.app.ui.components.HudBackground
import com.jianzhang.app.ui.components.hudGlow
import com.jianzhang.app.ui.ledger.LedgerScreen
import com.jianzhang.app.ui.lock.LockScreen
import com.jianzhang.app.ui.overview.OverviewScreen
import com.jianzhang.app.ui.record.RecordSheet
import com.jianzhang.app.ui.settings.SettingsScreen
import com.jianzhang.app.ui.theme.DarkPalette
import com.jianzhang.app.ui.theme.JzTheme
import com.jianzhang.app.ui.theme.LightPalette
import com.jianzhang.app.ui.theme.LocalFontFamily
import com.jianzhang.app.ui.theme.LocalUiStyle
import com.jianzhang.app.ui.theme.LocalPalette
import com.jianzhang.app.ui.theme.ProvideUiScale
import com.jianzhang.app.ui.theme.fontSizeScale
import com.jianzhang.app.util.currentMonth
import com.jianzhang.app.util.monthLabel

@Composable
fun JzApp(app: JzApplication) {
    val vm: JzViewModel = viewModel()
    val sysDark = isSystemInDarkTheme()
    val dark = when (vm.settings.themeMode) {
        "light" -> false
        "dark" -> true
        else -> sysDark
    }
    val palette = if (dark) DarkPalette else LightPalette
    JzTheme(darkTheme = dark) {
        ProvideUiScale(fontSizeScale(vm.settings.fontSize)) {
            CompositionLocalProvider(
                    LocalUiStyle provides vm.settings.uiStyle,
                LocalFontFamily provides if (vm.settings.fontFamily == "monospace") FontFamily.Monospace else FontFamily.Default,
            ) {
                Box(Modifier.fillMaxSize()) {
                    HudBackground(Modifier.fillMaxSize())
                    Box(Modifier.fillMaxSize()) {
                        if (vm.authStatus != AuthStatus.UNLOCKED) {
                            // 锁屏 / 首次设置密码：不渲染任何账目数据
                            LockScreen(vm)
                        } else {
                            var tab by rememberSaveable { mutableIntStateOf(0) }
                            Column(Modifier.fillMaxSize()) {
                                MonthTopBar(
                                    vm = vm,
                                    modifier = Modifier.statusBarsPadding(),
                                )
                                Box(Modifier.weight(1f)) {
                                    when (tab) {
                                        0 -> OverviewScreen(vm)
                                        1 -> LedgerScreen(vm)
                                        2 -> BudgetScreen(vm)
                                        3 -> AnalysisScreen(vm)
                                        4 -> SettingsScreen(vm)
                                        5 -> AboutScreen()
                                    }
                                }
                                BottomBar(
                                    tab = tab,
                                    onSelect = { tab = it },
                                    onAdd = vm::openAdd,
                                    modifier = Modifier.navigationBarsPadding(),
                                )
                            }
                            if (vm.sheetOpen) {
                                RecordSheet(vm)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthTopBar(vm: JzViewModel, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Row(
        modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { vm.changeMonth(-1) }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "上个月", tint = palette.text2)
        }
        Text(
            monthLabel(vm.month),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = palette.text,
            style = TextStyle(shadow = hudGlow(palette.accent, 0.3f, 12f)),
            letterSpacing = 1.sp,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        IconButton(onClick = { vm.changeMonth(1) }) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "下个月", tint = palette.text2)
        }
        if (vm.month != currentMonth()) {
            Text(
                "回到本月",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.accent,
                letterSpacing = 0.5.sp,
                modifier = Modifier
                    .clip(appShape(6.dp))
                    .background(palette.accentWeak)
                    .border(1.dp, palette.border, appShape(6.dp))
                    .clickable { vm.goCurrentMonth() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        } else {
            Box(Modifier.width(70.dp))
        }
    }
}

private data class NavItem(val label: String, val icon: ImageVector)

private val NAV_ITEMS = listOf(
    NavItem("概览", Icons.Filled.Dashboard),
    NavItem("流水", Icons.AutoMirrored.Filled.List),
    NavItem("预算", Icons.Filled.Savings),
    NavItem("AI 分析", Icons.Filled.AutoAwesome),
    NavItem("设置", Icons.Filled.Settings),
    NavItem("关于", Icons.Filled.Info),
)

@Composable
private fun BottomBar(tab: Int, onSelect: (Int) -> Unit, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val shape = appShape(6.dp)
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = palette.bg1.copy(alpha = 0.94f),
        shadowElevation = 14.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NAV_ITEMS.forEachIndexed { index, item ->
                val active = tab == index
                Column(
                    Modifier
                        .weight(1f)
                        .clip(shape)
                        .background(if (active) palette.accentWeak else Color.Transparent)
                        .clickable { onSelect(index) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Box(Modifier.fillMaxWidth()) {
                        if (active) {
                            // 激活项左侧橙色指示条
                            Box(
                                Modifier
                                    .align(Alignment.CenterStart)
                                    .width(3.dp)
                                    .height(18.dp)
                                    .background(palette.expense)
                                    .shadow(6.dp, CircleShape, ambientColor = palette.expense.copy(alpha = 0.6f)),
                            )
                        }
                        Icon(
                            item.icon,
                            contentDescription = item.label,
                            tint = if (active) palette.accent else palette.text3,
                            modifier = Modifier.size(21.dp),
                        )
                    }
                    Text(
                        item.label,
                        fontSize = 9.5.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (active) palette.accent else palette.text3,
                        letterSpacing = 0.5.sp,
                    )
                }
            }
            // 记一笔（切角 FAB）
            Box(
                Modifier
                    .padding(horizontal = 6.dp)
                    .size(50.dp)
                    .shadow(10.dp, appShape(12.dp), ambientColor = palette.accent.copy(alpha = 0.45f))
                    .clip(appShape(12.dp))
                    .background(Brush.linearGradient(listOf(palette.accent, palette.accentHover)))
                    .clickable(onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "记一笔", tint = Color(0xFF00171D), modifier = Modifier.size(23.dp))
            }
        }
    }
}
