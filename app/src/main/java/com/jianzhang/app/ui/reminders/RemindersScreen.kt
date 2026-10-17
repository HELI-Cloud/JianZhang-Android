package com.jianzhang.app.ui.reminders

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jianzhang.app.model.ReminderItem
import com.jianzhang.app.model.ReminderKind
import com.jianzhang.app.model.ReminderLevel
import com.jianzhang.app.ui.JzViewModel
import com.jianzhang.app.ui.components.InlineAction
import com.jianzhang.app.ui.components.PageHeader
import com.jianzhang.app.ui.components.SectionLabel
import com.jianzhang.app.ui.components.appShape
import com.jianzhang.app.ui.theme.LocalPalette
import kotlinx.coroutines.launch

@Composable
fun RemindersScreen(vm: JzViewModel) {
    val palette = LocalPalette.current
    val scope = rememberCoroutineScope()
    var dismissed by remember { mutableStateOf<Set<String>>(emptySet()) }
    var message by remember { mutableStateOf<String?>(null) }
    suspend fun reload() {
        vm.refreshReminders()
        dismissed = vm.repository.dismissedReminderKeys()
    }
    LaunchedEffect(vm.refreshKey, vm.settings) { reload() }
    val visible = vm.reminders.filter { it.key !in dismissed }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            PageHeader(
                title = "提醒中心",
                subtitle = if (visible.isEmpty()) "当前没有待处理提醒" else "${visible.size} 条待处理提醒",
                trailing = {
                    if (dismissed.isNotEmpty()) InlineAction("恢复已忽略", {
                        scope.launch { vm.clearReminderDismissals(); reload() }
                    })
                },
            )
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                SectionLabel("待处理")
                message?.let { Text(it, color = palette.income, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp)) }
                visible.forEach { item ->
                    ReminderRow(
                        item = item,
                        onConfirm = if (item.confirmable && item.ruleId != null) {
                            {
                                scope.launch {
                                    val rule = vm.repository.recurringRules().firstOrNull { it.id == item.ruleId }
                                    if (rule == null) message = "周期规则不存在"
                                    else runCatching { vm.repository.confirmRecurring(rule) }
                                        .onSuccess { message = "已确认入账"; vm.refresh(); reload() }
                                        .onFailure { message = it.message ?: "确认失败" }
                                }
                            }
                        } else null,
                        onDismiss = {
                            scope.launch {
                                vm.dismissReminder(item.key)
                                dismissed = dismissed + item.key
                            }
                        },
                    )
                }
                if (visible.isEmpty()) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Notifications, contentDescription = null, tint = palette.text3, modifier = Modifier.size(34.dp))
                        Text("暂无提醒", color = palette.text2, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                        Text("预算、周期账单和记账节奏提醒会显示在这里。", color = palette.text3, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderRow(
    item: ReminderItem,
    onConfirm: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val palette = LocalPalette.current
    val tint = when (item.level) {
        ReminderLevel.DANGER -> palette.danger
        ReminderLevel.WARN -> palette.expense
        ReminderLevel.INFO -> palette.accent
    }
    val icon = when (item.kind) {
        ReminderKind.BUDGET -> Icons.Filled.Warning
        ReminderKind.BILL -> Icons.Filled.Schedule
        else -> Icons.Filled.Notifications
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(appShape(8.dp))
            .background(tint.copy(alpha = 0.075f))
            .border(1.dp, tint.copy(alpha = 0.22f), appShape(8.dp))
            .padding(13.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).clip(appShape(7.dp)).background(tint.copy(alpha = 0.13f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(17.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(item.title, color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(item.body, color = palette.text2, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            }
            Icon(
                Icons.Filled.Close,
                contentDescription = "忽略",
                tint = palette.text3,
                modifier = Modifier.size(24.dp).clip(appShape(5.dp)).clickable(onClick = onDismiss).padding(4.dp),
            )
        }
        if (onConfirm != null) {
            Row(Modifier.fillMaxWidth().padding(top = 9.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onConfirm) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = palette.income, modifier = Modifier.size(15.dp))
                    Text("确认入账", color = palette.income, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}
