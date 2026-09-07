package com.jianzhang.app.model

enum class TxType(val db: String, val label: String) {
    EXPENSE("expense", "支出"),
    INCOME("income", "收入");

    companion object {
        fun from(s: String): TxType = if (s == "income") INCOME else EXPENSE
    }
}

enum class AuthStatus { BOOT, SETUP, LOCKED, UNLOCKED }
enum class UiStyle(val key: String) { HACKER("hacker"), GLASS("glass"); companion object { fun from(key: String) = if (key == GLASS.key) GLASS else HACKER } }


data class MonthAgg(val ym: String, val type: String, val totalCents: Long)
data class CategoryTotal(val name: String, val icon: String, val totalCents: Long)
data class DayTotal(val day: String, val totalCents: Long)
data class SummaryRow(val type: String, val totalCents: Long)

/** CSV 导出行（分类名已 JOIN，列名与 Room 查询别名对应） */
data class CsvRow(
    val id: Long,
    val date: String,
    val type: String,
    val category: String,
    val amountCents: Long,
    val note: String,
)

data class MonthSummary(val incomeCents: Long = 0, val expenseCents: Long = 0)

/** 应用设置（持久化到 SharedPreferences） */
data class AppSettings(
    val fontSize: String = "medium",
    val fontFamily: String = "default",
    val defaultType: String = "expense",
    val rememberCategory: Boolean = false,
    val themeMode: String = "system",
    val uiStyle: String = "hacker",
)
