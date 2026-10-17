package com.jianzhang.app.ui.tags

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.jianzhang.app.data.Tag
import com.jianzhang.app.model.TagUsage
import com.jianzhang.app.ui.JzViewModel
import com.jianzhang.app.ui.components.FlowRowChips
import com.jianzhang.app.ui.components.InlineAction
import com.jianzhang.app.ui.components.JzTextInput
import com.jianzhang.app.ui.components.PageHeader
import com.jianzhang.app.ui.components.SectionLabel
import com.jianzhang.app.ui.components.appShape
import com.jianzhang.app.ui.theme.LocalPalette
import kotlinx.coroutines.launch

private val TAG_COLORS = listOf("#00E5FF", "#00D6A3", "#FF5A1F", "#FFB020", "#A35CFF", "#2F80ED", "#FF2D78")

@Composable
fun TagsScreen(vm: JzViewModel) {
    val palette = LocalPalette.current
    val scope = rememberCoroutineScope()
    var tags by remember { mutableStateOf<List<TagUsage>>(emptyList()) }
    var editing by remember { mutableStateOf<Tag?>(null) }
    var dialogOpen by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        tags = vm.repository.tags()
    }
    LaunchedEffect(vm.refreshKey) { reload() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            PageHeader(
                title = "标签",
                subtitle = "${tags.size} 个标签 · 用于跨分类归类与筛选统计",
                trailing = { InlineAction("新增", { editing = null; dialogOpen = true }, icon = Icons.Filled.Add) },
            )
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                SectionLabel("全部标签")
                tags.forEach { usage ->
                    val tag = usage.tag
                    val tint = parseColor(tag.color, palette.accent)
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(22.dp).clip(CircleShape).background(tint.copy(alpha = 0.18f)).border(1.dp, tint, CircleShape))
                        Column(Modifier.weight(1f).padding(start = 11.dp)) {
                            Text(tag.name, color = palette.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("${usage.usageCount} 笔账目", color = palette.text3, fontSize = 11.5.sp)
                        }
                        IconButton(onClick = { editing = tag; dialogOpen = true }) {
                            Icon(Icons.Filled.Edit, "编辑", tint = palette.text3, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = {
                            scope.launch {
                                vm.repository.deleteTag(tag.id)
                                vm.refresh()
                            }
                        }) { Icon(Icons.Filled.Delete, "删除", tint = palette.danger, modifier = Modifier.size(16.dp)) }
                    }
                }
                if (tags.isEmpty()) {
                    Text("还没有标签，例如“旅行”“报销”“家庭”。", color = palette.text3, fontSize = 12.sp, modifier = Modifier.padding(vertical = 20.dp))
                }
            }
        }
    }

    if (dialogOpen) {
        var name by remember(editing) { mutableStateOf(editing?.name.orEmpty()) }
        var color by remember(editing) { mutableStateOf(editing?.color ?: TAG_COLORS.first()) }
        AlertDialog(
            onDismissRequest = { dialogOpen = false },
            shape = appShape(10.dp),
            containerColor = palette.card,
            title = { Text(if (editing == null) "新增标签" else "编辑标签") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    JzTextInput(name, { name = it.take(12); error = null }, "标签名称")
                    FlowRowChips(TAG_COLORS.map { it.hashCode().toLong() to it }, setOf(color.hashCode().toLong()), {
                        color = TAG_COLORS.first { c -> c.hashCode().toLong() == it }
                    })
                    Box(Modifier.fillMaxWidth().padding(top = 2.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(38.dp).clip(CircleShape).background(parseColor(color, palette.accent).copy(alpha = 0.22f)).border(2.dp, parseColor(color, palette.accent), CircleShape))
                    }
                    error?.let { Text(it, color = palette.danger, fontSize = 12.sp) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isBlank()) error = "请输入标签名称"
                    else scope.launch {
                        runCatching {
                            if (editing == null) vm.repository.addTag(name, color)
                            else vm.repository.updateTag(editing!!.copy(name = name.trim(), color = color))
                            vm.refresh()
                            dialogOpen = false
                        }.onFailure { error = it.message ?: "保存失败" }
                    }
                }) { Text("保存", color = palette.accent, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = { TextButton(onClick = { dialogOpen = false }) { Text("取消", color = palette.text2) } },
        )
    }
}

private fun parseColor(value: String, fallback: Color): Color =
    runCatching { Color(android.graphics.Color.parseColor(value)) }.getOrDefault(fallback)
