package com.jianzhang.app.ui.analysis

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jianzhang.app.data.AiAnalyzer
import com.jianzhang.app.data.AiConfig
import com.jianzhang.app.data.AiConfigStore
import com.jianzhang.app.data.AiFocus
import com.jianzhang.app.data.AiRange
import com.jianzhang.app.ui.JzViewModel
import com.jianzhang.app.ui.components.GlassCard
import com.jianzhang.app.ui.components.PrimaryButton
import com.jianzhang.app.ui.components.SegmentedControl
import com.jianzhang.app.ui.components.appShape
import com.jianzhang.app.ui.components.hudGlow
import com.jianzhang.app.ui.theme.LocalPalette
import kotlinx.coroutines.launch

@Composable
fun AnalysisScreen(vm: JzViewModel) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    val configStore = remember { AiConfigStore(context.applicationContext) }
    val analyzer = remember { AiAnalyzer() }
    val scope = rememberCoroutineScope()

    var endpoint by remember { mutableStateOf(configStore.load().endpoint) }
    var apiKey by remember { mutableStateOf(configStore.load().apiKey) }
    var model by remember { mutableStateOf(configStore.load().model) }
    var temperature by remember { mutableStateOf(configStore.load().temperature.toString()) }
    var timeout by remember { mutableStateOf(configStore.load().timeoutSecs.toString()) }
    var rangeIndex by remember { mutableStateOf(0) }
    var focusIndex by remember { mutableStateOf(0) }
    var customQuestion by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var running by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    val ranges = listOf(AiRange.MONTH, AiRange.SIX_MONTHS, AiRange.ALL)
    val focuses = listOf(AiFocus.HEALTH, AiFocus.STRUCTURE, AiFocus.BUDGET, AiFocus.CUSTOM)

    fun currentConfig() = AiConfig(
        endpoint = endpoint,
        apiKey = apiKey,
        model = model,
        temperature = temperature.toFloatOrNull() ?: 0.35f,
        timeoutSecs = timeout.toIntOrNull() ?: 90,
    )

    fun hasConfig(): Boolean = endpoint.isNotBlank() && apiKey.isNotBlank() && model.isNotBlank()

    fun save() {
        when {
            endpoint.isBlank() -> error = "请填写接口地址"
            apiKey.isBlank() -> error = "请填写 API Key"
            model.isBlank() -> error = "请填写模型名称"
            else -> {
                configStore.save(currentConfig())
                error = null
                saved = true
            }
        }
    }

    fun resetDefaults() {
        endpoint = "https://api.deepseek.com/chat/completions"
        model = "deepseek-chat"
        temperature = "0.35"
        timeout = "90"
        error = null
    }

    fun runAnalysis() {
        if (!hasConfig()) {
            error = "请先填写 AI 模型配置并保存"
            return
        }
        running = true
        error = null
        result = ""
        scope.launch {
            try {
                result = analyzer.run(
                    repository = vm.repository,
                    config = currentConfig(),
                    range = ranges[rangeIndex],
                    focus = focuses[focusIndex],
                    customQuestion = customQuestion,
                    selectedMonth = vm.month,
                )
            } catch (e: Exception) {
                error = e.message ?: "AI 分析失败"
            } finally {
                running = false
            }
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                "AI 分析",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = palette.text,
                letterSpacing = 1.sp,
                style = TextStyle(shadow = hudGlow(palette.accent, 0.32f, 14f)),
            )
        }

        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Text(
                    "AI 模型配置",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.text2,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                AiField(
                    label = "接口地址",
                    value = endpoint,
                    onValueChange = { endpoint = it; saved = false },
                    placeholder = "https://api.deepseek.com/chat/completions",
                )
                AiField(
                    label = "API Key",
                    value = apiKey,
                    onValueChange = { apiKey = it; saved = false },
                    placeholder = "sk-...",
                    password = true,
                    supporting = "当前状态：${configStore.maskApiKey(apiKey)}",
                )
                AiField(
                    label = "模型名称",
                    value = model,
                    onValueChange = { model = it; saved = false },
                    placeholder = "deepseek-chat",
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AiField(
                        label = "温度",
                        value = temperature,
                        onValueChange = { temperature = it.filter { c -> c.isDigit() || c == '.' }; saved = false },
                        placeholder = "0.35",
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f),
                    )
                    AiField(
                        label = "超时秒数",
                        value = timeout,
                        onValueChange = { timeout = it.filter { c -> c.isDigit() }; saved = false },
                        placeholder = "90",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    "API Key 仅保存在本机，并在点击“开始分析”时发送到你填写的接口。",
                    fontSize = 12.sp,
                    color = palette.text3,
                    modifier = Modifier.padding(top = 10.dp),
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = ::resetDefaults) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, tint = palette.text2, modifier = Modifier.size(16.dp))
                        Text("恢复默认", color = palette.text2, modifier = Modifier.padding(start = 4.dp))
                    }
                    Box(Modifier.weight(1f))
                    PrimaryButton(text = if (saved) "已保存" else "保存配置", onClick = ::save)
                }
            }
        }

        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Text(
                    "生成分析",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.text2,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Text("分析范围", fontSize = 13.sp, color = palette.text2, modifier = Modifier.padding(bottom = 6.dp))
                SegmentedControl(
                    options = listOf("本月", "近 6 个月", "全部历史"),
                    selectedIndex = rangeIndex,
                    onSelect = { rangeIndex = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("分析重点", fontSize = 13.sp, color = palette.text2, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
                SegmentedControl(
                    options = listOf("财务健康", "消费结构", "预算风险", "自定义"),
                    selectedIndex = focusIndex,
                    onSelect = { focusIndex = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (focusIndex == 3) {
                    AiField(
                        label = "补充问题",
                        value = customQuestion,
                        onValueChange = { customQuestion = it },
                        placeholder = "例如：我这个月有哪些不合理支出？下个月应该优先控制哪一类？",
                        multiline = true,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("范围：${listOf("本月", "近 6 个月", "全部历史")[rangeIndex]}", fontSize = 12.sp, color = palette.text3)
                    Box(Modifier.weight(1f))
                    PrimaryButton(text = if (running) "正在分析…" else "开始分析", onClick = ::runAnalysis, enabled = !running)
                }
            }
        }

        error?.let {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(appShape(10.dp))
                        .background(palette.danger.copy(alpha = 0.12f))
                        .border(1.dp, palette.danger.copy(alpha = 0.3f), appShape(10.dp))
                        .padding(14.dp),
                ) {
                    Text(it, fontSize = 13.sp, color = palette.danger)
                }
            }
        }

        if (result.isNotEmpty()) {
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = palette.accent, modifier = Modifier.size(17.dp))
                        Text(
                            "分析结果",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.text,
                            modifier = Modifier.padding(start = 7.dp),
                        )
                    }
                    SelectionContainer {
                        Text(
                            result,
                            fontSize = 13.5.sp,
                            lineHeight = 21.sp,
                            color = palette.text,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp)
                                .clip(appShape(10.dp))
                                .background(palette.bg)
                                .border(1.dp, palette.border, appShape(10.dp))
                                .padding(16.dp),
                        )
                    }
                }
            }
        } else if (!running) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(appShape(12.dp))
                        .background(palette.card.copy(alpha = 0.58f))
                        .border(1.dp, palette.border, appShape(12.dp))
                        .padding(vertical = 34.dp, horizontal = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = palette.accent, modifier = Modifier.size(28.dp))
                        Text("配置完成后，选择范围和分析重点，生成一份只基于本机账本数据的结论。", color = palette.text3, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AiField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
    multiline: Boolean = false,
    supporting: String? = null,
) {
    val palette = LocalPalette.current
    Column(modifier.fillMaxWidth().padding(vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = palette.text2)
        Box(
            Modifier
                .fillMaxWidth()
                .clip(appShape(9.dp))
                .background(palette.bg)
                .border(1.dp, palette.border, appShape(9.dp))
                .padding(horizontal = 14.dp, vertical = if (multiline) 12.dp else 11.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = palette.text, fontSize = 14.sp),
                singleLine = !multiline,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) Text(placeholder, color = palette.text3, fontSize = 14.sp)
                        inner()
                    }
                },
            )
        }
        if (supporting != null) {
            Text(supporting, fontSize = 11.sp, color = palette.text3)
        }
    }
}
