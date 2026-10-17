package com.jianzhang.app.ui.accounts

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jianzhang.app.data.Account
import com.jianzhang.app.data.Transfer
import com.jianzhang.app.model.AccountBalance
import com.jianzhang.app.model.AccountSummary
import com.jianzhang.app.model.summarizeAccounts
import com.jianzhang.app.ui.JzViewModel
import com.jianzhang.app.ui.components.ChoiceChips
import com.jianzhang.app.ui.components.FlowRowChips
import com.jianzhang.app.ui.components.InlineAction
import com.jianzhang.app.ui.components.JzTextInput
import com.jianzhang.app.ui.components.PageHeader
import com.jianzhang.app.ui.components.SectionLabel
import com.jianzhang.app.ui.components.appShape
import com.jianzhang.app.ui.components.hudGlow
import com.jianzhang.app.ui.theme.LocalPalette
import com.jianzhang.app.util.categoryIcon
import com.jianzhang.app.util.formatCents
import com.jianzhang.app.util.isValidDate
import com.jianzhang.app.util.parseYuanToCents
import com.jianzhang.app.util.sanitizeAmountInput
import com.jianzhang.app.util.yuanPlain
import com.jianzhang.app.util.today
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class AccountKind(val key: String, val label: String, val icon: String, val color: String)

private val KINDS = listOf(
    AccountKind("cash", "现金", "banknote", "#2ee6a8"),
    AccountKind("bank", "银行卡", "credit-card", "#1fd0ff"),
    AccountKind("wechat", "微信", "message-circle", "#3ad7d0"),
    AccountKind("alipay", "支付宝", "smartphone", "#5b8cff"),
    AccountKind("credit", "信用卡", "credit-card", "#ff8a3d"),
    AccountKind("investment", "投资", "trending-up", "#a06bff"),
    AccountKind("other", "其他", "wallet", "#8ba6bc"),
)

