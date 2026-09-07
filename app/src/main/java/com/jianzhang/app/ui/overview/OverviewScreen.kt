package com.jianzhang.app.ui.overview

import com.jianzhang.app.ui.components.appShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jianzhang.app.model.CategoryTotal
import com.jianzhang.app.model.DayTotal
import com.jianzhang.app.model.MonthAgg
import com.jianzhang.app.model.MonthSummary
import com.jianzhang.app.model.TxType
import com.jianzhang.app.ui.JzViewModel
import com.jianzhang.app.ui.components.CardTitle
import com.jianzhang.app.ui.components.CountUpText
import com.jianzhang.app.ui.components.ChamferShape
import com.jianzhang.app.ui.components.GlassCard
import com.jianzhang.app.ui.components.SegmentedControl
import com.jianzhang.app.ui.components.hudGlow
import com.jianzhang.app.ui.theme.LocalPalette
import com.jianzhang.app.util.formatCents
import com.jianzhang.app.util.lastNMonths
import com.jianzhang.app.util.monthDays
import com.jianzhang.app.util.shortMonth
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun OverviewScreen(vm: JzViewModel) {
    val palette = LocalPalette.current
    var summary by remember { mutableStateOf(MonthSummary()) }
    var trend by remember { mutableStateOf<List<MonthAgg>>(emptyList()) }
    var cats by remember { mutableStateOf<List<CategoryTotal>>(emptyList()) }
    var daily by remember { mutableStateOf<List<DayTotal>>(emptyList()) }
    var catTypeIndex by remember { mutableStateOf(0) }

    LaunchedEffect(vm.month, vm.refreshKey, catTypeIndex) {
        val months = lastNMonths(6, vm.month)
        val type = if (catTypeIndex == 0) TxType.EXPENSE else TxType.INCOME
        coroutineScope {
            launch { summary = vm.repository.monthSummary(vm.month) }
            launch { trend = vm.repository.trend(months) }
            launch { cats = vm.repository.categoryTotals(vm.month, type) }
            launch { daily = vm.repository.dailyTotals(vm.month) }
        }
    }

    val income = summary.incomeCents
    val expense = summary.expenseCents
    val balance = income - expense
    val total = income + expense
    val incomePercent = if (total > 0) ((income.toFloat() / total) * 100).toInt() else 0
    val expensePercent = if (total > 0) ((expense.toFloat() / total) * 100).toInt() else 0
    val balancePercent = if (income > 0) (((income - expense).toFloat() / income) * 100).toInt() else 0
    val months = remember(vm.month) { lastNMonths(6, vm.month) }
    val hasTrend = trend.any { it.totalCents > 0 }
    val dailyTotal = daily.sumOf { it.totalCents }
    val catTotal = cats.sumOf { it.totalCents }
    val catLabel = if (catTypeIndex == 0) "支出" else "收入"

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                "概览",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = palette.text,
                letterSpacing = 1.sp,
                style = TextStyle(shadow = hudGlow(palette.accent, 0.32f, 14f)),
            )
        }

        // 月度统计卡片
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("本月支出", Icons.Filled.ArrowDownward, palette.expense, expense, Modifier.weight(1f))
                StatCard("本月收入", Icons.Filled.ArrowUpward, palette.income, income, Modifier.weight(1f))
                StatCard("本月结余", Icons.Filled.Scale, palette.accent, balance, Modifier.weight(1f))
            }
        }

        // 收支比对（0.2.0 新增）
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                // 顶部渐变条
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Brush.horizontalGradient(listOf(palette.income, palette.accent, palette.expense))),
                )
                Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    CardTitle("收支比对", "本月收入 vs 支出对比分析")
                    CompareBars(income = income, expense = expense)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CompareStatItem("收入占比", "$incomePercent%", palette.income, incomePercent)
                        CompareStatItem("支出占比", "$expensePercent%", palette.expense, expensePercent)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("结余率", fontSize = 13.sp, color = palette.text)
                                Text(
                                    if (balance >= 0) "收入大于支出" else "支出大于收入",
                                    fontSize = 11.sp,
                                    color = palette.text3,
                                )
                            }
                            Text(
                                "${if (balance >= 0) "+" else ""}$balancePercent%",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (balance >= 0) palette.income else palette.expense,
                            )
                        }
                    }
                }
            }
        }

        // 收支趋势
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                CardTitle("收支趋势", "近 6 个月")
                Box(Modifier.padding(top = 12.dp)) {
                    LineTrendChart(
                        labels = months.map { shortMonth(it) },
                        income = months.map { m -> trend.firstOrNull { it.ym == m && it.type == "income" }?.totalCents ?: 0L },
                        expense = months.map { m -> trend.firstOrNull { it.ym == m && it.type == "expense" }?.totalCents ?: 0L },
                    )
                }
                if (!hasTrend) {
                    Text(
                        "近 6 个月暂无数据",
                        fontSize = 12.sp,
                        color = palette.text3,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp),
                    )
                }
            }
        }

        // 每日支出
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                CardTitle("每日支出", "${vm.month} · 合计 ${formatCents(dailyTotal)}")
                Box(Modifier.padding(top = 12.dp)) {
                    DailyBarChart(
                        days = daily.map { it.day.toInt() to it.totalCents },
                        monthDaysCount = monthDays(vm.month),
                    )
                }
                if (dailyTotal <= 0) {
                    Text(
                        "本月暂无支出记录",
                        fontSize = 12.sp,
                        color = palette.text3,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp),
                    )
                }
            }
        }

        // 分类分析
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                CardTitle(
                    "分类分析",
                    "本月$catLabel · 合计 ${formatCents(catTotal)}",
                    trailing = {
                        SegmentedControl(
                            options = listOf("支出", "收入"),
                            selectedIndex = catTypeIndex,
                            onSelect = { catTypeIndex = it },
                            modifier = Modifier.width(150.dp),
                        )
                    },
                )
                Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    if (cats.isEmpty()) {
                        Text("本月暂无${catLabel}数据", fontSize = 12.sp, color = palette.text3)
                    } else {
                        Text("${catLabel}分类", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = palette.text)
                        CategoryBars(cats, Modifier.fillMaxWidth())
                        Text("分类占比", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = palette.text)
                        DonutChart(
                            items = cats,
                            centerTitle = formatCents(catTotal),
                            centerSub = "本月$catLabel",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, icon: ImageVector, tint: Color, valueCents: Long, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    GlassCard(modifier = modifier, corner = 16.dp, contentPadding = 12.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier
                    .size(30.dp)
                    .clip(appShape(7.dp))
                    .background(tint.copy(alpha = 0.14f))
                    .border(1.dp, tint.copy(alpha = 0.25f), appShape(7.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
            }
            Text(label, fontSize = 11.sp, color = palette.text2, letterSpacing = 0.5.sp)
            CountUpText(
                targetCents = valueCents,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = tint,
                    shadow = hudGlow(tint, 0.3f, 8f),
                ),
            )
        }
    }
}

@Composable
private fun CompareStatItem(label: String, value: String, color: Color, percent: Int) {
    val palette = LocalPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 13.sp, color = palette.text)
            Box(Modifier.weight(1f))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
        }
        ShimmerProgress(progress = percent / 100f, color = color)
    }
}