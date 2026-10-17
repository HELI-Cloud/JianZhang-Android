package com.jianzhang.app.ui.recurring

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jianzhang.app.data.Account
import com.jianzhang.app.data.Category
import com.jianzhang.app.data.RecurringRule
import com.jianzhang.app.data.RecurringSchedule
import com.jianzhang.app.model.AccountBalance
import com.jianzhang.app.ui.JzViewModel
import com.jianzhang.app.ui.components.ChoiceChips
import com.jianzhang.app.ui.components.FlowRowChips
import com.jianzhang.app.ui.components.InlineAction
import com.jianzhang.app.ui.components.JzSwitch
import com.jianzhang.app.ui.components.JzTextInput
import com.jianzhang.app.ui.components.PageHeader
import com.jianzhang.app.ui.components.SectionLabel
import com.jianzhang.app.ui.components.appShape
import com.jianzhang.app.ui.components.hudGlow
import com.jianzhang.app.ui.theme.LocalPalette
import com.jianzhang.app.util.categoryIcon
import com.jianzhang.app.util.formatCents
import com.jianzhang.app.util.parseYuanToCents
import com.jianzhang.app.util.sanitizeAmountInput
import com.jianzhang.app.util.today
import kotlinx.coroutines.launch
import java.time.LocalDate

private data class RecurringPreset(
    val label: String,
    val note: String,
    val type: String,
    val frequency: String,
)

private val PRESETS = listOf(
    RecurringPreset("房租", "每月固定支出", "expense", "monthly"),
    RecurringPreset("工资", "每月固定收入", "income", "monthly"),
    RecurringPreset("订阅", "会员与软件", "expense", "monthly"),
    RecurringPreset("还款", "分期或信用卡", "expense", "monthly"),
)

