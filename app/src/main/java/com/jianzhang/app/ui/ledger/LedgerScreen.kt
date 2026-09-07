package com.jianzhang.app.ui.ledger

import com.jianzhang.app.ui.components.appShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jianzhang.app.data.Category
import com.jianzhang.app.data.Transaction
import com.jianzhang.app.ui.JzViewModel
import com.jianzhang.app.ui.components.EmptyState
import com.jianzhang.app.ui.components.ChamferShape
import com.jianzhang.app.ui.components.GlassCard
import com.jianzhang.app.ui.components.SearchField
import com.jianzhang.app.ui.components.hudGlow
import com.jianzhang.app.ui.components.SegmentedControl
import com.jianzhang.app.ui.theme.LocalPalette
import com.jianzhang.app.util.categoryIcon
import com.jianzhang.app.util.dateLabel
import com.jianzhang.app.util.formatCents

@Composable
fun LedgerScreen(vm: JzViewModel) {
    val palette = LocalPalette.current
    var filterIndex by remember { mutableStateOf(0) }
    var search by remember { mutableStateOf("") }
    var txs by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var cats by remember { mutableStateOf<List<Category>>(emptyList()) }

    LaunchedEffect(vm.month, vm.refreshKey) {
        txs = vm.repository.listByMonth(vm.month)
        cats = vm.repository.categories()
    }

    val catMap = remember(cats) { cats.associateBy { it.id } }
    val filter = when (filterIndex) {
        1 -> "expense"
        2 -> "income"
        else -> "all"
    }
    val q = search.trim()
    val visible = txs.filter { t ->
        (filter == "all" || t.type == filter) &&
            (q.isEmpty() || (catMap[t.categoryId]?.name?.contains(q) == true) || t.note.contains(q))
    }
    val groups = remember(visible) {
        visible.groupBy { it.date }.toSortedMap(compareByDescending { it })
    }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            "流水",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = palette.text,
            letterSpacing = 1.sp,
            style = TextStyle(shadow = hudGlow(palette.accent, 0.32f, 14f)),
            modifier = Modifier.padding(start = 16.dp, top = 8.dp),
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SegmentedControl(
                options = listOf("全部", "支出", "收入"),
                selectedIndex = filterIndex,
                onSelect = { filterIndex = it },
                modifier = Modifier.weight(1f),
            )
            SearchField(value = search, onValueChange = { search = it }, modifier = Modifier.weight(2f))
        }

        if (groups.isEmpty()) {
            EmptyState(
                icon = "📊",
                title = "这个月还没有账目",
                sub = "记录第一笔收支，开始了解你的消费",
                actionLabel = "记一笔",
                onAction = vm::openAdd,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                groups.forEach { (date, items) ->
                    val dayExpense = items.filter { it.type == "expense" }.sumOf { it.amountCents }
                    val dayIncome = items.filter { it.type == "income" }.sumOf { it.amountCents }
                    item(key = "head_$date") {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(6.dp)
                                        .clip(appShape(2.dp))
                                        .background(palette.accent),
                                )
                                Text(
                                    dateLabel(date),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.text,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.padding(start = 7.dp),
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (dayIncome > 0) {
                                    Text("收 ${formatCents(dayIncome)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = palette.income)
                                }
                                if (dayExpense > 0) {
                                    Text("支 ${formatCents(dayExpense)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = palette.expense)
                                }
                            }
                        }
                    }
                    item(key = "group_$date") {
                        GlassCard(modifier = Modifier.fillMaxWidth(), corner = 12.dp, contentPadding = 0.dp) {
                            items.forEachIndexed { index, t ->
                                val cat = catMap[t.categoryId]
                                val isExpense = t.type == "expense"
                                val tint = if (isExpense) palette.expense else palette.income
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { vm.openEdit(t) }
                                        .padding(horizontal = 14.dp, vertical = 12.dp)
                                        .background(if (index % 2 == 1) palette.accentWeak.copy(alpha = 0.35f) else androidx.compose.ui.graphics.Color.Transparent),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        Modifier
                                            .size(44.dp)
                                            .clip(appShape(10.dp))
                                            .background(tint.copy(alpha = 0.12f))
                                            .border(1.dp, tint.copy(alpha = 0.22f), appShape(10.dp)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            categoryIcon(cat?.icon ?: "more-horizontal"),
                                            contentDescription = null,
                                            tint = tint,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                    Column(
                                        Modifier.weight(1f).padding(horizontal = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        Text(
                                            cat?.name ?: "未分类",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = palette.text,
                                        )
                                        if (t.note.isNotEmpty()) {
                                            Text(
                                                t.note,
                                                fontSize = 13.sp,
                                                color = palette.text3,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                    Text(
                                        "${if (isExpense) "-" else "+"}${formatCents(t.amountCents)}",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = tint,
                                        letterSpacing = 0.5.sp,
                                        style = TextStyle(shadow = hudGlow(tint, 0.25f, 6f)),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}