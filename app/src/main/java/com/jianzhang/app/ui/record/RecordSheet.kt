package com.jianzhang.app.ui.record

import com.jianzhang.app.ui.components.appShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jianzhang.app.data.Category
import com.jianzhang.app.data.Transaction
import com.jianzhang.app.model.AccountBalance
import com.jianzhang.app.model.TagUsage
import com.jianzhang.app.ui.JzViewModel
import com.jianzhang.app.ui.components.ChamferShape
import com.jianzhang.app.ui.components.ConfirmDeleteButton
import com.jianzhang.app.ui.components.FlowRowChips
import com.jianzhang.app.ui.components.SegmentedControl
import com.jianzhang.app.ui.components.hudGlow
import com.jianzhang.app.ui.theme.LocalPalette
import com.jianzhang.app.util.categoryIcon
import com.jianzhang.app.util.formatCents
import com.jianzhang.app.util.currentMonth
import com.jianzhang.app.util.parseYuanToCents
import com.jianzhang.app.util.sanitizeAmountInput
import com.jianzhang.app.util.today
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordSheet(vm: JzViewModel) {
    val palette = LocalPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editing = vm.editing
    val scope = rememberCoroutineScope()

    var typeIndex by remember { mutableStateOf(0) }
    var amount by remember { mutableStateOf("") }
    var categoryId by remember { mutableLongStateOf(0L) }
    var date by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var cats by remember { mutableStateOf<List<Category>>(emptyList()) }
    var accounts by remember { mutableStateOf<List<AccountBalance>>(emptyList()) }
    var tags by remember { mutableStateOf<List<TagUsage>>(emptyList()) }
    var accountId by remember { mutableLongStateOf(0L) }
    var tagIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var datePickerOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        cats = vm.repository.categories()
        // 含停用账户：编辑旧账目时能看见真实归属，新增账目默认仍选在用账户
        accounts = vm.repository.accounts(includeArchived = true)
        tags = vm.repository.tags()
        val ed = editing
        if (ed != null) {
            // 编辑时保留原账户（含“无账户”），不能默默换到别的账户上
            accountId = ed.accountId ?: 0L
            tagIds = vm.repository.tagIdsForTransaction(ed.id).toSet()
            typeIndex = if (ed.type == "income") 1 else 0
            val yuan = ed.amountCents / 100.0
            amount = if (yuan == yuan.toLong().toDouble()) yuan.toLong().toString() else yuan.toString()
            val candidates = cats.filter { it.type == ed.type }
            categoryId = if (candidates.any { it.id == ed.categoryId }) ed.categoryId else (candidates.firstOrNull()?.id ?: 0L)
            date = ed.date
            note = ed.note
        } else {
            val activeAccounts = accounts.filter { !it.account.archived }
            accountId = vm.settings.defaultAccountId.takeIf { id -> activeAccounts.any { it.account.id == id } }
                ?: activeAccounts.firstOrNull()?.account?.id
                ?: accounts.firstOrNull()?.account?.id ?: 0L
            typeIndex = if (vm.settings.defaultType == "income") 1 else 0
            val type = if (typeIndex == 1) "income" else "expense"
            val remembered = vm.lastCategoryId()
            val candidates = cats.filter { it.type == type }
            categoryId = if (vm.settings.rememberCategory && candidates.any { it.id == remembered }) {
                remembered
            } else {
                candidates.firstOrNull()?.id ?: 0L
            }
            date = if (vm.month == currentMonth()) today() else "${vm.month}-01"
            note = ""
        }
    }

    val visibleCats = remember(cats, typeIndex) {
        cats.filter { it.type == (if (typeIndex == 1) "income" else "expense") }
    }

    fun pickType(index: Int) {
        typeIndex = index
        val list = cats.filter { it.type == (if (index == 1) "income" else "expense") }
        categoryId = list.firstOrNull()?.id ?: 0L
    }

    fun doSave() {
        val cents = parseYuanToCents(amount)
        if (cents == null || cents <= 0) {
            error = "请输入有效的金额"
            return
        }
        if (categoryId <= 0) {
            error = "请选择分类"
            return
        }
        if (date.isEmpty()) {
            error = "请选择日期"
            return
        }
        error = null
        saving = true
        scope.launch {
            try {
                val type = if (typeIndex == 1) "income" else "expense"
                val tx = Transaction(
                    id = editing?.id ?: 0L,
                    type = type,
                    amountCents = cents,
                    categoryId = categoryId,
                    accountId = accountId.takeIf { it > 0 },
                    note = note.trim(),
                    date = date,
                )
                if (editing != null) {
                    vm.repository.updateTransaction(tx, tagIds.toList())
                } else {
                    vm.repository.addTransaction(tx, tagIds.toList())
                    if (vm.settings.rememberCategory) vm.rememberLastCategory(categoryId)
                }
                vm.refresh()
                vm.closeSheet()
            } catch (e: Exception) {
                error = "保存失败：${e.message ?: "未知错误"}"
            } finally {
                saving = false
            }
        }
    }

    fun doDelete() {
        val id = editing?.id ?: return
        scope.launch {
            vm.repository.deleteTransaction(id)
            vm.refresh()
            vm.closeSheet()
        }
    }

    ModalBottomSheet(
        onDismissRequest = vm::closeSheet,
        sheetState = sheetState,
        containerColor = palette.bg1,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (editing != null) "编辑账目" else "记一笔",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.text,
                    letterSpacing = 1.sp,
                    style = TextStyle(shadow = hudGlow(palette.accent, 0.3f, 12f)),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = vm::closeSheet) {
                    Icon(Icons.Filled.Close, contentDescription = "关闭", tint = palette.text2, modifier = Modifier.size(18.dp))
                }
            }

            SegmentedControl(
                options = listOf("支出", "收入"),
                selectedIndex = typeIndex,
                onSelect = ::pickType,
            )

            // 金额输入
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(appShape(12.dp))
                    .background(palette.bg)
                    .border(1.dp, palette.border, appShape(12.dp))
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("¥", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = palette.accent, style = TextStyle(shadow = hudGlow(palette.accent, 0.35f, 10f)))
                BasicTextField(
                    value = amount,
                    onValueChange = { amount = sanitizeAmountInput(it) },
                    modifier = Modifier.weight(1f).padding(start = 10.dp),
                    textStyle = TextStyle(color = palette.text, fontSize = 36.sp, fontWeight = FontWeight.Bold),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    decorationBox = { inner ->
                        Box {
                            if (amount.isEmpty()) {
                                Text("0.00", color = palette.text3.copy(alpha = 0.5f), fontSize = 36.sp, fontWeight = FontWeight.Bold)
                            }
                            inner()
                        }
                    },
                )
            }

            // 分类网格（4 列）
            visibleCats.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    row.forEach { cat ->
                        val active = cat.id == categoryId
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(appShape(8.dp))
                                .background(if (active) palette.accentWeak else Color.Transparent)
                                .clickable { categoryId = cat.id }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .clip(appShape(10.dp))
                                    .background(if (active) palette.accent else palette.border.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    categoryIcon(cat.icon),
                                    contentDescription = null,
                                    tint = if (active) Color(0xFF00171D) else palette.text2,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Text(
                                cat.name,
                                fontSize = 11.sp,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (active) palette.accent else palette.text2,
                            )
                        }
                    }
                }
            }

            if (accounts.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("账户", color = palette.text3, fontSize = 12.sp)
                    FlowRowChips(
                        options = accounts.map { item ->
                            item.account.id to buildString {
                                append(item.account.name)
                                append("  ¥")
                                append(formatCents(item.balanceCents))
                                if (item.account.archived) append("  ·停用")
                            }
                        },
                        selectedIds = setOf(accountId),
                        onToggle = { accountId = it },
                    )
                    if (accounts.firstOrNull { it.account.id == accountId }?.account?.archived == true) {
                        Text("该账户已停用，仍可记账；余额会计入净资产", color = palette.text3, fontSize = 11.sp)
                    }
                }
            }
            if (tags.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("标签", color = palette.text3, fontSize = 12.sp)
                    FlowRowChips(
                        options = tags.map { it.tag.id to it.tag.name },
                        selectedIds = tagIds,
                        onToggle = { id -> tagIds = if (id in tagIds) tagIds - id else tagIds + id },
                    )
                }
            }
            // 日期 + 备注
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(
                    onClick = { datePickerOpen = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(appShape(8.dp))
                        .background(palette.bg)
                        .border(1.dp, palette.border, appShape(8.dp)),
                ) {
                    Text(
                        if (date.isEmpty()) "选择日期" else date,
                        color = palette.text,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(vertical = 6.dp),
                    )
                }
                BasicTextField(
                    value = note,
                    onValueChange = { note = it.take(40) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(appShape(8.dp))
                        .background(palette.bg)
                        .border(1.dp, palette.border, appShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    textStyle = TextStyle(color = palette.text, fontSize = 15.sp),
                    singleLine = true,
                    decorationBox = { inner ->
                        Box {
                            if (note.isEmpty()) Text("备注（可选）", color = palette.text3, fontSize = 15.sp)
                            inner()
                        }
                    },
                )
            }

            error?.let {
                Text(
                    it,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.danger,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(appShape(8.dp))
                        .background(palette.danger.copy(alpha = 0.1f))
                        .border(1.dp, palette.danger.copy(alpha = 0.3f), appShape(8.dp))
                        .padding(12.dp),
                )
            }

            // 操作按钮
            val saveBrush = if (saving) {
                SolidColor(palette.text3)
            } else {
                Brush.linearGradient(listOf(palette.accent, palette.accentHover))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (editing != null) {
                    ConfirmDeleteButton(onConfirm = ::doDelete)
                }
                Box(Modifier.weight(1f))
                TextButton(onClick = vm::closeSheet) {
                    Text("取消", color = palette.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                Box(
                    Modifier
                        .clip(appShape(10.dp))
                        .shadow(8.dp, appShape(10.dp), ambientColor = palette.accent.copy(alpha = 0.4f))
                        .background(saveBrush)
                        .clickable(enabled = !saving) { doSave() }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (saving) "保存中…" else "保存",
                        color = if (saving) Color.White else Color(0xFF00171D),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                    )
                }
            }
        }
    }

    if (datePickerOpen) {
        val millis = runCatching {
            LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }.getOrDefault(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
        val state = rememberDatePickerState(initialSelectedDateMillis = millis)
        DatePickerDialog(
            onDismissRequest = { datePickerOpen = false },
            shape = appShape(10.dp),
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        date = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate().toString()
                    }
                    datePickerOpen = false
                }) { Text("确定", color = palette.accent) }
            },
            dismissButton = {
                TextButton(onClick = { datePickerOpen = false }) { Text("取消", color = palette.text2) }
            },
        ) {
            DatePicker(state = state)
        }
    }
}
