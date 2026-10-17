package com.jianzhang.app.ui.more

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jianzhang.app.ui.JzViewModel
import com.jianzhang.app.ui.components.PageHeader
import com.jianzhang.app.ui.components.SectionLabel
import com.jianzhang.app.ui.components.appShape
import com.jianzhang.app.ui.theme.LocalPalette

private data class MoreItem(val route: String, val title: String, val subtitle: String, val icon: ImageVector)

private val ITEMS = listOf(
    MoreItem("budget", "预算", "月度总预算与分类预算", Icons.Filled.Savings),
    MoreItem("recurring", "周期记账", "房租、工资和订阅自动入账", Icons.Filled.Repeat),
    MoreItem("reminders", "提醒中心", "超支、到期账单与记账节奏", Icons.Filled.Notifications),
    MoreItem("tags", "标签管理", "用标签跨分类整理账目", Icons.Filled.Label),
    MoreItem("analysis", "AI 分析", "基于本机账目的消费洞察", Icons.Filled.AutoAwesome),
    MoreItem("settings", "设置", "外观、提醒、备份与安全", Icons.Filled.Settings),
    MoreItem("about", "关于简账", "版本与开源信息", Icons.Filled.Info),
)

@Composable
fun MoreScreen(vm: JzViewModel) {
    val palette = LocalPalette.current
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { PageHeader("更多", "常用管理与应用设置") }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                SectionLabel("功能")
                ITEMS.forEach { item ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(appShape(7.dp))
                            .clickable { vm.navigate(item.route) }
                            .padding(horizontal = 4.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(38.dp).clip(appShape(8.dp)).background(palette.accentWeak),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(item.icon, contentDescription = null, tint = palette.accent, modifier = Modifier.size(19.dp))
                        }
                        Column(Modifier.weight(1f).padding(horizontal = 11.dp)) {
                            Text(item.title, color = palette.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text(item.subtitle, color = palette.text3, fontSize = 11.5.sp)
                        }
                        if (item.route == "reminders" && vm.reminderCount > 0) {
                            Box(
                                Modifier.clip(appShape(10.dp)).background(palette.expense).padding(horizontal = 7.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(vm.reminderCount.toString(), color = androidx.compose.ui.graphics.Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = palette.text3, modifier = Modifier.size(17.dp))
                    }
                }
            }
        }
    }
}
