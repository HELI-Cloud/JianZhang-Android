package com.jianzhang.app.ui.overview

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jianzhang.app.model.CategoryTotal
import com.jianzhang.app.ui.theme.CategoryColors
import com.jianzhang.app.ui.theme.LocalPalette
import com.jianzhang.app.util.formatCents
import com.jianzhang.app.util.yuanAxis
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/** 图表入场动画进度（0->1，800ms cubicOut） */
@Composable
fun rememberChartProgress(durationMs: Int = 800): Float {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        anim.animateTo(1f, tween(durationMs, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)))
    }
    return anim.value
}

private fun niceCeil(cents: Long): Long {
    if (cents <= 0) return 100
    val yuan = cents / 100
    if (yuan <= 0) return 100
    val mag = 10.0.pow(kotlin.math.floor(kotlin.math.log10(yuan.toDouble())).toDouble()).toLong()
    val normalized = yuan / mag
    val nice = when {
        normalized <= 1 -> 1
        normalized <= 2 -> 2
        normalized <= 5 -> 5
        else -> 10
    }
    return nice * mag * 100
}

/** 平滑折线（中点二次贝塞尔） */
private fun Path.addSmoothed(points: List<Offset>) {
    if (points.isEmpty()) return
    if (points.size == 1) {
        moveTo(points[0].x, points[0].y)
        return
    }
    val start = Offset((points[0].x + points[1].x) / 2f, (points[0].y + points[1].y) / 2f)
    moveTo(start.x, start.y)
    for (i in 1 until points.size - 1) {
        val p0 = points[i]
        val p1 = points[i + 1]
        val mid = Offset((p0.x + p1.x) / 2f, (p0.y + p1.y) / 2f)
        quadraticBezierTo(p0.x, p0.y, mid.x, mid.y)
    }
    lineTo(points.last().x, points.last().y)
}

