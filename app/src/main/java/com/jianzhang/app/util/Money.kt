package com.jianzhang.app.util

import java.util.Locale

/** 将"分"格式化为千分位、两位小数金额，如 123456 -> "1,234.56" */
fun formatCents(cents: Long): String {
    val sign = if (cents < 0) "-" else ""
    val abs = kotlin.math.abs(cents)
    val yuan = abs / 100
    val fen = abs % 100
    return "$sign${groupDigits(yuan)}.${fen.toString().padStart(2, '0')}"
}

private fun groupDigits(n: Long): String {
    val s = n.toString()
    val out = StringBuilder()
    for (i in s.indices) {
        if (i > 0 && (s.length - i) % 3 == 0) out.append(',')
        out.append(s[i])
    }
    return out.toString()
}

/** 解析用户输入的元为分；非法输入返回 null（与桌面版规则一致） */
/**
 * 将"分"还原为可直接编辑的元字符串：无千分位、无科学计数法、去掉多余末尾零。
 * 用于金额输入框回填，避免 (cents / 100.0).toString() 在大额时输出 1.9E10 这类科学计数法。
 * 0 返回空串（表单占位为空比 "0" 更自然）。
 */
fun yuanPlain(cents: Long): String {
    if (cents == 0L) return ""
    val sign = if (cents < 0) "-" else ""
    val abs = kotlin.math.abs(cents)
    val yuan = abs / 100
    val fen = (abs % 100).toInt()
    val frac = when {
        fen == 0 -> ""
        fen % 10 == 0 -> ".${fen / 10}"
        else -> ".${fen.toString().padStart(2, '0')}"
    }
    return "$sign$yuan$frac"
}

fun parseYuanToCents(input: String): Long? {
    val s = input.trim()
    if (!Regex("""\d+(\.\d{0,2})?""").matches(s)) return null
    val parts = s.split('.')
    val yuan = parts[0].toLongOrNull() ?: return null
    val fen = parts.getOrNull(1).orEmpty().padEnd(2, '0').take(2).toLong()
    val cents = yuan * 100 + fen
    return if (cents >= 0) cents else null
}

/** 金额输入清洗：仅数字与一个小数点、最多两位小数、去除前导零 */
fun sanitizeAmountInput(raw: String): String {
    var s = raw.replace(Regex("[^0-9.]"), "")
    s = Regex("""^(\d+\.\d{0,2}).*$""").replace(s, "$1")
    s = Regex("""^(0+)(\d)""").replace(s, "$2")
    return s
}

/** 图表坐标轴金额缩写：>=1 万显示 x.x万，否则取整 */
fun yuanAxis(cents: Long): String {
    val yuan = cents / 100.0
    return if (yuan >= 10000) {
        String.format(Locale.US, "%.1f万", yuan / 10000)
    } else {
        kotlin.math.round(yuan).toLong().toString()
    }
}
