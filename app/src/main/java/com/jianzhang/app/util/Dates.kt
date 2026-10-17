package com.jianzhang.app.util

import java.time.LocalDate
import java.time.YearMonth

fun pad(n: Int): String = n.toString().padStart(2, '0')

fun currentMonth(): String {
    val d = LocalDate.now()
    return "${d.year}-${pad(d.monthValue)}"
}

fun today(): String = LocalDate.now().toString()

fun isValidDate(date: String): Boolean = runCatching { LocalDate.parse(date) }.isSuccess

fun isValidMonth(month: String): Boolean {
    if (!Regex("""\d{4}-\d{2}""").matches(month)) return false
    return runCatching { YearMonth.parse(month) }.isSuccess
}

fun shiftMonth(month: String, delta: Int): String {
    val base = YearMonth.parse(month)
    val d = base.plusMonths(delta.toLong())
    return "${d.year}-${pad(d.monthValue)}"
}

fun lastNMonths(n: Int, endMonth: String): List<String> {
    if (n <= 0) return emptyList()
    if (!isValidMonth(endMonth)) return emptyList()
    return (n - 1 downTo 0).map { shiftMonth(endMonth, -it) }
}

fun monthLabel(month: String): String {
    if (!isValidMonth(month)) return month
    val parts = month.split('-')
    return "${parts[0]}年${parts[1].toInt()}月"
}

fun shortMonth(month: String): String =
    if (isValidMonth(month)) "${month.split('-')[1].toInt()}月" else month

fun dateLabel(date: String): String {
    if (!isValidDate(date)) return date
    val d = LocalDate.parse(date)
    val week = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")[d.dayOfWeek.value % 7]
    return "${d.monthValue}月${d.dayOfMonth}日 $week"
}

fun monthDays(month: String): Int =
    if (isValidMonth(month)) YearMonth.parse(month).lengthOfMonth() else 0

fun startOfMonth(month: String): String = if (isValidMonth(month)) "$month-01" else month

fun endOfMonth(month: String): String {
    val days = monthDays(month)
    return if (days > 0) "$month-${pad(days)}" else month
}

fun daysBetween(start: String, end: String): Long {
    if (!isValidDate(start) || !isValidDate(end) || start > end) return 0
    return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(start), LocalDate.parse(end)) + 1
}

fun monthsBetween(start: String, end: String): List<String> {
    if (!isValidDate(start) || !isValidDate(end) || start > end) return emptyList()
    val first = YearMonth.from(LocalDate.parse(start))
    val last = YearMonth.from(LocalDate.parse(end))
    val out = mutableListOf<String>()
    var cursor = first
    while (cursor <= last && out.size < 240) {
        out += "${cursor.year}-${pad(cursor.monthValue)}"
        cursor = cursor.plusMonths(1)
    }
    return out
}

fun daysLeftInMonth(month: String): Int {
    if (!isValidMonth(month)) return 0
    val total = monthDays(month)
    return if (month == currentMonth()) {
        val day = LocalDate.now().dayOfMonth
        maxOf(0, total - day + 1)
    } else if (month < currentMonth()) {
        0
    } else {
        total
    }
}