/** 收支趋势折线图（近 6 个月） */
@Composable
fun LineTrendChart(
    labels: List<String>,
    income: List<Long>,
    expense: List<Long>,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val textMeasurer = rememberTextMeasurer()
    val progress = rememberChartProgress(800)
    var selected by remember { mutableIntStateOf(-1) }
    val allMax = remember(income, expense) { (income + expense).maxOrNull() ?: 0L }
    val maxCents = remember(allMax) { niceCeil(allMax) }
    val labelStyle = TextStyle(color = palette.text3, fontSize = 10.sp)
    val incomeColor = palette.income
    val expenseColor = palette.expense

    Canvas(
        modifier
            .fillMaxWidth()
            .height(190.dp)
            .pointerInput(labels.size) {
                detectTapGestures { pos ->
                    if (labels.size < 2) return@detectTapGestures
                    val left = 40.dp.toPx()
                    val right = 10.dp.toPx()
                    val slot = (size.width - left - right) / (labels.size - 1)
                    val idx = ((pos.x - left) / slot).roundToInt()
                    selected = if (selected == idx) -1 else idx.coerceIn(0, labels.size - 1)
                }
            },
    ) {
        val left = 40.dp.toPx()
        val right = 10.dp.toPx()
        val top = 10.dp.toPx()
        val bottom = 20.dp.toPx()
        val w = size.width - left - right
        val h = size.height - top - bottom
        if (w <= 0 || h <= 0 || labels.size < 2) return@Canvas

        val y = { value: Long -> top + h - h * (value.toFloat() / maxCents.toFloat()).coerceIn(0f, 1f) }
        val x = { index: Int -> left + w * index / (labels.size - 1) }

        // 网格与 Y 轴标签
        val levels = 4
        for (i in 0..levels) {
            val ly = top + h - h * i / levels
            drawLine(palette.border, Offset(left, ly), Offset(size.width - right, ly), 1.dp.toPx())
            drawText(textMeasurer, yuanAxis(maxCents * i / levels), topLeft = Offset(2.dp.toPx(), ly - 7.dp.toPx()), style = labelStyle)
        }
        // X 轴标签
        labels.forEachIndexed { i, label ->
            drawText(textMeasurer, label, topLeft = Offset(x(i) - 12.dp.toPx(), size.height - 17.dp.toPx()), style = labelStyle)
        }

        fun pointsOf(values: List<Long>): List<Offset> =
            values.mapIndexed { i, v -> Offset(x(i), y(v)) }

        // 渐变面积 + 发光线条（按 progress 从左向右揭示）
        val series = listOf(
            Triple(income, incomeColor, "收入"),
            Triple(expense, expenseColor, "支出"),
        )
        for ((values, color, _) in series) {
            val pts = pointsOf(values)
            if (pts.isEmpty()) continue
            val areaPath = Path().apply {
                addSmoothed(pts)
                lineTo(pts.last().x, top + h)
                lineTo(pts.first().x, top + h)
                close()
            }
            val linePath = Path().apply { addSmoothed(pts) }
            clipRect(left, top, left + w * progress, top + h) {
                drawPath(
                    areaPath,
                    Brush.verticalGradient(listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0f))),
                )
                // 外发光
                drawPath(linePath, color.copy(alpha = 0.22f), style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round))
                drawPath(linePath, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            }
        }

        // 选中提示
        if (selected in 0 until labels.size) {
            val sx = x(selected)
            drawLine(
                palette.text3.copy(alpha = 0.6f),
                Offset(sx, top),
                Offset(sx, top + h),
                1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
            )
            val pts = listOf(pointsOf(income), pointsOf(expense))
            val colors = listOf(incomeColor, expenseColor)
            val names = listOf("收入", "支出")
            for ((i, list) in pts.withIndex()) {
                val p = list.getOrNull(selected) ?: continue
                drawCircle(Color.White, 5.dp.toPx(), p)
                drawCircle(colors[i], 3.2.dp.toPx(), p)
            }
            // 数值提示框
            val lines = names.mapIndexedNotNull { i, name ->
                val v = listOf(income, expense)[i].getOrNull(selected) ?: return@mapIndexedNotNull null
                "$name  ¥${formatCents(v)}"
            }
            val boxW = 118.dp.toPx()
            val lineH = 16.dp.toPx()
            val boxH = lines.size * lineH + 10.dp.toPx()
            var boxX = sx - boxW / 2f
            boxX = boxX.coerceIn(left, size.width - right - boxW)
            val boxY = top + 2.dp.toPx()
            drawRoundRect(
                palette.card.copy(alpha = 0.96f),
                topLeft = Offset(boxX, boxY),
                size = Size(boxW, boxH),
                cornerRadius = CornerRadius(10.dp.toPx()),
            )
            drawRoundRect(
                palette.border,
                topLeft = Offset(boxX, boxY),
                size = Size(boxW, boxH),
                cornerRadius = CornerRadius(10.dp.toPx()),
                style = Stroke(1.dp.toPx()),
            )
            lines.forEachIndexed { i, line ->
                val color = colors[i]
                drawText(
                    textMeasurer,
                    line,
                    topLeft = Offset(boxX + 10.dp.toPx(), boxY + 6.dp.toPx() + i * lineH),
                    style = TextStyle(color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

/** 每日支出柱状图 */
@Composable
fun DailyBarChart(
    days: List<Pair<Int, Long>>,
    monthDaysCount: Int,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val textMeasurer = rememberTextMeasurer()
    val progress = rememberChartProgress(800)
    var selected by remember { mutableIntStateOf(-1) }
    val maxDay = remember(days) { days.maxOfOrNull { it.second } ?: 0L }
    val maxCents = remember(maxDay) { niceCeil(maxDay) }
    val labelStyle = TextStyle(color = palette.text3, fontSize = 10.sp)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(170.dp)
            .pointerInput(days) {
                detectTapGestures { pos ->
                    val left = 36.dp.toPx()
                    val right = 8.dp.toPx()
                    val slot = (size.width - left - right) / monthDaysCount.toFloat()
                    val day = (((pos.x - left) / slot).roundToInt() + 1).coerceIn(1, monthDaysCount)
                    selected = if (selected == day) -1 else day
                }
            },
    ) {
        val left = 36.dp.toPx()
        val right = 8.dp.toPx()
        val top = 8.dp.toPx()
        val bottom = 20.dp.toPx()
        val w = size.width - left - right
        val h = size.height - top - bottom
        if (w <= 0 || h <= 0 || monthDaysCount <= 0) return@Canvas

        // 网格与 Y 轴
        val levels = 3
        for (i in 0..levels) {
            val ly = top + h - h * i / levels
            drawLine(palette.border, Offset(left, ly), Offset(size.width - right, ly), 1.dp.toPx())
            drawText(textMeasurer, yuanAxis(maxCents * i / levels), topLeft = Offset(2.dp.toPx(), ly - 7.dp.toPx()), style = labelStyle)
        }
        // X 轴日期（每 5 天一个标签）
        val step = 5
        var day = 1
        while (day <= monthDaysCount) {
            val dx = left + (day - 0.5f) * w / monthDaysCount
            drawText(textMeasurer, day.toString(), topLeft = Offset(dx - 6.dp.toPx(), size.height - 17.dp.toPx()), style = labelStyle)
            day += step
        }

        // 柱体
        val slot = w / monthDaysCount
        val barW = min(slot * 0.62f, 24.dp.toPx())
        for ((d, value) in days) {
            val cx = left + (d - 0.5f) * slot
            val barH = h * (value.toFloat() / maxCents.toFloat()).coerceIn(0f, 1f) * progress
            val isSel = selected == d
            val brush = Brush.verticalGradient(
                if (isSel) listOf(palette.expense, palette.expense)
                else listOf(palette.expense, palette.expense.copy(alpha = 0.55f)),
            )
            drawRoundRect(
                brush,
                topLeft = Offset(cx - barW / 2f, top + h - barH),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(min(6.dp.toPx(), barW / 2f)),
            )
        }

        // 选中提示
        if (selected in 1..monthDaysCount) {
            val value = days.firstOrNull { it.first == selected }?.second ?: 0L
            val sx = left + (selected - 0.5f) * slot
            val tipText = "${selected}日  ¥${formatCents(value)}"
            val layout = textMeasurer.measure(AnnotatedString(tipText), TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold))
            val boxW = layout.size.width + 16.dp.toPx()
            val boxH = layout.size.height + 8.dp.toPx()
            var boxX = sx - boxW / 2f
            boxX = boxX.coerceIn(left, size.width - right - boxW)
            val boxY = top + 4.dp.toPx()
            drawRoundRect(
                if (value > 0) palette.expense else palette.text3,
                topLeft = Offset(boxX, boxY),
                size = Size(boxW, boxH),
                cornerRadius = CornerRadius(8.dp.toPx()),
            )
            drawText(textMeasurer, tipText, topLeft = Offset(boxX + 8.dp.toPx(), boxY + 4.dp.toPx()), style = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold))
        }
    }
}

/** 分类横向条形图 */
@Composable
fun CategoryBars(items: List<CategoryTotal>, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val progress = rememberChartProgress(800)
    val maxCents = remember(items) { items.maxOfOrNull { it.totalCents } ?: 0L }
    if (items.isEmpty()) return
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEachIndexed { index, item ->
            val color = CategoryColors[index % CategoryColors.size]
            val frac = if (maxCents > 0) item.totalCents.toFloat() / maxCents.toFloat() else 0f
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.name,
                    fontSize = 12.sp,
                    color = palette.text,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(54.dp),
                    maxLines = 1,
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(16.dp)
                        .background(palette.border, RoundedCornerShape(7.dp)),
                ) {
                    Canvas(Modifier.fillMaxWidth().height(16.dp)) {
                        val bw = size.width * frac * progress
                        if (bw > 0) {
                            drawRoundRect(
                                color,
                                topLeft = Offset(0f, 0f),
                                size = Size(bw, size.height),
                                cornerRadius = CornerRadius(7.dp.toPx()),
                            )
                        }
                    }
                }
                Text(
                    "¥${formatCents(item.totalCents)}",
                    fontSize = 11.sp,
                    color = palette.text2,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

/** 分类占比环形图（中心显示总金额 + 图例） */
@Composable
fun DonutChart(
    items: List<CategoryTotal>,
    centerTitle: String,
    centerSub: String,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val progress = rememberChartProgress(800)
    val total = remember(items) { items.sumOf { it.totalCents } }
    val thickness = 22.dp

    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (total <= 0 || items.isEmpty()) return@Column
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(150.dp)) {
                val stroke = thickness.toPx()
                var start = -90f
                val gap = 2.5f
                items.forEachIndexed { index, item ->
                    val sweep = 360f * item.totalCents.toFloat() / total.toFloat() * progress
                    val drawSweep = (sweep - gap).coerceAtLeast(0f)
                    drawArc(
                        color = CategoryColors[index % CategoryColors.size],
                        startAngle = start + gap / 2f,
                        sweepAngle = drawSweep,
                        useCenter = false,
                        topLeft = Offset(stroke / 2f, stroke / 2f),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(stroke),
                    )
                    start += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(centerTitle, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = palette.text)
                Text(centerSub, fontSize = 11.sp, color = palette.text3)
            }
        }
        // 图例
        Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items.forEachIndexed { index, item ->
                val color = CategoryColors[index % CategoryColors.size]
                val percent = (item.totalCents.toFloat() / total.toFloat() * 100).roundToInt()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(color, RoundedCornerShape(4.dp)))
                    Text(item.name, fontSize = 12.sp, color = palette.text, modifier = Modifier.padding(start = 6.dp))
                    SpacerW(8.dp)
                    Text("$percent%", fontSize = 11.sp, color = palette.text3)
                    Box(Modifier.weight(1f))
                    Text("¥${formatCents(item.totalCents)}", fontSize = 12.sp, color = palette.text2)
                }
            }
        }
    }
}

