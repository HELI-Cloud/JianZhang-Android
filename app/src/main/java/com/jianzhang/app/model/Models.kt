package com.jianzhang.app.model

import com.jianzhang.app.data.Account
import com.jianzhang.app.data.Tag

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
    val account: String,
    val amountCents: Long,
    val note: String,
)

data class MonthSummary(val incomeCents: Long = 0, val expenseCents: Long = 0)

data class AccountBalance(val account: Account, val balanceCents: Long, val txCount: Int)

/**
 * 账户汇总（纯函数，便于单测）。
 * 口径统一：资产 = 正余额账户之和，负债 = 负余额账户之和，净资产 = 资产 + 负债。
 * 三者均包含已停用账户（停用只是 UI 归档，余额并未从净资产中消失），故恒有 netWorth == assets + debt。
 */
data class AccountSummary(
    val netWorthCents: Long,
    val assetsCents: Long,
    val debtCents: Long,
    val activeCount: Int,
    val archivedCount: Int,
)

fun summarizeAccounts(balances: List<AccountBalance>): AccountSummary {
    var assets = 0L
    var debt = 0L
    for (b in balances) {
        if (b.balanceCents >= 0) assets += b.balanceCents else debt += b.balanceCents
    }
    return AccountSummary(
        netWorthCents = assets + debt,
        assetsCents = assets,
        debtCents = debt,
        activeCount = balances.count { !it.account.archived },
        archivedCount = balances.count { it.account.archived },
    )
}

data class TagUsage(val tag: Tag, val usageCount: Int)

data class LedgerEntry(
    val kind: String,
    val id: Long,
    val date: String,
    val amountCents: Long,
    val note: String,
    val categoryId: Long?,
    val accountId: Long?,
    val toAccountId: Long?,
    val feeCents: Long,
    val createdAt: String,
    val tagIds: List<Long> = emptyList(),
)

enum class ReminderKind {
    BUDGET, BILL, DAILY, WEEKLY
}

enum class ReminderLevel { INFO, WARN, DANGER }

data class ReminderItem(
    val key: String,
    val kind: ReminderKind,
    val title: String,
    val body: String,
    val level: ReminderLevel,
    val route: String,
    val ruleId: Long? = null,
    val confirmable: Boolean = false,
)

/** 应用设置（持久化到 SharedPreferences） */
data class AppSettings(
    val fontSize: String = "medium",
    val fontFamily: String = "default",
    val defaultType: String = "expense",
    val rememberCategory: Boolean = false,
    val themeMode: String = "system",
    val uiStyle: String = "hacker",
    val defaultAccountId: Long = 0,
    val budgetAlert: Boolean = true,
    val billAlert: Boolean = true,
    val dailyReminder: Boolean = false,
    val weeklyReminder: Boolean = true,
    val dailyReminderTime: String = "21:00",
    val weeklyReminderWeekday: Int = 0,
    val systemNotify: Boolean = true,
)
