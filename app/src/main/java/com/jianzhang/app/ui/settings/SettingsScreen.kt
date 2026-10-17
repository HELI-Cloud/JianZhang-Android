package com.jianzhang.app.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

import com.jianzhang.app.ui.components.appShape
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jianzhang.app.data.Category
import com.jianzhang.app.model.AppSettings
import com.jianzhang.app.model.TxType
import com.jianzhang.app.ui.JzViewModel
import com.jianzhang.app.ui.components.ChamferShape
import com.jianzhang.app.ui.components.GlassCard
import com.jianzhang.app.ui.components.JzSwitch
import com.jianzhang.app.ui.components.hudGlow
import com.jianzhang.app.ui.components.SegmentedControl
import com.jianzhang.app.ui.theme.LocalPalette
import com.jianzhang.app.util.CATEGORY_ICON_NAMES
import com.jianzhang.app.util.categoryIcon
import com.jianzhang.app.util.today
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(vm: JzViewModel) {
    val palette = LocalPalette.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var cats by remember { mutableStateOf<List<Category>>(emptyList()) }
    var accounts by remember { mutableStateOf<List<com.jianzhang.app.model.AccountBalance>>(emptyList()) }

    LaunchedEffect(vm.refreshKey) {
        cats = vm.repository.categories()
        accounts = vm.repository.accounts()
    }

    val expenseCats = cats.filter { it.type == "expense" }
    val incomeCats = cats.filter { it.type == "income" }

    // 添加分类表单状态
    var addType by remember { mutableStateOf("expense") }
    var addOpen by remember { mutableStateOf(false) }
    var addName by remember { mutableStateOf("") }
    var addIcon by remember { mutableStateOf("more-horizontal") }
    var addError by remember { mutableStateOf<String?>(null) }

    // 删除分类二次确认
    var confirmId by remember { mutableStateOf(0L) }
    LaunchedEffect(confirmId) {
        if (confirmId != 0L) {
            delay(2600)
            confirmId = 0L
        }
    }

    // 修改密码
    var pwdOpen by remember { mutableStateOf(false) }
    var curPwd by remember { mutableStateOf("") }
    var newPwd by remember { mutableStateOf("") }
    var newPwd2 by remember { mutableStateOf("") }
    var pwdError by remember { mutableStateOf<String?>(null) }

    // 数据管理
    var importReplace by remember { mutableStateOf(false) }
    var dataMsg by remember { mutableStateOf<String?>(null) }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) scope.launch {
            try {
                val json = vm.repository.buildBackupJson()
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                dataMsg = "备份已导出到所选位置"
            } catch (e: Exception) {
                dataMsg = "导出失败：${e.message ?: "未知错误"}"
            }
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) scope.launch {
            try {
                val csv = vm.repository.buildCsv()
                context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray(Charsets.UTF_8)) }
                dataMsg = "流水 CSV 已导出"
            } catch (e: Exception) {
                dataMsg = "导出失败：${e.message ?: "未知错误"}"
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) scope.launch {
            try {
                val raw = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                if (raw == null) {
                    dataMsg = "读取文件失败"
                    return@launch
                }
                val r = vm.repository.importBackup(raw, importReplace)
                vm.refresh()
                dataMsg = "导入完成（${if (importReplace) "覆盖" else "合并"}）：账目 ${r.transactions}、账户 ${r.accounts}、转账 ${r.transfers}、标签 ${r.tags}、周期 ${r.recurringRules}"
            } catch (e: Exception) {
                dataMsg = "导入失败：${e.message ?: "未知错误"}"
            }
        }
    }

    val notificationGranted = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun openAddForm(type: String) {
        addType = type
        addName = ""
        addIcon = "more-horizontal"
        addError = null
        addOpen = true
    }

    fun saveCategory() {
        val name = addName.trim()
        if (name.isEmpty()) {
            addError = "请输入分类名称"
            return
        }
        val dup = cats.any { it.type == addType && it.name == name }
        if (dup) {
            addError = "该分类已存在"
            return
        }
        scope.launch {
            try {
                vm.repository.addCategory(if (addType == "income") TxType.INCOME else TxType.EXPENSE, name, addIcon)
                vm.refresh()
                addOpen = false
            } catch (e: Exception) {
                addError = "添加失败：${e.message ?: "未知错误"}"
            }
        }
    }

    fun deleteCategory(cat: Category) {
        if (confirmId != cat.id) {
            confirmId = cat.id
            return
        }
        confirmId = 0L
        scope.launch {
            vm.repository.deleteCategory(cat)
            vm.refresh()
        }
    }

    fun update(patch: (AppSettings) -> AppSettings) {
        vm.saveSettings(patch(vm.settings))
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
                item {
            Text(
                "设置",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = palette.text,
                letterSpacing = 1.sp,
                style = TextStyle(shadow = hudGlow(palette.accent, 0.32f, 14f)),
            )
        }

        // 显示
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("显示", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = palette.text2, modifier = Modifier.padding(bottom = 10.dp))
                SettingPills(
                    label = "字体大小",
                    desc = "调整整个界面的文字与元素大小",
                    options = listOf("小", "标准", "大", "特大"),
                    values = listOf("small", "medium", "large", "xlarge"),
                    selected = vm.settings.fontSize,
                    onSelect = { v -> update { it.copy(fontSize = v) } },
                )
                SettingPills(
                    label = "字体格式",
                    desc = "选择界面使用的字体",
                    options = listOf("系统默认", "等宽"),
                    values = listOf("default", "monospace"),
                    selected = vm.settings.fontFamily,
                    onSelect = { v -> update { it.copy(fontFamily = v) } },
                )
                SettingPills(
                    label = "外观",
                    desc = "深色 / 浅色模式",
                    options = listOf("跟随系统", "浅色", "深色"),
                    values = listOf("system", "light", "dark"),
                    selected = vm.settings.themeMode,
                    onSelect = { v -> update { it.copy(themeMode = v) } },
                )
                SettingPills(
                    label = "界面风格",
                    desc = "黑客终端保留切角 HUD；圆角玻璃使用更柔和的卡片与按钮",
                    options = listOf("黑客终端", "圆角玻璃"),
                    values = listOf("hacker", "glass"),
                    selected = vm.settings.uiStyle,
                    onSelect = { v -> update { it.copy(uiStyle = v) } },
                )
            }
        }

        // 记账偏好
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("记账偏好", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = palette.text2, modifier = Modifier.padding(bottom = 10.dp))
                SettingPills(
                    label = "默认账目类型",
                    desc = "打开“记一笔”时默认选中支出或收入",
                    options = listOf("支出", "收入"),
                    values = listOf("expense", "income"),
                    selected = vm.settings.defaultType,
                    onSelect = { v -> update { it.copy(defaultType = v) } },
                )
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("记住上次分类", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = palette.text)
                        Text("记一笔时自动选中上次使用的分类", fontSize = 12.sp, color = palette.text3)
                    }
                    JzSwitch(checked = vm.settings.rememberCategory, onChange = { v -> update { it.copy(rememberCategory = v) } })
                }
                if (accounts.isNotEmpty()) {
                    SettingPills(
                        label = "默认账户",
                        desc = "打开“记一笔”时默认使用的账户",
                        options = listOf("自动") + accounts.map { it.account.name },
                        values = listOf("0") + accounts.map { it.account.id.toString() },
                        selected = vm.settings.defaultAccountId.toString(),
                        onSelect = { v -> update { it.copy(defaultAccountId = v.toLongOrNull() ?: 0L) } },
                    )
                }
            }
        }

        // 提醒与通知
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("提醒与通知", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = palette.text2, modifier = Modifier.padding(bottom = 6.dp))
                ToggleSetting("超支预警", "总预算或分类预算达到 80% / 超支时提醒", vm.settings.budgetAlert) { v -> update { it.copy(budgetAlert = v) } }
                ToggleSetting("账单到期提醒", "周期账目到期前 3 天提醒", vm.settings.billAlert) { v -> update { it.copy(billAlert = v) } }
                ToggleSetting("每日记账提醒", "当日未记账时在设定时间提醒", vm.settings.dailyReminder) { v -> update { it.copy(dailyReminder = v) } }
                ToggleSetting("每周记账提醒", "所选星期及之后仍无记录时提醒", vm.settings.weeklyReminder) { v -> update { it.copy(weeklyReminder = v) } }
                SettingPills(
                    label = "每日提醒时间",
                    desc = "Android 可能在省电模式下略有延后",
                    options = listOf("08:00", "12:00", "18:00", "21:00"),
                    values = listOf("08:00", "12:00", "18:00", "21:00"),
                    selected = vm.settings.dailyReminderTime,
                    onSelect = { v -> update { it.copy(dailyReminderTime = v) } },
                )
                SettingPills(
                    label = "每周提醒日",
                    desc = "到达该星期后仍无记录时提醒",
                    options = listOf("周日", "周一", "周二", "周三", "周四", "周五", "周六"),
                    values = listOf("0", "1", "2", "3", "4", "5", "6"),
                    selected = vm.settings.weeklyReminderWeekday.toString(),
                    onSelect = { v -> update { it.copy(weeklyReminderWeekday = v.toIntOrNull() ?: 0) } },
                )
                SettingActionRow(
                    label = "系统通知权限",
                    desc = if (notificationGranted) "已授权" else "Android 13 及以上需要通知权限",
                    actionLabel = if (notificationGranted) "已开启" else "去授权",
                ) {
                    if (Build.VERSION.SDK_INT >= 33 && !notificationGranted) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                ToggleSetting("同时发送系统通知", "关闭后仅在应用内提醒中心显示", vm.settings.systemNotify) { v -> update { it.copy(systemNotify = v) } }
            }
        }

        // 安全与隐私（v0.3.0）
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("安全与隐私", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = palette.text2, modifier = Modifier.padding(bottom = 6.dp))
                SettingActionRow(
                    label = "修改密码",
                    desc = "验证当前密码后设置新密码",
                    actionLabel = "修改密码",
                ) {
                    pwdOpen = true
                    curPwd = ""
                    newPwd = ""
                    newPwd2 = ""
                    pwdError = null
                }
                SettingActionRow(
                    label = "立即锁定",
                    desc = "回到密码锁屏，隐藏所有账目数据",
                    actionLabel = "锁定",
                ) {
                    vm.lockNow()
                }
            }
        }

        // 数据管理（v0.3.0）
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("数据管理", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = palette.text2, modifier = Modifier.padding(bottom = 6.dp))
                SettingActionRow(
                    label = "导出备份",
                    desc = "分类、账目与预算导出为 JSON 文件",
                    actionLabel = "导出",
                ) {
                    exportJsonLauncher.launch("简账备份-${today()}.json")
                }
                SettingActionRow(
                    label = "导出流水 CSV",
                    desc = "全部账目导出，可用 Excel / WPS 打开",
                    actionLabel = "导出",
                ) {
                    exportCsvLauncher.launch("简账流水-${today()}.csv")
                }

                Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("导入备份", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = palette.text)
                    Text("合并会按分类名称去重；覆盖会先清空现有数据再恢复", fontSize = 12.sp, color = palette.text3)
                    SegmentedControl(
                        options = listOf("合并", "覆盖"),
                        selectedIndex = if (importReplace) 1 else 0,
                        onSelect = { importReplace = it == 1 },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(appShape(8.dp))
                            .background(palette.accentWeak)
                            .border(1.dp, palette.border, appShape(8.dp))
                            .clickable { importLauncher.launch(arrayOf("application/json")) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("选择文件并导入", color = palette.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                dataMsg?.let {
                    Text(
                        it,
                        fontSize = 12.sp,
                        color = palette.income,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }

        // 分类管理
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("分类管理", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = palette.text2, modifier = Modifier.padding(bottom = 6.dp))
                CategoryGroup(
                    title = "支出分类",
                    cats = expenseCats,
                    addOpen = addOpen && addType == "expense",
                    addName = addName,
                    addIcon = addIcon,
                    addError = addError,
                    onAddClick = { openAddForm("expense") },
                    onNameChange = { addName = it.take(10) },
                    onIconSelect = { addIcon = it },
                    onCancel = { addOpen = false; addError = null },
                    onSave = ::saveCategory,
                    confirmId = confirmId,
                    onDelete = ::deleteCategory,
                )
                CategoryGroup(
                    title = "收入分类",
                    cats = incomeCats,
                    addOpen = addOpen && addType == "income",
                    addName = addName,
                    addIcon = addIcon,
                    addError = addError,
                    onAddClick = { openAddForm("income") },
                    onNameChange = { addName = it.take(10) },
                    onIconSelect = { addIcon = it },
                    onCancel = { addOpen = false; addError = null },
                    onSave = ::saveCategory,
                    confirmId = confirmId,
                    onDelete = ::deleteCategory,
                )
                Text(
                    "自定义分类删除后，其下账目会自动归入“其他”。",
                    fontSize = 12.sp,
                    color = palette.text3,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }

    // 修改密码弹窗
    if (pwdOpen) {
        AlertDialog(
            onDismissRequest = { pwdOpen = false },
            shape = appShape(12.dp),
            title = { Text("修改密码") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PwdField(value = curPwd, onValueChange = { curPwd = it }, placeholder = "当前密码")
                    PwdField(value = newPwd, onValueChange = { newPwd = it }, placeholder = "新密码（至少 4 位）")
                    PwdField(value = newPwd2, onValueChange = { newPwd2 = it }, placeholder = "再次输入新密码")
                    pwdError?.let {
                        Text(it, fontSize = 12.sp, color = palette.danger)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    when {
                        newPwd.length < 4 -> pwdError = "新密码至少需要 4 位"
                        newPwd != newPwd2 -> pwdError = "两次输入的新密码不一致"
                        else -> {
                            if (vm.changePassword(curPwd, newPwd)) {
                                pwdOpen = false
                                curPwd = ""
                                newPwd = ""
                                newPwd2 = ""
                                pwdError = null
                            } else {
                                pwdError = "当前密码不正确"
                            }
                        }
                    }
                }) { Text("确认", color = palette.accent, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { pwdOpen = false }) { Text("取消", color = palette.text2) }
            },
            containerColor = palette.card,
        )
    }
}

@Composable
private fun ToggleSetting(
    label: String,
    desc: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val palette = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = palette.text)
            Text(desc, fontSize = 12.sp, color = palette.text3)
        }
        JzSwitch(checked = checked, onChange = onChange)
    }
}

