package com.jianzhang.app.ui.overview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jianzhang.app.ui.theme.LocalPalette
import com.jianzhang.app.util.formatCents
import com.jianzhang.app.util.yuanAxis
import kotlin.math.min
import kotlin.math.pow

private fun niceCents(cents: Long): Long {
    if (cents <= 0) return 100L
    val yuan = (cents / 100).coerceAtLeast(1)
    val magnitude = 10.0.pow(kotlin.math.floor(kotlin.math.log10(yuan.toDouble()))).toLong()
    val normalized = yuan / magnitude
    val nice = when {
        normalized <= 1 -> 1
        normalized <= 2 -> 2
        normalized <= 5 -> 5
        else -> 10
    }
    return nice * magnitude * 100
}

/** 年度双序列柱状图；点击月份可查看当月收入与支出。 */
@Composable
fun MonthlyBarsChart(
    labels: List<String>,
    income: List<Long>,
    expense: List<Long>,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val textMeasurer = rememberTextMeasurer()
    val progress = rememberChartProgress(760)
    var selected by remember { mutableIntStateOf(-1) }
    val maxCents = remember(income, expense) { niceCents((income + expense).maxOrNull() ?: 0L) }
    val axisStyle = TextStyle(color = palette.text3, fontSize = 10.sp)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(210.dp)
            .pointerInput(labels.size) {
                detectTapGestures { pos ->
                    if (labels.isEmpty()) return@detectTapGestures
                    val left = 38.dp.toPx()
                    val right = 8.dp.toPx()
                    val slot = (size.width - left - right) / labels.size
                    selected = ((pos.x - left) / slot).toInt().coerceIn(0, labels.lastIndex)
                }
            },
    ) {
        val left = 38.dp.toPx()
        val right = 8.dp.toPx()
        val top = 8.dp.toPx()
        val bottom = 22.dp.toPx()
        val chartWidth = size.width - left - right
        val chartHeight = size.height - top - bottom
        if (chartWidth <= 0 || chartHeight <= 0 || labels.isEmpty()) return@Canvas

        val levels = 4
        for (i in 0..levels) {
            val y = top + chartHeight - chartHeight * i / levels
            drawLine(palette.border, Offset(left, y), Offset(size.width - right, y), 1.dp.toPx())
            drawText(textMeasurer, yuanAxis(maxCents * i / levels), topLeft = Offset(0f, y - 7.dp.toPx()), style = axisStyle)
        }

        val slot = chartWidth / labels.size
        val barWidth = min(slot * 0.28f, 13.dp.toPx())
        labels.forEachIndexed { index, label ->
            val centerX = left + slot * (index + 0.5f)
            val incomeHeight = chartHeight * (income.getOrElse(index) { 0L }.toFloat() / maxCents.toFloat()).coerceIn(0f, 1f) * progress
            val expenseHeight = chartHeight * (expense.getOrElse(index) { 0L }.toFloat() / maxCents.toFloat()).coerceIn(0f, 1f) * progress
            if (selected == index) {
                drawRect(palette.accent.copy(alpha = 0.06f), Offset(left + slot * index, top), Size(slot, chartHeight))
            }
            drawRoundRect(
                palette.income,
                Offset(centerX - barWidth - 1.dp.toPx(), top + chartHeight - incomeHeight),
                Size(barWidth, incomeHeight),
                CornerRadius(4.dp.toPx()),
            )
            drawRoundRect(
                palette.expense,
                Offset(centerX + 1.dp.toPx(), top + chartHeight - expenseHeight),
                Size(barWidth, expenseHeight),
                CornerRadius(4.dp.toPx()),
            )
            drawText(textMeasurer, label, topLeft = Offset(centerX - 11.dp.toPx(), size.height - 18.dp.toPx()), style = axisStyle)
        }
        if (selected in labels.indices) {
            val text = "${labels[selected]}  收 ¥${formatCents(income.getOrElse(selected) { 0L })}  支 ¥${formatCents(expense.getOrElse(selected) { 0L })}"
            drawText(
                textMeasurer,
                text,
                topLeft = Offset(left, top + 2.dp.toPx()),
                style = TextStyle(color = palette.text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
            )
        }
    }
}