@Composable
fun AccountsScreen(vm: JzViewModel) {
    val palette = LocalPalette.current
    val scope = rememberCoroutineScope()
    var accounts by remember { mutableStateOf<List<AccountBalance>>(emptyList()) }
    var transfers by remember { mutableStateOf<List<Transfer>>(emptyList()) }
    var formOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Account?>(null) }
    var confirmDelete by remember { mutableLongStateOf(0L) }
    var message by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(confirmDelete) {
        if (confirmDelete != 0L) {
            delay(3200)
            confirmDelete = 0L
        }
    }
    LaunchedEffect(message) {
        if (message != null) {
            delay(2600)
            message = null
        }
    }

    suspend fun reload() {
        accounts = vm.repository.accounts(includeArchived = true)
        transfers = vm.repository.transfers(vm.month)
    }
    LaunchedEffect(vm.month, vm.refreshKey) { reload() }

    // 统一口径（修复）：净资产 / 资产 / 负债 均含停用账户，保证 净资产 == 资产 + 负债
    val summary = summarizeAccounts(accounts)
    val activeAccounts = accounts.filter { !it.account.archived }
    val archivedAccounts = accounts.filter { it.account.archived }
    val maxAbs = accounts.maxOfOrNull { kotlin.math.abs(it.balanceCents) }?.coerceAtLeast(1L) ?: 1L
    val accountMap = remember(accounts) { accounts.associateBy { it.account.id } }

    fun toggleArchived(item: AccountBalance) {
        scope.launch {
            runCatching { vm.repository.setAccountArchived(item.account.id, !item.account.archived) }
                .onFailure { message = it.message ?: "操作失败" }
            vm.refresh()
        }
    }

    fun deleteAccount(item: AccountBalance) {
        if (confirmDelete != item.account.id) {
            confirmDelete = item.account.id
        } else {
            confirmDelete = 0
            scope.launch {
                runCatching { vm.repository.deleteAccount(item.account.id) }
                    .onFailure { message = it.message ?: "删除失败" }
                vm.refresh()
            }
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
    ) {
        item {
            PageHeader(
                title = "账户",
                subtitle = "${summary.activeCount} 个在用 · ${summary.archivedCount} 个停用 · 余额按账目与转账实时结算",
                trailing = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InlineAction("账户", { editing = null; formOpen = true }, icon = Icons.Filled.Add)
                        InlineAction("转账", { vm.openTransfer() }, icon = Icons.Filled.SwapHoriz)
                    }
                },
            )
        }
        item { NetWorthBlock(summary, Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) }
        message?.let { hint ->
            item {
                Text(
                    hint,
                    color = palette.danger,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                if (activeAccounts.isNotEmpty()) {
                    SectionLabel("在用账户", "点击账户行即可编辑 · 停用账户仍保留历史余额")
                    activeAccounts.forEach { item ->
                        AccountRow(
                            item = item,
                            maxAbs = maxAbs,
                            confirming = confirmDelete == item.account.id,
                            onEdit = { editing = item.account; formOpen = true },
                            onArchive = { toggleArchived(item) },
                            onDelete = { deleteAccount(item) },
                        )
                    }
                }
                if (archivedAccounts.isNotEmpty()) {
                    SectionLabel("已停用", "余额仍计入净资产，随时可启用")
                    archivedAccounts.forEach { item ->
                        AccountRow(
                            item = item,
                            maxAbs = maxAbs,
                            confirming = confirmDelete == item.account.id,
                            onEdit = { editing = item.account; formOpen = true },
                            onArchive = { toggleArchived(item) },
                            onDelete = { deleteAccount(item) },
                        )
                    }
                }
                if (accounts.isEmpty()) Text("还没有账户，点右上角「账户」新建一个", color = palette.text3, modifier = Modifier.padding(vertical = 16.dp))
            }
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                SectionLabel("本月转账", vm.month)
                transfers.forEach { transfer ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .clickable { vm.openTransfer(transfer) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.SwapHoriz, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(
                                "${accountMap[transfer.fromAccountId]?.account?.name ?: "—"} → ${accountMap[transfer.toAccountId]?.account?.name ?: "—"}",
                                color = palette.text,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                "${transfer.date}${if (transfer.note.isBlank()) "" else " · ${transfer.note}"}${if (transfer.feeCents > 0) " · 手续费 ¥${formatCents(transfer.feeCents)}" else ""}",
                                color = palette.text3,
                                fontSize = 11.5.sp,
                            )
                        }
                        Text(
                            "¥${formatCents(transfer.amountCents)}",
                            color = palette.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
                if (transfers.isEmpty()) Text("本月还没有转账记录", color = palette.text3, modifier = Modifier.padding(vertical = 16.dp))
            }
        }
    }

    if (formOpen) {
        AccountDialog(
            account = editing,
            onDismiss = { formOpen = false },
            onSave = { account ->
                runCatching {
                    if (account.id == 0L) vm.repository.addAccount(account) else vm.repository.updateAccount(account)
                    vm.refresh()
                }.fold(onSuccess = { null }, onFailure = { it.message ?: "保存失败" })
            },
        )
    }
    if (vm.transferOpen && accounts.isNotEmpty()) {
        TransferSheet(vm, accounts) { vm.closeTransfer() }
    }
}

/**
 * 净资产区：去掉整块渐变卡片，改为轻薄分栏 + 结构条。
 * 口径统一（修复点）：净资产、资产、负债全部含停用账户，恒有 净资产 = 资产 + 负债。
 */