@Composable
private fun SettingActionRow(
    label: String,
    desc: String,
    actionLabel: String,
    onClick: () -> Unit,
) {
    val palette = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = palette.text)
            Text(desc, fontSize = 12.sp, color = palette.text3)
        }
        TextButton(onClick = onClick) {
            Text(actionLabel, color = palette.accent, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PwdField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    val palette = LocalPalette.current
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .clip(appShape(8.dp))
            .background(palette.bg)
            .border(1.dp, palette.border, appShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        textStyle = TextStyle(color = palette.text, fontSize = 14.sp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = PasswordVisualTransformation(),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(placeholder, color = palette.text3, fontSize = 14.sp)
                inner()
            }
        },
    )
}

@Composable
private fun SettingPills(
    label: String,
    desc: String,
    options: List<String>,
    values: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    val palette = LocalPalette.current
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = palette.text)
        Text(desc, fontSize = 12.sp, color = palette.text3)
        SegmentedControl(
            options = options,
            selectedIndex = values.indexOf(selected).coerceAtLeast(0),
            onSelect = { onSelect(values[it]) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CategoryGroup(
    title: String,
    cats: List<Category>,
    addOpen: Boolean,
    addName: String,
    addIcon: String,
    addError: String?,
    onAddClick: () -> Unit,
    onNameChange: (String) -> Unit,
    onIconSelect: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    confirmId: Long,
    onDelete: (Category) -> Unit,
) {
    val palette = LocalPalette.current
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = palette.text, modifier = Modifier.weight(1f))
            Row(
                Modifier
                    .clip(appShape(6.dp))
                    .background(palette.accentWeak)
                    .border(1.dp, palette.border, appShape(6.dp))
                    .clickable(onClick = onAddClick)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = palette.accent, modifier = Modifier.size(14.dp))
                Text("添加", color = palette.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 2.dp))
            }
        }

        if (addOpen) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(appShape(8.dp))
                    .background(palette.bg)
                    .border(1.dp, palette.border, appShape(8.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BasicTextField(
                    value = addName,
                    onValueChange = onNameChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(appShape(6.dp))
                        .background(palette.card)
                        .border(1.dp, palette.border, appShape(6.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    textStyle = TextStyle(color = palette.text, fontSize = 14.sp),
                    singleLine = true,
                    decorationBox = { inner ->
                        Box {
                            if (addName.isEmpty()) Text("分类名称，如：宠物", color = palette.text3, fontSize = 14.sp)
                            inner()
                        }
                    },
                )
                // 图标选择器（8 列）
                CATEGORY_ICON_NAMES.chunked(8).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { name ->
                            val active = addIcon == name
                            Box(
                                Modifier
                                    .size(34.dp)
                                    .clip(appShape(7.dp))
                                    .background(if (active) palette.accent else palette.card)
                                    .border(1.dp, if (active) palette.accent else palette.border, appShape(7.dp))
                                    .clickable { onIconSelect(name) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    categoryIcon(name),
                                    contentDescription = null,
                                    tint = if (active) Color(0xFF00171D) else palette.text2,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
                addError?.let {
                    Text(it, fontSize = 12.sp, color = palette.danger)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f))
                    TextButton(onClick = onCancel) { Text("取消", color = palette.text2) }
                    TextButton(onClick = onSave) { Text("保存", color = palette.accent, fontWeight = FontWeight.SemiBold) }
                }
            }
        }

        if (cats.isEmpty()) {
            Text("暂无分类", fontSize = 13.sp, color = palette.text3, modifier = Modifier.padding(vertical = 8.dp))
        } else {
            cats.forEach { cat ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(32.dp).clip(appShape(7.dp)).background(palette.bg1).border(1.dp, palette.border, appShape(7.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(categoryIcon(cat.icon), contentDescription = null, tint = palette.text2, modifier = Modifier.size(16.dp))
                    }
                    Text(cat.name, fontSize = 15.sp, color = palette.text, modifier = Modifier.weight(1f).padding(start = 12.dp))
                    if (cat.builtin) {
                        Text(
                            "内置",
                            fontSize = 11.sp,
                            color = palette.accent,
                            modifier = Modifier
                                .clip(appShape(5.dp))
                                .background(palette.accentWeak)
                                .border(1.dp, palette.border, appShape(5.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    } else {
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(appShape(7.dp))
                                .background(if (confirmId == cat.id) palette.danger else palette.danger.copy(alpha = 0.12f))
                                .border(1.dp, palette.danger.copy(alpha = 0.3f), appShape(7.dp))
                                .clickable { onDelete(cat) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (confirmId == cat.id) Icons.Filled.Check else Icons.Filled.Delete,
                                contentDescription = "删除",
                                tint = if (confirmId == cat.id) Color.White else palette.danger,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
