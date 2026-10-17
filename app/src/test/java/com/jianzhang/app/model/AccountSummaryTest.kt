package com.jianzhang.app.model

import com.jianzhang.app.data.Account
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 回归测试：账户汇总口径必须自洽。
 * 旧实现 netWorth 含停用账户、assets/debt 只含在用账户，导致归档账户后
 * 出现「净资产 ≠ 资产 + 负债」的余额异常。
 */
class AccountSummaryTest {

    private fun account(id: Long, archived: Boolean = false) = Account(
        id = id,
        name = "acc$id",
        kind = "cash",
        icon = "wallet",
        color = "#1fd0ff",
        initialCents = 0L,
        sort = id.toInt(),
        archived = archived,
        createdAt = "2026-01-01T00:00:00Z",
    )

    private fun balance(id: Long, cents: Long, archived: Boolean = false) =
        AccountBalance(account = account(id, archived), balanceCents = cents, txCount = 0)

    @Test
    fun netWorthEqualsAssetsPlusDebtEvenWithArchivedAccounts() {
        val rows = listOf(
            balance(1, 100_00L),
            balance(2, -30_00L),
            balance(3, 50_00L, archived = true),
            balance(4, -20_00L, archived = true),
        )
        val summary = summarizeAccounts(rows)
        assertEquals(150_00L, summary.assetsCents)
        assertEquals(-50_00L, summary.debtCents)
        assertEquals(100_00L, summary.netWorthCents)
        assertEquals(summary.assetsCents + summary.debtCents, summary.netWorthCents)
    }

    @Test
    fun countsSplitActiveAndArchived() {
        val rows = listOf(balance(1, 0L), balance(2, 0L), balance(3, 0L, archived = true))
        val summary = summarizeAccounts(rows)
        assertEquals(2, summary.activeCount)
        assertEquals(1, summary.archivedCount)
        assertEquals(0L, summary.netWorthCents)
    }

    @Test
    fun emptyListIsAllZero() {
        val summary = summarizeAccounts(emptyList())
        assertEquals(0L, summary.netWorthCents)
        assertEquals(0, summary.activeCount)
        assertEquals(0, summary.archivedCount)
    }

    @Test
    fun zeroBalanceAccountDoesNotBreakInvariant() {
        val rows = listOf(balance(1, 0L), balance(2, -1L))
        val summary = summarizeAccounts(rows)
        assertEquals(summary.assetsCents + summary.debtCents, summary.netWorthCents)
        assertEquals(-1L, summary.netWorthCents)
    }
}
