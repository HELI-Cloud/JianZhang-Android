package com.jianzhang.app.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.jianzhang.app.model.UiStyle
import com.jianzhang.app.ui.theme.LocalUiStyle

/**
 * 按当前界面风格返回形状：
 * - hacker：保留切角 HUD 面板
 * - glass：使用圆角玻璃面板
 */
@Composable
fun appShape(cut: Dp, rounded: Dp = cut): Shape =
    if (LocalUiStyle.current == UiStyle.GLASS.key) {
        RoundedCornerShape(rounded)
    } else {
        ChamferShape(cut)
    }