@Composable
fun RecurringScreen(vm: JzViewModel) {
    val palette = LocalPalette.current
    val scope = rememberCoroutineScope()
    var rules by remember { mutableStateOf<List<RecurringRule>>(emptyList()) }
    var cats by remember { mutableStateOf<List<Category>>(emptyList()) }
    var accounts by remember { mutableStateOf<List<AccountBalance>>(emptyList()) }
    var editing by remember { mutableStateOf<RecurringRule?>(null) }
    var formOpen by remember { mutableStateOf(false) }
    var preset by remember { mutableStateOf<RecurringPreset?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        rules = vm.repository.recurringRules()
        cats = vm.repository.categories()
        accounts = vm.repository.accounts()
    }
    LaunchedEffect(vm.refreshKey) { reload() }

    val active = rules.filter { it.enabled }
    val monthlyLoad = active.filter { it.type == "expense" }.sumOf { RecurringSchedule.monthlyEquivalentCents(it) }
    val catMap = remember(cats) { cats.associateBy { it.id } }
    val accountMap = remember(accounts) { accounts.associateBy { it.account.id } }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            PageHeader(
                title = "周期记账",
                subtitle = "${active.size} 条启用 · 月固定支出 ¥${formatCents(monthlyLoad)}",
                trailing = { InlineAction("新增", { editing = null; preset = null; formOpen = true }, icon = Icons.Filled.Add) },
            )
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text("常用模板", color = palette.text3, fontSize = 11.5.sp, letterSpacing = 0.8.sp)
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PRESETS.forEach { item ->
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(appShape(7.dp))
                                .background(palette.bg1.copy(alpha = 0.74f))
                                .border(1.dp, palette.border, appShape(7.dp))
                                .clickable {
                                    editing = null
                                    preset = item
                                    formOpen = true
                                }
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(Icons.Filled.Bolt, contentDescription = null, tint = palette.accent, modifier = Modifier.size(15.dp))
                            Text(item.label, color = palette.text, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                SectionLabel("全部周期账目")
                message?.let {
                    Text(it, color = if (it.contains("失败") || it.contains("超过")) palette.danger else palette.income, fontSize = 12.sp)
                }
                rules.forEach { rule ->
                    val cat = catMap[rule.categoryId]
                    val tint = if (rule.type == "expense") palette.expense else palette.income
                    Row(Modifier.fillMaxWidth().padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(38.dp).clip(appShape(8.dp)).background(tint.copy(alpha = 0.12f)).border(1.dp, tint.copy(alpha = 0.25f), appShape(8.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(categoryIcon(cat?.icon ?: "more-horizontal"), contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                        }
                        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                            Text(
                                "${cat?.name ?: "未分类"}${if (rule.note.isBlank()) "" else " · ${rule.note}"}",
                                color = if (rule.enabled) palette.text else palette.text3,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "${RecurringSchedule.label(rule.frequency, rule.intervalCount)} · 下次 ${rule.nextDate}${if (rule.endDate != null) " · 至 ${rule.endDate}" else ""}",
                                color = palette.text3,
                                fontSize = 11.5.sp,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "${if (rule.type == "expense") "-" else "+"}¥${formatCents(rule.amountCents)}",
                                color = tint,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(accountMap[rule.accountId]?.account?.name ?: "默认账户", color = palette.text3, fontSize = 10.5.sp)
                        }
                        if (rule.enabled && rule.nextDate <= today() && (rule.endDate == null || rule.nextDate <= rule.endDate)) {
                            IconButton(onClick = {
                                scope.launch {
                                    message = runCatching { vm.repository.confirmRecurring(rule); vm.refresh(); "已入账 ${rule.nextDate}" }
                                        .getOrElse { it.message ?: "入账失败" }
                                }
                            }) { Icon(Icons.Filled.Bolt, "立即入账", tint = palette.accent, modifier = Modifier.size(17.dp)) }
                        }
                        IconButton(onClick = {
                            scope.launch {
                                vm.repository.setRecurringEnabled(rule.id, !rule.enabled)
                                vm.refresh()
                            }
                        }) {
                            Icon(if (rule.enabled) Icons.Filled.Pause else Icons.Filled.PlayArrow, "暂停或启用", tint = palette.text3, modifier = Modifier.size(17.dp))
                        }
                        IconButton(onClick = { editing = rule; preset = null; formOpen = true }) {
                            Icon(Icons.Filled.Edit, "编辑", tint = palette.text3, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = {
                            scope.launch {
                                vm.repository.deleteRecurringRule(rule.id)
                                vm.refresh()
                            }
                        }) { Icon(Icons.Filled.Delete, "删除", tint = palette.danger, modifier = Modifier.size(16.dp)) }
                    }
                }
                if (rules.isEmpty()) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Repeat, contentDescription = null, tint = palette.text3, modifier = Modifier.size(32.dp))
                        Text("还没有周期账目", color = palette.text2, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                        Text("房租、工资、订阅设置一次，之后自动记账。", color = palette.text3, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    if (formOpen) {
        RecurringDialog(
            editing = editing,
            preset = preset,
            categories = cats,
            accounts = accounts,
            onDismiss = { formOpen = false },
            onSave = { rule ->
                scope.launch {
                    runCatching {
                        if (editing == null) vm.repository.addRecurringRule(rule) else vm.repository.updateRecurringRule(rule)
                        vm.refresh()
                        formOpen = false
                    }.onFailure { message = "保存失败：${it.message ?: "未知错误"}" }
                }
            },
        )
    }
}

@Composable
private fun RecurringDialog(
    editing: RecurringRule?,
    preset: RecurringPreset?,
    categories: List<Category>,
    accounts: List<AccountBalance>,
    onDismiss: () -> Unit,
    onSave: (RecurringRule) -> Unit,
) {
    val palette = LocalPalette.current
    var type by remember { mutableStateOf(editing?.type ?: preset?.type ?: "expense") }
    var amount by remember { mutableStateOf(editing?.amountCents?.let { yuan(it) }.orEmpty()) }
    var categoryId by remember { mutableLongStateOf(editing?.categoryId ?: 0L) }
    var accountId by remember { mutableLongStateOf(editing?.accountId ?: accounts.firstOrNull()?.account?.id ?: 0L) }
    var note by remember { mutableStateOf(editing?.note ?: preset?.note.orEmpty()) }
    var frequencyIndex by remember {
        mutableIntStateOf(listOf("daily", "weekly", "monthly", "yearly").indexOf(editing?.frequency ?: preset?.frequency ?: "monthly").takeIf { it >= 0 } ?: 2)
    }
    var interval by remember { mutableStateOf(editing?.intervalCount?.toString() ?: "1") }
    var startDate by remember { mutableStateOf(editing?.startDate ?: today()) }
    var endDate by remember { mutableStateOf(editing?.endDate.orEmpty()) }
    var autoCreate by remember { mutableStateOf(editing?.autoCreate ?: true) }
    var error by remember { mutableStateOf<String?>(null) }
    val frequencies = listOf("daily", "weekly", "monthly", "yearly")
    val visibleCats = categories.filter { it.type == type }
    LaunchedEffect(type) {
        if (visibleCats.none { it.id == categoryId }) categoryId = visibleCats.firstOrNull()?.id ?: 0L
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = appShape(10.dp),
        containerColor = palette.card,
        title = { Text(if (editing == null) "新增周期账目" else "编辑周期账目") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ChoiceChips(listOf("支出", "收入"), if (type == "expense") 0 else 1, { type = if (it == 0) "expense" else "income" })
                JzTextInput(amount, { amount = sanitizeAmountInput(it); error = null }, "金额", prefix = "¥")
                Text("分类", color = palette.text3, fontSize = 11.5.sp)
                FlowRowChips(visibleCats.map { it.id to it.name }, setOf(categoryId), { categoryId = it })
                Text("账户", color = palette.text3, fontSize = 11.5.sp)
                FlowRowChips(accounts.map { it.account.id to it.account.name }, setOf(accountId), { accountId = it })
                ChoiceChips(listOf("每天", "每周", "每月", "每年"), frequencyIndex, { frequencyIndex = it })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    JzTextInput(interval, { interval = it.filter(Char::isDigit).take(2); error = null }, "间隔", Modifier.weight(1f))
                    JzTextInput(note, { note = it.take(40) }, "备注", Modifier.weight(2f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    JzTextInput(startDate, { startDate = it.take(10); error = null }, "开始日期", Modifier.weight(1f))
                    JzTextInput(endDate, { endDate = it.take(10); error = null }, "结束日期（可空）", Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("到期自动入账", color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("关闭后由提醒中心确认入账", color = palette.text3, fontSize = 11.5.sp)
                    }
                    JzSwitch(autoCreate, { autoCreate = it })
                }
                error?.let { Text(it, color = palette.danger, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cents = parseYuanToCents(amount)
                val count = interval.toIntOrNull()?.coerceIn(1, 60) ?: 1
                val frequency = frequencies[frequencyIndex]
                val date = runCatching { LocalDate.parse(startDate) }.getOrNull()
                val end = endDate.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                error = when {
                    cents == null || cents <= 0 -> "请输入有效金额"
                    categoryId == 0L -> "请选择分类"
                    date == null -> "请输入有效开始日期"
                    endDate.isNotBlank() && end == null -> "请输入有效结束日期"
                    end != null && end < date -> "结束日期不能早于开始日期"
                    else -> null
                }
                if (error == null) {
                    onSave(
                        RecurringRule(
                            id = editing?.id ?: 0L,
                            type = type,
                            amountCents = cents!!,
                            categoryId = categoryId,
                            accountId = accountId.takeIf { it > 0 },
                            note = note.trim(),
                            frequency = frequency,
                            intervalCount = count,
                            weekday = if (frequency == "weekly") date!!.dayOfWeek.value % 7 else null,
                            dayOfMonth = if (frequency == "monthly" || frequency == "yearly") date!!.dayOfMonth else null,
                            monthOfYear = if (frequency == "yearly") date!!.monthValue else null,
                            startDate = startDate,
                            endDate = endDate.takeIf { it.isNotBlank() },
                            nextDate = editing?.nextDate ?: startDate,
                            autoCreate = autoCreate,
                            enabled = editing?.enabled ?: true,
                            createdAt = editing?.createdAt.orEmpty(),
                        )
                    )
                }
            }) { Text("保存", color = palette.accent, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = palette.text2) } },
    )
}

private fun yuan(cents: Long): String = (cents / 100.0).toString().trimEnd('0').trimEnd('.')
