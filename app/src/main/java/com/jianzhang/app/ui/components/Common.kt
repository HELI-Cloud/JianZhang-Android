package com.jianzhang.app.ui.components

import com.jianzhang.app.ui.components.appShape
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jianzhang.app.ui.theme.LocalPalette
import com.jianzhang.app.util.formatCents
import kotlinx.coroutines.delay

/** 军团 HUD 玻璃卡片：切角面板 + 顶部青色描边 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    corner: Dp = 14.dp,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalPalette.current
    val shape = appShape(corner)
    Box(modifier) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            color = palette.card.copy(alpha = 0.86f),
            border = BorderStroke(1.dp, palette.border),
            shadowElevation = 2.dp,
        ) {
            Column {
                // 顶部青色渐变描边（被切角自然裁剪）
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, palette.accent.copy(alpha = 0.8f), Color.Transparent),
                            ),
                        ),
                )
                Column(Modifier.padding(contentPadding), content = content)
            }
        }
    }
}

/** 分段控制器（暗色轨道 + 切角 + 军团青激活块） */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val track = palette.bg1
    val shape = appShape(8.dp)
    Row(
        modifier
            .clip(shape)
            .background(track)
            .border(BorderStroke(1.dp, palette.border), shape)
            .padding(3.dp),
    ) {
        options.forEachIndexed { index, label ->
            val active = index == selectedIndex
            val pillModifier = Modifier
                .weight(1f)
                .clip(appShape(6.dp))
                .clickable { onSelect(index) }
                .padding(vertical = 7.dp)
            Box(
                if (active) {
                    pillModifier
                        .shadow(4.dp, appShape(6.dp), ambientColor = palette.accent.copy(alpha = 0.3f))
                        .background(Brush.linearGradient(listOf(palette.accent, palette.accentHover)))
                } else {
                    pillModifier.background(Color.Transparent)
                },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (active) Color(0xFF00171D) else palette.text2,
                    letterSpacing = 0.5.sp,
                )
            }
        }
    }
}

/** 军团风格开关（切角轨道，开启 = 极光绿） */
@Composable
fun JzSwitch(checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val shape = appShape(6.dp)
    val offset by animateDpAsState(if (checked) 24.dp else 3.dp, tween(180), label = "thumb")
    val trackColor by animateColorAsState(
        if (checked) palette.income.copy(alpha = 0.9f) else if (palette.isDark) Color(0xFF16222B) else Color(0xFFB9C9CF),
        tween(180),
        label = "track",
    )
    Box(
        modifier
            .width(54.dp)
            .height(30.dp)
            .clip(shape)
            .background(trackColor)
            .border(BorderStroke(1.dp, if (checked) palette.income.copy(alpha = 0.5f) else palette.border), shape)
            .clickable { onChange(!checked) },
    ) {
        Box(
            Modifier
                .offset(x = offset, y = 3.dp)
                .size(24.dp)
                .clip(appShape(4.dp))
                .shadow(2.dp, appShape(4.dp))
                .background(if (checked) Color.White else palette.text3),
        )
    }
}

/** 数字滚动动画文本 */
@Composable
fun CountUpText(
    targetCents: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    color: Color = Color.Unspecified,
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(targetCents) {
        anim.animateTo(
            targetCents.toFloat(),
            tween(700, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)),
        )
    }
    Text(
        text = formatCents(anim.value.toLong()),
        style = style,
        color = color,
        modifier = modifier,
    )
}

/** 空状态（切角卡片 + 青色辉光标题） */
@Composable
fun EmptyState(
    icon: String,
    title: String,
    sub: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    GlassCard(modifier = modifier.fillMaxWidth(), contentPadding = 32.dp) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(icon, fontSize = 56.sp)
            Text(
                title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = palette.text,
                style = TextStyle(shadow = hudGlow(palette.accent, 0.28f, 12f)),
            )
            Text(
                sub,
                fontSize = 13.sp,
                color = palette.text3,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            if (actionLabel != null && onAction != null) {
                PrimaryButton(
                    text = actionLabel,
                    onClick = onAction,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

/** 军团青渐变主按钮 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val palette = LocalPalette.current
    val shape = appShape(10.dp)
    Box(
        modifier
            .shadow(8.dp, shape, ambientColor = palette.accent.copy(alpha = 0.35f))
            .clip(shape)
            .background(
                if (enabled) {
                    Brush.linearGradient(listOf(palette.accent, palette.accentHover))
                } else {
                    Brush.linearGradient(listOf(palette.text3, palette.text3))
                }
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 24.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (enabled) Color(0xFF00171D) else Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
        )
    }
}

/** 二次确认删除按钮（2.6 秒倒计时，危险红） */
@Composable
fun ConfirmDeleteButton(
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "删除",
    confirmingLabel: String = "确认删除？",
) {
    val palette = LocalPalette.current
    var confirming by remember { mutableStateOf(false) }
    LaunchedEffect(confirming) {
        if (confirming) {
            delay(2600)
            confirming = false
        }
    }
    val shape = appShape(10.dp)
    Box(
        modifier
            .clip(shape)
            .background(if (confirming) palette.danger else palette.danger.copy(alpha = 0.10f))
            .border(BorderStroke(1.dp, if (confirming) palette.danger else palette.danger.copy(alpha = 0.3f)), shape)
            .clickable {
                if (confirming) onConfirm() else confirming = true
            }
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (confirming) confirmingLabel else label,
            color = if (confirming) Color.White else palette.danger,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** 搜索框（切角 + 聚焦青色光晕） */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "搜索分类或备注",
) {
    val palette = LocalPalette.current
    var focused by remember { mutableStateOf(false) }
    val shape = appShape(10.dp)
    Box(
        modifier
            .fillMaxWidth()
            .shadow(if (focused) 8.dp else 0.dp, shape, ambientColor = palette.accent.copy(alpha = 0.35f))
            .clip(shape)
            .background(palette.card.copy(alpha = 0.9f))
            .border(
                BorderStroke(
                    1.dp,
                    if (focused) palette.accent else palette.border,
                ),
                shape,
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = if (focused) palette.accent else palette.text3,
                modifier = Modifier.size(16.dp),
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { focused = it.isFocused }
                    .padding(horizontal = 12.dp, vertical = 13.dp),
                textStyle = TextStyle(color = palette.text, fontSize = 15.sp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) {
                            Text(placeholder, color = palette.text3, fontSize = 15.sp)
                        }
                        inner()
                    }
                },
            )
        }
    }
}

/** 卡片标题行（青色切角标记 + 标题 + 副标题 + 尾部插槽） */
@Composable
fun CardTitle(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val palette = LocalPalette.current
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(7.dp)
                    .clip(appShape(2.dp))
                    .background(Brush.linearGradient(listOf(palette.accent, palette.accentHover))),
            )
            Column(Modifier.padding(start = 8.dp)) {
                Text(
                    title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.text,
                    letterSpacing = 0.5.sp,
                )
                if (subtitle != null) {
                    Text(subtitle, fontSize = 12.sp, color = palette.text3)
                }
            }
        }
        trailing?.invoke()
    }
}
