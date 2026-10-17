package com.jianzhang.app.ui

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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import com.jianzhang.app.ui.accounts.AccountsScreen
import com.jianzhang.app.ui.analysis.AnalysisScreen
import com.jianzhang.app.ui.budget.BudgetScreen
import com.jianzhang.app.ui.components.HudBackground
import com.jianzhang.app.ui.components.appShape
import com.jianzhang.app.ui.components.hudGlow
import com.jianzhang.app.ui.ledger.LedgerScreen
import com.jianzhang.app.ui.lock.LockScreen
import com.jianzhang.app.ui.more.MoreScreen
import com.jianzhang.app.ui.overview.OverviewScreen
import com.jianzhang.app.ui.record.RecordSheet
import com.jianzhang.app.ui.recurring.RecurringScreen
import com.jianzhang.app.ui.reminders.RemindersScreen
import com.jianzhang.app.ui.settings.SettingsScreen
import com.jianzhang.app.ui.stats.StatsScreen
import com.jianzhang.app.ui.tags.TagsScreen
import com.jianzhang.app.ui.theme.DarkPalette
import com.jianzhang.app.ui.theme.JzTheme
import com.jianzhang.app.ui.theme.LightPalette
import com.jianzhang.app.ui.theme.LocalFontFamily
import com.jianzhang.app.ui.theme.LocalPalette
import kotlinx.coroutines.delay
import com.jianzhang.app.ui.theme.LocalUiStyle
import com.jianzhang.app.ui.theme.ProvideUiScale
import com.jianzhang.app.ui.theme.fontSizeScale
import com.jianzhang.app.util.currentMonth
import com.jianzhang.app.util.monthLabel

private val PRIMARY = setOf("overview", "ledger", "accounts", "stats", "more")
private val MONTH_ROUTES = setOf("overview", "ledger", "stats", "budget")

@Composable
fun JzApp(app: JzApplication, initialRoute: String? = null) {
    val vm: JzViewModel = viewModel()
    LaunchedEffect(Unit) { if (initialRoute != null) vm.navigate(initialRoute) }
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
                LaunchedEffect(vm.refreshKey, vm.settings) { vm.refreshReminders() }
                LaunchedEffect(Unit) {
                    while (true) {
                        delay(60_000)
                        vm.refreshReminders()
                    }
                }
                Box(Modifier.fillMaxSize()) {
                    HudBackground(Modifier.fillMaxSize())
                    if (vm.authStatus != AuthStatus.UNLOCKED) {
                        LockScreen(vm)
                    } else {
                        Column(Modifier.fillMaxSize()) {
                            if (vm.route in MONTH_ROUTES) {
                                MonthTopBar(vm, Modifier.statusBarsPadding())
                            } else {
                                SecondaryBackBar(vm, Modifier.statusBarsPadding())
                            }
                            Box(Modifier.weight(1f)) {
                                when (vm.route) {
                                    "overview" -> OverviewScreen(vm)
                                    "ledger" -> LedgerScreen(vm)
                                    "accounts" -> AccountsScreen(vm)
                                    "stats" -> StatsScreen(vm)
                                    "more" -> MoreScreen(vm)
                                    "budget" -> BudgetScreen(vm)
                                    "recurring" -> RecurringScreen(vm)
                                    "reminders" -> RemindersScreen(vm)
                                    "tags" -> TagsScreen(vm)
                                    "analysis" -> AnalysisScreen(vm)
                                    "settings" -> SettingsScreen(vm)
                                    "about" -> AboutScreen()
                                    else -> OverviewScreen(vm)
                                }
                            }
                            BottomBar(
                                route = vm.route,
                                reminderCount = vm.reminderCount,
                                onSelect = vm::navigate,
                                onAdd = vm::openAdd,
                                modifier = Modifier.navigationBarsPadding(),
                            )
                        }
                        if (vm.sheetOpen) RecordSheet(vm)
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
                modifier = Modifier.clip(appShape(6.dp)).background(palette.accentWeak).border(1.dp, palette.border, appShape(6.dp))
                    .clickable { vm.goCurrentMonth() }.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        } else {
            Box(Modifier.width(70.dp))
        }
    }
}

@Composable
private fun SecondaryBackBar(vm: JzViewModel, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Row(modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { vm.navigate("more") }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回更多", tint = palette.text2)
        }
        Text("返回更多", color = palette.text3, fontSize = 12.sp)
    }
}

private data class NavItem(val route: String, val label: String, val icon: ImageVector)

private val NAV_ITEMS = listOf(
    NavItem("overview", "概览", Icons.Filled.Dashboard),
    NavItem("ledger", "流水", Icons.AutoMirrored.Filled.List),
    NavItem("accounts", "账户", Icons.Filled.AccountBalanceWallet),
    NavItem("stats", "统计", Icons.Filled.BarChart),
    NavItem("more", "更多", Icons.Filled.MoreHoriz),
)

@Composable
private fun BottomBar(
    route: String,
    reminderCount: Int,
    onSelect: (String) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val activeRoute = if (route in PRIMARY) route else "more"
    Surface(modifier.fillMaxWidth(), color = palette.bg1.copy(alpha = 0.95f), shadowElevation = 12.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NAV_ITEMS.forEach { item ->
                val active = activeRoute == item.route
                Column(
                    Modifier
                        .weight(1f)
                        .clip(appShape(7.dp))
                        .background(if (active) palette.accentWeak else Color.Transparent)
                        .clickable { onSelect(item.route) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Box(Modifier.size(23.dp), contentAlignment = Alignment.Center) {
                        Icon(item.icon, contentDescription = item.label, tint = if (active) palette.accent else palette.text3, modifier = Modifier.size(20.dp))
                        if (item.route == "more" && reminderCount > 0) {
                            Box(
                                Modifier.align(Alignment.TopEnd).size(14.dp).clip(CircleShape).background(palette.expense),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    if (reminderCount > 9) "9" else reminderCount.toString(),
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                    Text(item.label, color = if (active) palette.accent else palette.text3, fontSize = 9.5.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
            Box(
                Modifier
                    .padding(horizontal = 6.dp)
                    .size(50.dp)
                    .shadow(9.dp, appShape(12.dp), ambientColor = palette.accent.copy(alpha = 0.35f))
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
