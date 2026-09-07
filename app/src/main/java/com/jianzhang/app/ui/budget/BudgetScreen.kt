package com.jianzhang.app.ui.budget

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jianzhang.app.data.Budget
import com.jianzhang.app.data.Category
import com.jianzhang.app.model.MonthSummary
import com.jianzhang.app.model.TxType
import com.jianzhang.app.ui.JzViewModel
import com.jianzhang.app.ui.components.ChamferShape
import com.jianzhang.app.ui.components.GlassCard
import com.jianzhang.app.ui.components.hudGlow
import com.jianzhang.app.ui.theme.LocalPalette
import com.jianzhang.app.util.categoryIcon
import com.jianzhang.app.util.daysLeftInMonth
import com.jianzhang.app.util.formatCents
import com.jianzhang.app.util.parseYuanToCents
import com.jianzhang.app.util.sanitizeAmountInput
import kotlinx.coroutines.launch

private data class CatBudgetRow(
    val cat: Category,
    val budgetCents: Long,
    val spentCents: Long,
    val percent: Int,
    val over: Boolean,
    val remaining: Long,
)

@Composable
fun BudgetScreen(vm: JzViewModel) {
    val palette = LocalPalette.current
    val scope = rememberCoroutineScope()

    var budgets by remember { mutableStateOf<List<Budget>>(emptyList()) }
    var cats by remember { mutableStateOf<List<Category>>(emptyList()) }
    var summary by remember { mutableStateOf(MonthSummary()) }
    var spentByName by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }

    LaunchedEffect(vm.month, vm.refreshKey) {
        budgets = vm.repository.budgetsForMonth(vm.month)
        cats = vm.repository.categories()
        summary = vm.repository.monthSummary(vm.month)
        spentByName = vm.repository.categoryTotals(vm.month, TxType.EXPENSE).associate { it.name to it.totalCents }
    }

    val totalBudget = budgets.firstOrNull { it.categoryId == null }?.amountCents ?: 0L
    val expense = summary.expenseCents
    val totalPercent = if (totalBudget > 0) minOf(999, ((expense.toFloat() / totalBudget) * 100).toInt()) else 0
    val totalOver = totalBudget > 0 && expense > totalBudget
    val remaining = totalBudget - expense
    val daysLeft = daysLeftInMonth(vm.month)
    val dailyAllowance = if (remaining > 0 && daysLeft > 0) remaining / daysLeft else 0L

    val catRows = cats
        .filter { it.type == "expense" }
        .map { c ->
            val b = budgets.firstOrNull { it.categoryId == c.id }?.amountCents ?: 0L
            val spent = spentByName[c.name] ?: 0L
            CatBudgetRow(
                cat = c,
                budgetCents = b,
                spentCents = spent,
                percent = if (b > 0) minOf(999, ((spent.toFloat() / b) * 100).toInt()) else 0,
                over = b > 0 && spent > b,
                remaining = b - spent,
            )
        }
        .filter { it.budgetCents > 0 }
        .sortedByDescending { it.spentCents }

    var totalDialogOpen by remember { mutableStateOf(false) }
    var totalAmount by remember { mutableStateOf("") }

    var catDialogOpen by remember { mutableStateOf(false) }
    var catEditingId by remember { mutableLongStateOf(0L) }
    var catPickId by remember { mutableLongStateOf(0L) }
    var catAmount by remember { mutableStateOf("") }
    var dialogError by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
                item {
            Text(
                "预算",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = palette.text,
                letterSpacing = 1.sp,
                style = TextStyle(shadow = hudGlow(palette.accent, 0.32f, 14f)),
            )
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("本月总预算", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = palette.text)
                        Text("按全部支出控制月度开销", fontSize = 12.sp, color = palette.text3)
                    }
                    TextButton(onClick = {
                        totalAmount = if (totalBudget > 0) (totalBudget / 100.0).toString().let { sanitizeAmountInput(it) } else ""
                        dialogError = null
                        totalDialogOpen = true
                    }) {
                        Text(if (totalBudget > 0) "修改" else "设置", color = palette.accent, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (totalBudget > 0) {
                    Text(
                        "¥${formatCents(totalBudget)}",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.text,
                        letterSpacing = 1.sp,
                        style = TextStyle(shadow = hudGlow(palette.accent, 0.3f, 12f)),
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    BudgetBar(percent = totalPercent, over = totalOver, modifier = Modifier.padding(top = 14.dp))
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "已用 ${formatCents(expense)} · $totalPercent%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (totalOver) palette.expense else palette.text2,
                        )
                        Text(
                            if (totalOver) "超支 ${formatCents(-remaining)}" else "剩余 ${formatCents(remaining)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (totalOver) palette.expense else palette.income,
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        StatMini("本月已支出", formatCents(expense), palette.expense, Modifier.weight(1f))
                        StatMini(
                            "剩余日均可用",
                            if (daysLeft > 0) "¥${(dailyAllowance / 100.0).toString().let { sanitizeAmountInput(it) }}/天" else "—",
                            palette.accent,
                            Modifier.weight(1f),
                        )
                        StatMini(
                            if (totalOver) "超支" else "预算结余",
                            if (totalOver) "-${formatCents(-remaining)}" else "+${formatCents(remaining)}",
                            if (totalOver) palette.expense else palette.income,
                            Modifier.weight(1f),
                        )
                    }
                } else {
                    Text(
                        "设置总预算后，这里会显示进度与日均可用额度。",
                        fontSize = 13.sp,
                        color = palette.text3,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                val withBudget = catRows.map { it.cat.id }.toSet()
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("分类预算", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = palette.text)
                        Text("为支出分类单独设限，超支标红提醒", fontSize = 12.sp, color = palette.text3)
                    }
                    if (cats.count { it.type == "expense" } > catRows.size) {
                        TextButton(onClick = {
                            val candidates = cats.filter { it.type == "expense" && it.id !in withBudget }
                            catPickId = candidates.firstOrNull()?.id ?: 0L
                            catEditingId = 0L
                            catAmount = ""
                            dialogError = null
                            catDialogOpen = true
                        }) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = palette.accent, modifier = Modifier.size(16.dp))
                            Text("添加", color = palette.accent, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                if (catRows.isEmpty()) {
                    Column(
                        Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Filled.Savings,
                            contentDescription = null,
                            tint = palette.text3,
                            modifier = Modifier.size(34.dp),
                        )
                        Text(
                            "还没有分类预算",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.text2,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        Text(
                            "为“餐饮”“购物”等分类单独设限，超支一目了然",
                            fontSize = 12.sp,
                            color = palette.text3,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                } else {
                    Column(
                        Modifier.padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        catRows.forEach { row ->
                            Column {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        Modifier.size(30.dp).clip(appShape(7.dp))
                                            .background(palette.accentWeak)
                                            .border(1.dp, palette.border, appShape(7.dp)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            categoryIcon(row.cat.icon),
                                            contentDescription = null,
                                            tint = palette.accent,
                                            modifier = Modifier.size(15.dp),
                                        )
                                    }
                                    Text(
                                        row.cat.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = palette.text,
                                        modifier = Modifier.weight(1f).padding(start = 10.dp),
                                    )
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = "修改",
                                        tint = palette.text3,
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(appShape(6.dp))
                                            .clickable {
                                                catEditingId = row.cat.id
                                                catPickId = row.cat.id
                                                catAmount = (row.budgetCents / 100.0).toString().let { sanitizeAmountInput(it) }
                                                dialogError = null
                                                catDialogOpen = true
                                            }
                                            .padding(6.dp),
                                    )
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "删除",
                                        tint = palette.text3,
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(appShape(6.dp))
                                            .clickable {
                                                scope.launch {
                                                    vm.repository.saveCategoryBudget(vm.month, row.cat.id, 0L)
                                                    vm.refresh()
                                                }
                                            }
                                            .padding(6.dp),
                                    )
                                }
                                BudgetBar(
                                    percent = row.percent,
                                    over = row.over,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                                Row(
                                    Modifier.fillMaxWidth().padding(top = 5.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        "${formatCents(row.spentCents)} / ${formatCents(row.budgetCents)} · ${row.percent}%",
                                        fontSize = 11.5.sp,
                                        color = palette.text2,
                                    )
                                    Text(
                                        if (row.over) "超 ${formatCents(-row.remaining)}" else "余 ${formatCents(row.remaining)}",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (row.over) palette.expense else palette.income,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (totalDialogOpen) {
        AmountDialog(
            title = if (totalBudget > 0) "修改本月总预算" else "设置本月总预算",
            amount = totalAmount,
            onAmountChange = { totalAmount = sanitizeAmountInput(it) },
            error = dialogError,
            onDismiss = { totalDialogOpen = false },
            onConfirm = {
                val cents = parseYuanToCents(totalAmount)
                if (cents == null || cents <= 0) {
                    dialogError = "请输入有效金额"
                } else {
                    scope.launch {
                        vm.repository.saveTotalBudget(vm.month, cents)
                        vm.refresh()
                        totalDialogOpen = false
                    }
                }
            },
        )
    }

    if (catDialogOpen) {
        val candidates = cats.filter { it.type == "expense" }
        AlertDialog(
            onDismissRequest = { catDialogOpen = false },
            shape = appShape(12.dp),
            title = { Text(if (catEditingId != 0L) "修改分类预算" else "添加分类预算") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (catEditingId == 0L) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(candidates) { c ->
                                val active = catPickId == c.id
                                Box(
                                    Modifier
                                        .clip(appShape(6.dp))
                                        .background(if (active) palette.accent else palette.bg1)
                                        .border(1.dp, if (active) palette.accent else palette.border, appShape(6.dp))
                                        .clickable { catPickId = c.id }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                ) {
                                    Text(
                                        c.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (active) Color(0xFF00171D) else palette.text,
                                    )
                                }
                            }
                        }
                    }
                    BasicTextField(
                        value = catAmount,
                        onValueChange = { catAmount = sanitizeAmountInput(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(appShape(8.dp))
                            .background(palette.bg)
                            .border(1.dp, palette.border, appShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        textStyle = TextStyle(color = palette.text, fontSize = 15.sp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        decorationBox = { inner ->
                            Box {
                                if (catAmount.isEmpty()) Text("预算金额（元）", color = palette.text3, fontSize = 15.sp)
                                inner()
                            }
                        },
                    )
                    dialogError?.let {
                        Text(it, fontSize = 12.sp, color = palette.danger)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val cents = parseYuanToCents(catAmount)
                    if (cents == null || cents <= 0) {
                        dialogError = "请输入有效金额"
                    } else if (catPickId == 0L) {
                        dialogError = "请选择分类"
                    } else {
                        scope.launch {
                            vm.repository.saveCategoryBudget(vm.month, catPickId, cents)
                            vm.refresh()
                            catDialogOpen = false
                        }
                    }
                }) {
                    Text("保存", color = palette.accent, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { catDialogOpen = false }) { Text("取消", color = palette.text2) }
            },
            containerColor = palette.card,
        )
    }
}

@Composable
private fun BudgetBar(percent: Int, over: Boolean, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val fraction = (percent.coerceIn(0, 100)) / 100f
    val shape = appShape(4.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(shape)
            .background(palette.bg)
            .border(1.dp, palette.border, shape),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .height(8.dp)
                .clip(shape)
                .background(Brush.horizontalGradient(listOf(if (over) palette.expense else palette.accent, if (over) palette.danger else palette.accentHover))),
        )
    }
}

@Composable
private fun StatMini(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Column(
        modifier
            .clip(appShape(8.dp))
            .background(palette.bg)
            .border(1.dp, palette.border, appShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(label, fontSize = 11.sp, color = palette.text3)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun AmountDialog(
    title: String,
    amount: String,
    onAmountChange: (String) -> Unit,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val palette = LocalPalette.current
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = appShape(12.dp),
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BasicTextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(appShape(8.dp))
                        .background(palette.bg)
                        .border(1.dp, palette.border, appShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    textStyle = TextStyle(color = palette.text, fontSize = 15.sp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    decorationBox = { inner ->
                        Box {
                            if (amount.isEmpty()) Text("每月预算金额（元）", color = palette.text3, fontSize = 15.sp)
                            inner()
                        }
                    },
                )
                error?.let {
                    Text(it, fontSize = 12.sp, color = palette.danger)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("保存", color = palette.accent, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = palette.text2) }
        },
        containerColor = palette.card,
    )
}
