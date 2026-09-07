package com.jianzhang.app.util

import java.time.LocalDate

fun pad(n: Int): String = n.toString().padStart(2, '0')

fun currentMonth(): String {
    val d = LocalDate.now()
    return "${d.year}-${pad(d.monthValue)}"
}

fun today(): String = LocalDate.now().toString()

fun shiftMonth(month: String, delta: Int): String {
    val parts = month.split('-')
    val d = LocalDate.of(parts[0].toInt(), parts[1].toInt(), 1).plusMonths(delta.toLong())
    return "${d.year}-${pad(d.monthValue)}"
}

fun lastNMonths(n: Int, endMonth: String): List<String> = (n - 1 downTo 0).map { shiftMonth(endMonth, -it) }

fun monthLabel(month: String): String {
    val parts = month.split('-')
    return "${parts[0]}年${parts[1].toInt()}月"
}

fun shortMonth(month: String): String = "${month.split('-')[1].toInt()}月"

fun dateLabel(date: String): String {
    val d = LocalDate.parse(date)
    val week = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")[d.dayOfWeek.value % 7]
    return "${d.monthValue}月${d.dayOfMonth}日 $week"
}

fun monthDays(month: String): Int {
    val parts = month.split('-')
    return LocalDate.of(parts[0].toInt(), parts[1].toInt(), 1).lengthOfMonth()
}
fun daysLeftInMonth(month: String): Int {
    val parts = month.split('-')
    val total = LocalDate.of(parts[0].toInt(), parts[1].toInt(), 1).lengthOfMonth()
    return if (month == currentMonth()) {
        val day = LocalDate.now().dayOfMonth
        maxOf(0, total - day + 1)
    } else if (month < currentMonth()) {
        0
    } else {
        total
    }
}