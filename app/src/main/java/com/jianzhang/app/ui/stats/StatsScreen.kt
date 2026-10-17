package com.jianzhang.app.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jianzhang.app.data.Tag
import com.jianzhang.app.model.CategoryTotal
import com.jianzhang.app.model.MonthAgg
import com.jianzhang.app.model.MonthSummary
import com.jianzhang.app.model.TxType
import com.jianzhang.app.ui.JzViewModel
import com.jianzhang.app.ui.components.ChoiceChips
import com.jianzhang.app.ui.components.FlowRowChips
import com.jianzhang.app.ui.components.JzTextInput
import com.jianzhang.app.ui.components.PageHeader
import com.jianzhang.app.ui.components.SectionLabel
import com.jianzhang.app.ui.components.appShape
import com.jianzhang.app.ui.overview.CategoryBars
import com.jianzhang.app.ui.overview.LineTrendChart
import com.jianzhang.app.ui.overview.MonthlyBarsChart
import com.jianzhang.app.ui.theme.LocalPalette
import com.jianzhang.app.util.daysBetween
import com.jianzhang.app.util.endOfMonth
import com.jianzhang.app.util.formatCents
import com.jianzhang.app.util.isValidDate
import com.jianzhang.app.util.lastNMonths
import com.jianzhang.app.util.monthLabel
import com.jianzhang.app.util.monthsBetween
import com.jianzhang.app.util.shortMonth
import com.jianzhang.app.util.startOfMonth
import kotlinx.coroutines.launch
import java.time.LocalDate

private val PRESETS = listOf("本月", "近 3 月", "近 12 月", "本年", "自定义")