@Composable
private fun NetWorthBlock(summary: AccountSummary, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val totalAbs = (summary.assetsCents + kotlin.math.abs(summary.debtCents)).coerceAtLeast(1L)
    val assetTarget = (summary.assetsCents.toFloat() / totalAbs).coerceAtLeast(0.001f)
    val debtTarget = (kotlin.math.abs(summary.debtCents).toFloat() / totalAbs).coerceAtLeast(0.001f)
    val assetFrac by animateFloatAsState(assetTarget, tween(520), label = "assetShare")
    val debtFrac by animateFloatAsState(debtTarget, tween(520), label = "debtShare")

    Column(modifier.fillMaxWidth()) {
        Text("净资产", color = palette.text3, fontSize = 12.sp, letterSpacing = 1.sp)
        Text(
            "¥${formatCents(summary.netWorthCents)}",
            color = if (summary.netWorthCents < 0) palette.expense else palette.text,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 2.dp),
        )
        if (summary.assetsCents > 0L || summary.debtCents != 0L) {
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp).height(6.dp).clip(appShape(3.dp)),
            ) {
                Box(Modifier.weight(assetFrac).fillMaxSize().background(palette.accent))
                Spacer(Modifier.width(2.dp))
                Box(Modifier.weight(debtFrac).fillMaxSize().background(palette.danger))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            StatCell("资产", summary.assetsCents, palette.income, Modifier.weight(1f))
            StatCell("负债", summary.debtCents, if (summary.debtCents < 0) palette.expense else palette.text2, Modifier.weight(1f))
            StatCell("在用账户", summary.activeCount.toLong(), palette.text2, Modifier.weight(1f), isMoney = false)
        }
        if (summary.archivedCount > 0) {
            Text(
                "含 ${summary.archivedCount} 个已停用账户的余额",
                color = palette.text3,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        Box(Modifier.fillMaxWidth().padding(top = 12.dp).height(1.dp).background(palette.border.copy(alpha = 0.5f)))
    }
}

