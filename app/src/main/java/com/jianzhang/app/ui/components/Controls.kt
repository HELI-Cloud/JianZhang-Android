package com.jianzhang.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jianzhang.app.ui.theme.LocalPalette

@Composable
fun PageHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val palette = LocalPalette.current
    Row(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = palette.text, fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            if (subtitle != null) {
                Text(subtitle, color = palette.text3, fontSize = 12.5.sp, modifier = Modifier.padding(top = 3.dp))
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun JzTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    prefix: String? = null,
) {
    val palette = LocalPalette.current
    var focused by remember { mutableStateOf(false) }
    val shape = appShape(7.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.bg1.copy(alpha = 0.92f))
            .border(1.dp, if (focused) palette.accent else palette.border, shape)
            .padding(horizontal = 13.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (prefix != null) {
            Text(prefix, color = palette.accent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .padding(start = if (prefix == null) 0.dp else 8.dp)
                .onFocusChanged { focused = it.isFocused },
            textStyle = TextStyle(color = palette.text, fontSize = 15.sp),
            singleLine = singleLine,
            maxLines = maxLines,
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text(placeholder, color = palette.text3, fontSize = 14.sp)
                    inner()
                }
            },
        )
    }
}

@Composable
fun ChoiceChips(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        options.forEachIndexed { index, label ->
            val active = index == selectedIndex
            Box(
                Modifier
                    .weight(1f)
                    .clip(appShape(6.dp))
                    .background(if (active) palette.accentWeak else palette.bg1.copy(alpha = 0.7f))
                    .border(1.dp, if (active) palette.accent else palette.border, appShape(6.dp))
                    .clickable { onSelect(index) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (active) palette.accent else palette.text2,
                    fontSize = 12.5.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun FlowRowChips(
    options: List<Pair<Long, String>>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color? = null,
) {
    val palette = LocalPalette.current
    val tint = accent ?: palette.accent
    androidx.compose.foundation.layout.FlowRow(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        options.forEach { (id, label) ->
            val active = id in selectedIds
            Box(
                Modifier
                    .clip(appShape(6.dp))
                    .background(if (active) tint.copy(alpha = 0.18f) else palette.bg1)
                    .border(1.dp, if (active) tint else palette.border, appShape(6.dp))
                    .clickable { onToggle(id) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Text(label, color = if (active) tint else palette.text2, fontSize = 12.5.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
}

@Composable
fun SectionLabel(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val palette = LocalPalette.current
    Row(modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(palette.accent))
        Column(Modifier.weight(1f).padding(start = 9.dp)) {
            Text(title, color = palette.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) Text(subtitle, color = palette.text3, fontSize = 11.5.sp)
        }
        trailing?.invoke()
    }
}

@Composable
fun InlineAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tint: Color? = null,
) {
    val palette = LocalPalette.current
    val color = tint ?: palette.accent
    Row(
        modifier
            .clip(appShape(6.dp))
            .background(color.copy(alpha = 0.11f))
            .border(1.dp, color.copy(alpha = 0.32f), appShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
        }
        Text(text, color = color, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = if (icon == null) 0.dp else 6.dp))
    }
}