@Composable
fun StatsScreen(vm: JzViewModel) {
    val palette = LocalPalette.current
    var preset by remember { mutableIntStateOf(0) }
    var customStart by remember { mutableStateOf(startOfMonth(vm.month)) }
    var customEnd by remember { mutableStateOf(endOfMonth(vm.month)) }
    var yearAnchor by remember { mutableIntStateOf(LocalDate.now().year) }
    var tagFilter by remember { mutableLongStateOf(0L) }
    var tags by remember { mutableStateOf<List<Tag>>(emptyList()) }
    var summary by remember { mutableStateOf(MonthSummary()) }
    var cats by remember { mutableStateOf<List<CategoryTotal>>(emptyList()) }
    var monthly by remember { mutableStateOf<List<MonthAgg>>(emptyList()) }
    var yearMonthly by remember { mutableStateOf<List<MonthAgg>>(emptyList()) }
    var compare by remember { mutableStateOf(CompareStats()) }
    var error by remember { mutableStateOf<String?>(null) }

    val range = remember(preset, vm.month, customStart, customEnd, yearAnchor) {
        when (preset) {
            0 -> startOfMonth(vm.month) to endOfMonth(vm.month)
            1 -> {
                val ms = lastNMonths(3, vm.month)
                startOfMonth(ms.first()) to endOfMonth(ms.last())
            }
            2 -> {
                val ms = lastNMonths(12, vm.month)
                startOfMonth(ms.first()) to endOfMonth(ms.last())
            }
            3 -> "$yearAnchor-01-01" to "$yearAnchor-12-31"
            else -> customStart to customEnd
        }
    }
    val rangeValid = remember(range) {
        isValidDate(range.first) && isValidDate(range.second) && range.first <= range.second
    }
    val months = remember(range, rangeValid) {
        if (rangeValid) monthsBetween(range.first, range.second) else emptyList()
    }
    val yearMonths = remember(yearAnchor) { (1..12).map { "$yearAnchor-${it.toString().padStart(2, '0')}" } }

    LaunchedEffect(vm.month, vm.refreshKey, preset, tagFilter, yearAnchor, customStart, customEnd) {
        val (start, end) = range
        if (!isValidDate(start) || !isValidDate(end)) {
            error = "请输入有效日期（YYYY-MM-DD）"
            summary = MonthSummary()
            cats = emptyList()
            monthly = emptyList()
            yearMonthly = emptyList()
            compare = CompareStats()
            return@LaunchedEffect
        }
        if (start > end) {
            error = "开始日期不能晚于结束日期"
            summary = MonthSummary()
            cats = emptyList()
            monthly = emptyList()
            yearMonthly = emptyList()
            compare = CompareStats()
            return@LaunchedEffect
        }
        error = null
        val tagId = tagFilter.takeIf { it > 0 }
        runCatching {
            val current = vm.repository.rangeSummary(start, end, tagId)
            val currentCats = vm.repository.categoryTotalsRange(start, end, TxType.EXPENSE, tagId)
            val currentMonthly = vm.repository.monthlyAggregatesRange(start, end, tagId)
            val yearAgg = vm.repository.monthlyAggregatesRange("$yearAnchor-01-01", "$yearAnchor-12-31", tagId)
            val prevMonth = vm.utilShiftMonth(-1)
            val lastYearMonth = "${vm.month.take(4).toInt() - 1}-${vm.month.takeLast(2)}"
            val currentAnchor = vm.repository.rangeSummary(startOfMonth(vm.month), endOfMonth(vm.month), tagId)
            val previous = vm.repository.rangeSummary(startOfMonth(prevMonth), endOfMonth(prevMonth), tagId)
            val lastYear = vm.repository.rangeSummary(startOfMonth(lastYearMonth), endOfMonth(lastYearMonth), tagId)
            summary = current
            cats = currentCats
            monthly = currentMonthly
            yearMonthly = yearAgg
            compare = CompareStats(
                currentExpense = currentAnchor.expenseCents,
                previousExpense = previous.expenseCents,
                lastYearExpense = lastYear.expenseCents,
                currentIncome = currentAnchor.incomeCents,
                previousIncome = previous.incomeCents,
                lastYearIncome = lastYear.incomeCents,
            )
            tags = vm.repository.tags().map { it.tag }
        }.onFailure { error = it.message ?: "统计加载失败" }
    }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { PageHeader("统计", "${range.first} 至 ${range.second}") }
        item {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ChoiceChips(PRESETS, preset, { preset = it })
                if (preset == 4) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        JzTextInput(customStart, { customStart = it.take(10) }, "开始日期", Modifier.weight(1f))
                        JzTextInput(customEnd, { customEnd = it.take(10) }, "结束日期", Modifier.weight(1f))
                    }
                }
                if (tags.isNotEmpty()) {
                    FlowRowChips(
                        options = listOf(0L to "全部标签") + tags.map { it.id to it.name },
                        selectedIds = setOf(tagFilter),
                        onToggle = { tagFilter = it },
                    )
                }
                error?.let { Text(it, color = palette.danger, fontSize = 12.sp) }
            }
        }
        item {
            Row(
                Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(appShape(9.dp))
                    .background(palette.bg1.copy(alpha = 0.72f))
                    .border(1.dp, palette.border, appShape(9.dp))
                    .padding(vertical = 13.dp),
            ) {
                StatCell("区间支出", "¥${formatCents(summary.expenseCents)}", palette.expense, "日均 ¥${formatCents(summary.expenseCents / daysBetween(range.first, range.second).coerceAtLeast(1))}", Modifier.weight(1f))
                StatCell("区间收入", "¥${formatCents(summary.incomeCents)}", palette.income, "${daysBetween(range.first, range.second)} 天", Modifier.weight(1f))
                StatCell("区间结余", "¥${formatCents(summary.incomeCents - summary.expenseCents)}", palette.accent, "收入 − 支出", Modifier.weight(1f))
                StatCell("环比 / 同比", "${delta(compare.currentExpense, compare.previousExpense)} / ${delta(compare.currentExpense, compare.lastYearExpense)}", palette.gold, "较上月 / 去年同月", Modifier.weight(1f))
            }
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                SectionLabel("区间收支趋势", if (months.size > 1) "按所选范围逐月聚合" else "单月范围")
                if (!rangeValid) {
                    Text("请修正日期范围后查看趋势", color = palette.text3, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
                } else if (months.size > 1) {
                    LineTrendChart(
                        labels = months.map(::shortMonth),
                        income = months.map { m -> monthly.firstOrNull { it.ym == m && it.type == "income" }?.totalCents ?: 0L },
                        expense = months.map { m -> monthly.firstOrNull { it.ym == m && it.type == "expense" }?.totalCents ?: 0L },
                    )
                } else {
                    MonthlyBarsChart(
                        labels = listOf(shortMonth(months.first())),
                        income = listOf(summary.incomeCents),
                        expense = listOf(summary.expenseCents),
                    )
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                SectionLabel(
                    "年度总览",
                    "$yearAnchor 年",
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { yearAnchor-- }, modifier = Modifier.width(34.dp)) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "上一年", tint = palette.text2, modifier = Modifier.width(16.dp))
                            }
                            Text(yearAnchor.toString(), color = palette.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            IconButton(onClick = { yearAnchor++ }, modifier = Modifier.width(34.dp)) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, "下一年", tint = palette.text2, modifier = Modifier.width(16.dp))
                            }
                        }
                    },
                )
                MonthlyBarsChart(
                    labels = yearMonths.map { "${it.takeLast(2).toInt()}月" },
                    income = yearMonths.map { m -> yearMonthly.firstOrNull { it.ym == m && it.type == "income" }?.totalCents ?: 0L },
                    expense = yearMonths.map { m -> yearMonthly.firstOrNull { it.ym == m && it.type == "expense" }?.totalCents ?: 0L },
                )
            }
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                SectionLabel("支出分类", "${cats.size} 类")
                if (cats.isEmpty()) Text("该区间暂无支出记录", color = palette.text3, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
                else CategoryBars(cats, Modifier.fillMaxWidth())
            }
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                SectionLabel("对比明细", "锚定 ${monthLabel(vm.month)}")
                CompareRow("本月支出", compare.currentExpense, compare.previousExpense, "上月")
                CompareRow("去年同期支出", compare.currentExpense, compare.lastYearExpense, "同比基数")
                CompareRow("本月收入", compare.currentIncome, compare.previousIncome, "上月")
            }
        }
    }
}