@Composable
private fun StatCell(label: String, value: Long, color: Color, modifier: Modifier = Modifier, isMoney: Boolean = true) {
    val palette = LocalPalette.current
    Column(modifier) {
        Text(label, color = palette.text3, fontSize = 11.sp)
        Text(
            if (isMoney) "¥${formatCents(value)}" else value.toString(),
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun AccountRow(
    item: AccountBalance,
    maxAbs: Long,
    confirming: Boolean,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    val palette = LocalPalette.current
    val account = item.account
    val archived = account.archived
    val tint = runCatching { Color(android.graphics.Color.parseColor(account.color)) }.getOrDefault(palette.accent)
    val shareTarget = (kotlin.math.abs(item.balanceCents).toFloat() / maxAbs).coerceIn(0.02f, 1f)
    val share by animateFloatAsState(shareTarget, tween(420), label = "accountShare")
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp)
                .clickable(onClick = onEdit)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(appShape(8.dp))
                    .background(tint.copy(alpha = if (archived) 0.07f else 0.13f))
                    .border(1.dp, tint.copy(alpha = if (archived) 0.16f else 0.28f), appShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(accountIcon(account.icon), contentDescription = null, tint = tint.copy(alpha = if (archived) 0.5f else 1f), modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(
                    account.name,
                    color = if (archived) palette.text3 else palette.text,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    buildString {
                        append(KINDS.firstOrNull { it.key == account.kind }?.label ?: "其他")
                        append(" · ")
                        append(item.txCount)
                        append(" 笔")
                        if (archived) append(" · 已停用")
                    },
                    color = palette.text3,
                    fontSize = 11.5.sp,
                )
                Box(
                    Modifier
                        .fillMaxWidth(share)
                        .padding(top = 6.dp)
                        .height(3.dp)
                        .clip(appShape(2.dp))
                        .background(if (item.balanceCents < 0) palette.danger.copy(alpha = 0.65f) else tint.copy(alpha = 0.65f)),
                )
            }
            Text(
                "¥${formatCents(item.balanceCents)}",
                color = if (archived) palette.text3 else if (item.balanceCents < 0) palette.expense else palette.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
            IconButton(onClick = onArchive) {
                Icon(
                    if (archived) Icons.Filled.Unarchive else Icons.Filled.Archive,
                    contentDescription = if (archived) "启用账户" else "停用账户",
                    tint = palette.text3,
                    modifier = Modifier.size(17.dp),
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = if (confirming) "再次点击确认删除" else "删除账户",
                    tint = if (confirming) palette.danger else palette.text3,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
        if (confirming) {
            Text(
                "再次点击删除图标即确认；该账户的账目与转账会转到另一个在用账户",
                color = palette.danger,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(palette.border.copy(alpha = 0.28f)))
    }
}

@Composable
private fun AccountDialog(account: Account?, onDismiss: () -> Unit, onSave: suspend (Account) -> String?) {
    val palette = LocalPalette.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(account?.name.orEmpty()) }
    var initial by remember { mutableStateOf(yuanPlain(account?.initialCents ?: 0L)) }
    var kindIndex by remember {
        mutableStateOf(KINDS.indexOfFirst { it.key == account?.kind }.takeIf { it >= 0 } ?: 1)
    }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val kind = KINDS[kindIndex]
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        shape = appShape(10.dp),
        containerColor = palette.card,
        title = { Text(if (account == null) "新增账户" else "编辑账户") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                JzTextInput(name, { name = it.take(16); error = null }, "账户名称")
                ChoiceChips(KINDS.map { it.label }, kindIndex, { kindIndex = it })
                JzTextInput(initial, { initial = sanitizeAmountInput(it); error = null }, "初始余额（可空）", prefix = "¥")
                error?.let { Text(it, color = palette.danger, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) {
                    error = "请输入账户名称"
                } else {
                    val cents = if (initial.isBlank()) 0L else parseYuanToCents(initial)
                    if (cents == null) {
                        error = "初始余额格式不正确"
                    } else {
                        scope.launch {
                            saving = true
                            val failure = onSave(
                                Account(
                                    id = account?.id ?: 0L,
                                    name = name.trim(),
                                    kind = kind.key,
                                    icon = kind.icon,
                                    color = kind.color,
                                    initialCents = cents,
                                    sort = account?.sort ?: 0,
                                    archived = account?.archived ?: false,
                                    createdAt = account?.createdAt.orEmpty(),
                                )
                            )
                            saving = false
                            if (failure == null) onDismiss() else error = failure
                        }
                    }
                }
            }, enabled = !saving) { Text(if (saving) "保存中…" else "保存", color = palette.accent, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = palette.text2) } },
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TransferSheet(vm: JzViewModel, accounts: List<AccountBalance>, onClose: () -> Unit) {
    val palette = LocalPalette.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val active = accounts.filter { !it.account.archived || it.account.id == vm.editingTransfer?.fromAccountId || it.account.id == vm.editingTransfer?.toAccountId }
    val editing = vm.editingTransfer
    var fromId by remember { mutableLongStateOf(editing?.fromAccountId ?: active.getOrNull(0)?.account?.id ?: 0L) }
    var toId by remember { mutableLongStateOf(editing?.toAccountId ?: active.getOrNull(1)?.account?.id ?: active.getOrNull(0)?.account?.id ?: 0L) }
    var amount by remember { mutableStateOf(editing?.amountCents?.let { yuanPlain(it) }.orEmpty()) }
    var fee by remember { mutableStateOf(editing?.feeCents?.takeIf { it > 0 }?.let { yuanPlain(it) }.orEmpty()) }
    var note by remember { mutableStateOf(editing?.note.orEmpty()) }
    var date by remember { mutableStateOf(editing?.date ?: today()) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    val balanceOf = { id: Long -> accounts.firstOrNull { it.account.id == id }?.balanceCents ?: 0L }
    val cents = parseYuanToCents(amount)
    val feeCents = if (fee.isBlank()) 0L else parseYuanToCents(fee)
    val labelOf: (AccountBalance) -> String = { item ->
        buildString {
            append(item.account.name)
            append("  ¥")
            append(formatCents(item.balanceCents))
            if (item.account.archived) append("  ·停用")
        }
    }

    ModalBottomSheet(onDismissRequest = onClose, sheetState = sheetState, containerColor = palette.bg1) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(if (editing == null) "账户间转账" else "编辑转账", color = palette.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            if (active.size < 2) {
                Text("至少需要两个账户才能转账", color = palette.danger, fontSize = 13.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onClose) { Text("关闭", color = palette.text2) }
                }
            } else {
                Text("转出账户", color = palette.text3, fontSize = 12.sp)
                FlowRowChips(active.map { it.account.id to labelOf(it) }, setOf(fromId), { fromId = it; error = null })
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = palette.accent)
                }
                Text("转入账户", color = palette.text3, fontSize = 12.sp)
                FlowRowChips(active.map { it.account.id to labelOf(it) }, setOf(toId), { toId = it; error = null })
                if (fromId == toId && fromId != 0L) {
                    Text("转出与转入账户不能相同", color = palette.danger, fontSize = 12.sp)
                }
                JzTextInput(amount, { amount = sanitizeAmountInput(it); error = null }, "转账金额", prefix = "¥")
                if (cents != null && cents > 0 && fromId != 0L && toId != 0L && fromId != toId) {
                    val out = balanceOf(fromId) - cents - (feeCents ?: 0L)
                    val into = balanceOf(toId) + cents
                    Text(
                        "转出后 ${formatCents(out)} · 转入后 ${formatCents(into)}",
                        color = palette.text3,
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                JzTextInput(fee, { fee = sanitizeAmountInput(it); error = null }, "手续费（可选）", prefix = "¥")
                JzTextInput(date, { date = it.take(10); error = null }, "日期 YYYY-MM-DD")
                JzTextInput(note, { note = it.take(40) }, "备注（可选）")
                error?.let { Text(it, color = palette.danger, fontSize = 12.sp) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (editing != null) {
                        InlineAction("删除", {
                            scope.launch {
                                runCatching { vm.repository.deleteTransfer(editing.id) }
                                    .onFailure { error = it.message ?: "删除失败" }
                                vm.refresh()
                                onClose()
                            }
                        }, tint = palette.danger, icon = Icons.Filled.Delete)
                        Box(Modifier.size(8.dp))
                    }
                    TextButton(onClick = onClose) { Text("取消", color = palette.text2) }
                    TextButton(enabled = !saving, onClick = {
                        error = when {
                            fromId == 0L || toId == 0L -> "请选择转出与转入账户"
                            fromId == toId -> "转出与转入账户不能相同"
                            cents == null || cents <= 0 -> "请输入有效的转账金额"
                            feeCents == null -> "手续费格式不正确"
                            !isValidDate(date) -> "请输入有效日期"
                            balanceOf(fromId) - (cents ?: 0L) - (feeCents ?: 0L) < 0 -> "转出后余额会变成负数，请确认金额"
                            else -> null
                        }
                        if (error == null) {
                            scope.launch {
                                saving = true
                                val transfer = Transfer(
                                    id = editing?.id ?: 0L,
                                    fromAccountId = fromId,
                                    toAccountId = toId,
                                    amountCents = cents!!,
                                    feeCents = feeCents!!,
                                    note = note.trim(),
                                    date = date,
                                    createdAt = editing?.createdAt.orEmpty(),
                                )
                                runCatching {
                                    if (editing == null) vm.repository.addTransfer(transfer) else vm.repository.updateTransfer(transfer)
                                }.onFailure { error = it.message ?: "保存失败" }
                                saving = false
                                if (error == null) {
                                    vm.refresh()
                                    onClose()
                                }
                            }
                        }
                    }) { Text(if (saving) "保存中…" else "保存", color = palette.accent, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}
private fun accountIcon(name: String) = when (name) {
    "banknote" -> categoryIcon("banknote")
    "credit-card" -> categoryIcon("credit-card")
    "trending-up" -> categoryIcon("trending-up")
    "smartphone" -> categoryIcon("phone")
    "message-circle" -> categoryIcon("phone")
    else -> categoryIcon("more-horizontal")
}
