package com.jianzhang.app.ui.ledger

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jianzhang.app.data.Account
import com.jianzhang.app.data.Category
import com.jianzhang.app.data.Tag
import com.jianzhang.app.data.Transaction
import com.jianzhang.app.model.AccountBalance
import com.jianzhang.app.model.LedgerEntry
import com.jianzhang.app.model.TagUsage
import com.jianzhang.app.ui.JzViewModel
import com.jianzhang.app.ui.components.ChoiceChips
import com.jianzhang.app.ui.components.EmptyState
import com.jianzhang.app.ui.components.FlowRowChips
import com.jianzhang.app.ui.components.InlineAction
import com.jianzhang.app.ui.components.PageHeader
import com.jianzhang.app.ui.components.SearchField
import com.jianzhang.app.ui.components.appShape
import com.jianzhang.app.ui.theme.LocalPalette
import com.jianzhang.app.util.categoryIcon
import com.jianzhang.app.util.dateLabel
import com.jianzhang.app.util.formatCents
import kotlinx.coroutines.launch

private enum class BatchMode { CATEGORY, ACCOUNT, TAG, DELETE }

@Composable
fun LedgerScreen(vm: JzViewModel) {
    val palette = LocalPalette.current
    val scope = rememberCoroutineScope()
    var filterIndex by remember { mutableStateOf(0) }
    var search by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf<List<LedgerEntry>>(emptyList()) }
    var cats by remember { mutableStateOf<List<Category>>(emptyList()) }
    var accounts by remember { mutableStateOf<List<AccountBalance>>(emptyList()) }
    var tags by remember { mutableStateOf<List<TagUsage>>(emptyList()) }
    var accountFilter by remember { mutableLongStateOf(0L) }
    var tagFilter by remember { mutableLongStateOf(0L) }
    var selecting by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var batchMode by remember { mutableStateOf<BatchMode?>(null) }
    var batchError by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        entries = vm.repository.listLedger(vm.month)
        cats = vm.repository.categories()
        accounts = vm.repository.accounts(includeArchived = true)
        tags = vm.repository.tags()
    }
    LaunchedEffect(vm.month, vm.refreshKey) { reload() }

    val catMap = remember(cats) { cats.associateBy { it.id } }
    val accountMap = remember(accounts) { accounts.associateBy { it.account.id } }
    val tagMap = remember(tags) { tags.associateBy { it.tag.id } }
    val q = search.trim()
    val visible = entries.filter { entry ->
        val typeOk = when (filterIndex) {
            1 -> entry.kind == "expense"
            2 -> entry.kind == "income"
            3 -> entry.kind == "transfer"
            else -> true
        }
        val accountOk = accountFilter == 0L || entry.accountId == accountFilter || entry.toAccountId == accountFilter
        val tagOk = tagFilter == 0L || tagFilter in entry.tagIds
        val textSource = buildString {
            append(entry.note)
            append(catMap[entry.categoryId]?.name.orEmpty())
            append(accountMap[entry.accountId]?.account?.name.orEmpty())
            append(accountMap[entry.toAccountId]?.account?.name.orEmpty())
            entry.tagIds.forEach { append(tagMap[it]?.tag?.name.orEmpty()) }
        }
        typeOk && accountOk && tagOk && (q.isEmpty() || textSource.contains(q, ignoreCase = true))
    }
    val groups = remember(visible) { visible.groupBy { it.date }.toSortedMap(compareByDescending { it }) }
    val selectedTransactions = entries.filter { it.kind != "transfer" && it.id in selected }.map { it.id }

    Column(Modifier.fillMaxSize()) {
        PageHeader(
            title = "流水",
            subtitle = if (selecting) "已选择 ${selected.size} 笔" else "${entries.size} 条记录 · 含转账",
            trailing = {
                InlineAction(
                    if (selecting) "完成" else "批量",
                    {
                        selecting = !selecting
                        if (!selecting) selected = emptySet()
                    },
                    icon = Icons.Filled.CheckCircle,
                )
            },
        )
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            SearchField(search, { search = it }, placeholder = "搜索分类、账户、标签或备注")
            ChoiceChips(listOf("全部", "支出", "收入", "转账"), filterIndex, { filterIndex = it })
            if (accounts.size > 1) {
                FlowRowChips(
                    options = listOf(0L to "全部账户") + accounts.map { it.account.id to it.account.name },
                    selectedIds = setOf(accountFilter),
                    onToggle = { accountFilter = it },
                )
            }
            if (tags.isNotEmpty()) {
                FlowRowChips(
                    options = listOf(0L to "全部标签") + tags.map { it.tag.id to it.tag.name },
                    selectedIds = setOf(tagFilter),
                    onToggle = { tagFilter = it },
                )
            }
            if (selecting) {
                BatchBar(
                    count = selected.size,
                    onClear = { selected = emptySet() },
                    onCategory = { batchMode = BatchMode.CATEGORY; batchError = null },
                    onAccount = { batchMode = BatchMode.ACCOUNT; batchError = null },
                    onTag = { batchMode = BatchMode.TAG; batchError = null },
                    onDelete = { batchMode = BatchMode.DELETE; batchError = null },
                )
            }
            batchError?.let { Text(it, color = palette.danger, fontSize = 12.sp) }
        }
        if (groups.isEmpty()) {
            EmptyState(
                icon = "📊",
                title = "没有匹配的流水",
                sub = "调整筛选条件，或记录第一笔收支",
                actionLabel = "记一笔",
                onAction = vm::openAdd,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                groups.forEach { (date, items) ->
                    val dayExpense = items.filter { it.kind == "expense" }.sumOf { it.amountCents }
                    val dayIncome = items.filter { it.kind == "income" }.sumOf { it.amountCents }
                    item(key = "head_$date") {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(dateLabel(date), color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            if (dayIncome > 0) Text("收 ¥${formatCents(dayIncome)}", color = palette.income, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                            if (dayExpense > 0) Text("支 ¥${formatCents(dayExpense)}", color = palette.expense, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 10.dp))
                        }
                    }
                    items.forEach { entry ->
                        item(key = "${entry.kind}_${entry.id}") {
                            LedgerRow(
                                entry = entry,
                                cat = entry.categoryId?.let { catMap[it] },
                                from = entry.accountId?.let { accountMap[it]?.account },
                                to = entry.toAccountId?.let { accountMap[it]?.account },
                                tags = entry.tagIds.mapNotNull { tagMap[it]?.tag },
                                selecting = selecting,
                                selected = entry.id in selected && entry.kind != "transfer",
                                onClick = {
                                    when {
                                        selecting && entry.kind == "transfer" -> batchError = "转账不能参与账目批量操作"
                                        selecting -> selected = if (entry.id in selected) selected - entry.id else selected + entry.id
                                        entry.kind == "transfer" -> scope.launch {
                                            vm.repository.transfer(entry.id)?.let(vm::openTransfer)
                                        }
                                        else -> {
                                            val tx = Transaction(
                                                id = entry.id,
                                                type = entry.kind,
                                                amountCents = entry.amountCents,
                                                categoryId = entry.categoryId ?: 0L,
                                                accountId = entry.accountId,
                                                note = entry.note,
                                                date = entry.date,
                                                createdAt = entry.createdAt,
                                            )
                                            vm.openEdit(tx)
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (batchMode != null) {
        BatchDialog(
            mode = batchMode!!,
            count = selected.size,
            validCount = selectedTransactions.size,
            categories = cats,
            accounts = accounts.map { it.account },
            tags = tags.map { it.tag },
            onDismiss = { batchMode = null },
            onCategory = { id ->
                scope.launch {
                    runCatching { vm.repository.bulkSetCategory(selectedTransactions, id); vm.refresh() }
                        .onSuccess { batchMode = null; selected = emptySet() }
                        .onFailure { batchError = it.message ?: "批量修改失败" }
                }
            },
            onAccount = { id ->
                scope.launch {
                    runCatching { vm.repository.bulkSetAccount(selectedTransactions, id); vm.refresh() }
                        .onSuccess { batchMode = null; selected = emptySet() }
                        .onFailure { batchError = it.message ?: "批量修改失败" }
                }
            },
            onTag = { id ->
                scope.launch {
                    runCatching { vm.repository.bulkAddTag(selectedTransactions, id); vm.refresh() }
                        .onSuccess { batchMode = null; selected = emptySet() }
                        .onFailure { batchError = it.message ?: "批量加标签失败" }
                }
            },
            onDelete = {
                scope.launch {
                    runCatching { vm.repository.bulkDeleteTransactions(selectedTransactions); vm.refresh() }
                        .onSuccess { batchMode = null; selected = emptySet() }
                        .onFailure { batchError = it.message ?: "批量删除失败" }
                }
            },
        )
    }
}

@Composable
private fun LedgerRow(
    entry: LedgerEntry,
    cat: Category?,
    from: Account?,
    to: Account?,
    tags: List<Tag>,
    selecting: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalPalette.current
    val isTransfer = entry.kind == "transfer"
    val isExpense = entry.kind == "expense"
    val tint = when (entry.kind) {
        "income" -> palette.income
        "transfer" -> palette.accent
        else -> palette.expense
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(appShape(7.dp))
            .background(if (selected) palette.accentWeak else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 5.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selecting) {
            Icon(
                if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isTransfer) palette.text3 else if (selected) palette.accent else palette.text3,
                modifier = Modifier.size(19.dp),
            )
        }
        Box(
            Modifier.size(38.dp).clip(appShape(8.dp)).background(tint.copy(alpha = 0.12f)).border(1.dp, tint.copy(alpha = 0.24f), appShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (isTransfer) Icons.Filled.SwapHoriz else categoryIcon(cat?.icon ?: "more-horizontal"),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(
                when {
                    isTransfer -> "${from?.name ?: "—"} → ${to?.name ?: "—"}"
                    else -> cat?.name ?: "未分类"
                },
                color = palette.text,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val sub = buildString {
                if (entry.note.isNotBlank()) append(entry.note)
                if (!isTransfer && from != null) {
                    if (isNotEmpty()) append(" · ")
                    append(from.name)
                }
                if (isTransfer && entry.feeCents > 0) {
                    if (isNotEmpty()) append(" · ")
                    append("手续费 ¥${formatCents(entry.feeCents)}")
                }
                if (tags.isNotEmpty()) {
                    if (isNotEmpty()) append(" · ")
                    append(tags.joinToString(" ") { "#${it.name}" })
                }
            }
            if (sub.isNotBlank()) Text(sub, color = palette.text3, fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(
            when (entry.kind) {
                "income" -> "+¥${formatCents(entry.amountCents)}"
                "expense" -> "-¥${formatCents(entry.amountCents)}"
                else -> "¥${formatCents(entry.amountCents)}"
            },
            color = tint,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun BatchBar(
    count: Int,
    onClear: () -> Unit,
    onCategory: () -> Unit,
    onAccount: () -> Unit,
    onTag: () -> Unit,
    onDelete: () -> Unit,
) {
    val palette = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().clip(appShape(7.dp)).background(palette.accentWeak).border(1.dp, palette.borderStrong, appShape(7.dp)).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text("$count 笔", color = palette.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        InlineAction("分类", onCategory)
        InlineAction("账户", onAccount)
        InlineAction("标签", onTag)
        InlineAction("删除", onDelete, tint = palette.danger, icon = Icons.Filled.Delete)
        Box(Modifier.weight(1f))
        TextButton(onClick = onClear) { Text("清空", color = palette.text3, fontSize = 11.5.sp) }
    }
}

@Composable
private fun BatchDialog(
    mode: BatchMode,
    count: Int,
    validCount: Int,
    categories: List<Category>,
    accounts: List<Account>,
    tags: List<Tag>,
    onDismiss: () -> Unit,
    onCategory: (Long) -> Unit,
    onAccount: (Long?) -> Unit,
    onTag: (Long) -> Unit,
    onDelete: () -> Unit,
) {
    val palette = LocalPalette.current
    when (mode) {
        BatchMode.DELETE -> AlertDialog(
            onDismissRequest = onDismiss,
            shape = appShape(10.dp),
            containerColor = palette.card,
            title = { Text("批量删除") },
            text = { Text("将删除已选择的 $validCount 笔账目，转账不会被删除。此操作不可撤销。") },
            confirmButton = { TextButton(onClick = onDelete) { Text("确认删除", color = palette.danger, fontWeight = FontWeight.SemiBold) } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = palette.text2) } },
        )
        BatchMode.CATEGORY -> AlertDialog(
            onDismissRequest = onDismiss,
            shape = appShape(10.dp),
            containerColor = palette.card,
            title = { Text("批量修改分类") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("所选账目必须同为支出或收入；所选 $count 笔中可操作 $validCount 笔。", color = palette.text3, fontSize = 12.sp)
                    ChoiceList(categories.map { it.id to it.name }, onCategory)
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("关闭", color = palette.accent) } },
        )
        BatchMode.ACCOUNT -> AlertDialog(
            onDismissRequest = onDismiss,
            shape = appShape(10.dp),
            containerColor = palette.card,
            title = { Text("批量修改账户") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { ChoiceList(accounts.map { it.id to it.name }, { onAccount(it) }) } },
            confirmButton = { TextButton(onClick = onDismiss) { Text("关闭", color = palette.accent) } },
        )
        BatchMode.TAG -> AlertDialog(
            onDismissRequest = onDismiss,
            shape = appShape(10.dp),
            containerColor = palette.card,
            title = { Text("批量添加标签") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { ChoiceList(tags.map { it.id to it.name }, onTag) } },
            confirmButton = { TextButton(onClick = onDismiss) { Text("关闭", color = palette.accent) } },
        )
    }
}

@Composable
private fun ChoiceList(options: List<Pair<Long, String>>, onSelect: (Long) -> Unit) {
    val palette = LocalPalette.current
    if (options.isEmpty()) {
        Text("暂无可选项", color = palette.text3, fontSize = 12.sp)
        return
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.take(20).forEach { (id, label) ->
            Row(
                Modifier.fillMaxWidth().clip(appShape(6.dp)).clickable { onSelect(id) }.padding(horizontal = 8.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, color = palette.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.Edit, contentDescription = null, tint = palette.text3, modifier = Modifier.size(15.dp))
            }
        }
    }
}