private data class CompareStats(
    val currentExpense: Long = 0,
    val previousExpense: Long = 0,
    val lastYearExpense: Long = 0,
    val currentIncome: Long = 0,
    val previousIncome: Long = 0,
    val lastYearIncome: Long = 0,
)

private fun delta(current: Long, base: Long): String {
    if (base == 0L) return "—"
    val pct = ((current - base) * 100 / base).toInt()
    return if (pct > 0) "+$pct%" else "$pct%"
}

private fun JzViewModel.utilShiftMonth(delta: Int): String {
    val date = LocalDate.parse("$month-01").plusMonths(delta.toLong())
    return "${date.year}-${date.monthValue.toString().padStart(2, '0')}"
}

@Composable
private fun StatCell(label: String, value: String, color: androidx.compose.ui.graphics.Color, foot: String, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Column(modifier.padding(horizontal = 8.dp)) {
        Text(label, color = palette.text3, fontSize = 10.5.sp)
        Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 3.dp))
        Text(foot, color = palette.text3, fontSize = 9.5.sp)
    }
}

@Composable
private fun CompareRow(label: String, current: Long, base: Long, baseLabel: String) {
    val palette = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = palette.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text("¥${formatCents(current)}", color = palette.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("$baseLabel ¥${formatCents(base)}", color = palette.text3, fontSize = 10.5.sp)
        }
        Text(delta(current, base), color = if (current > base) palette.expense else palette.income, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 12.dp))
    }
}