@Composable
private fun SpacerW(width: androidx.compose.ui.unit.Dp) {
    Box(Modifier.width(width))
}

/** 收支比对双向条形（左支出 / 右收入） */
@Composable
fun CompareBars(income: Long, expense: Long, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val progress = rememberChartProgress(900)
    val maxVal = maxOf(income, expense, 1L)
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier.fillMaxWidth().height(56.dp)) {
        val top = 2.dp.toPx()
        val bottom = size.height - 20.dp.toPx()
        val h = bottom - top
        val cx = size.width / 2f
        val halfW = (size.width - 16.dp.toPx()) / 2f
        val r = 8.dp.toPx()

        // 中心刻度线
        drawLine(palette.border, Offset(cx, top), Offset(cx, bottom), 1.dp.toPx())

        // 支出（左侧，右侧圆角）
        val expW = halfW * (expense.toFloat() / maxVal.toFloat()) * progress
        if (expW > 0) {
            val path = Path().apply {
                moveTo(cx - expW + r, top)
                lineTo(cx - r, top)
                arcTo(Rect(cx - 2 * r, top, cx, top + 2 * r), 270f, 90f, false)
                lineTo(cx, bottom)
                lineTo(cx - expW, bottom)
                close()
            }
            drawPath(
                path,
                Brush.horizontalGradient(listOf(palette.expense.copy(alpha = 0.7f), palette.expense)),
            )
        }

        // 收入（右侧，左侧圆角）
        val incW = halfW * (income.toFloat() / maxVal.toFloat()) * progress
        if (incW > 0) {
            val path = Path().apply {
                moveTo(cx + r, top)
                lineTo(cx + incW - r, top)
                arcTo(Rect(cx + incW - 2 * r, top, cx + incW, top + 2 * r), 270f, 90f, false)
                lineTo(cx + incW, bottom)
                lineTo(cx, bottom)
                close()
            }
            drawPath(
                path,
                Brush.horizontalGradient(listOf(palette.income, palette.income.copy(alpha = 0.7f))),
            )
        }

        // 标签
        val expLabel = "支出 ¥${formatCents(expense)}"
        val incLabel = "收入 ¥${formatCents(income)}"
        val expLayout = textMeasurer.measure(AnnotatedString(expLabel), TextStyle(color = palette.expense, fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
        val incLayout = textMeasurer.measure(AnnotatedString(incLabel), TextStyle(color = palette.income, fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
        drawText(textMeasurer, expLabel, topLeft = Offset(0f, bottom + 6.dp.toPx()), style = TextStyle(color = palette.expense, fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
        drawText(
            textMeasurer,
            incLabel,
            topLeft = Offset(size.width - incLayout.size.width, bottom + 6.dp.toPx()),
            style = TextStyle(color = palette.income, fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
        )
    }
}

/** 带闪光效果的进度条 */
@Composable
fun ShimmerProgress(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val animProgress = rememberChartProgress(900)
    val fill = progress * animProgress
    Box(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(palette.border, RoundedCornerShape(4.dp)),
    ) {
        Canvas(Modifier.fillMaxWidth().height(8.dp)) {
            val fw = size.width * fill.coerceIn(0f, 1f)
            if (fw > 0) {
                drawRoundRect(color, size = Size(fw, size.height), cornerRadius = CornerRadius(4.dp.toPx()))
                // 高光
                if (animProgress > 0f) {
                    val glowX = size.width * animProgress
                    drawRect(
                        Brush.horizontalGradient(
                            listOf(Color.White.copy(alpha = 0f), Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0f)),
                            startX = glowX - 24.dp.toPx(),
                            endX = glowX + 24.dp.toPx(),
                        ),
                        topLeft = Offset(0f, 0f),
                        size = Size(fw, size.height),
                    )
                }
            }
        }
    }
}
