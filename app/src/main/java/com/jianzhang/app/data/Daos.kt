package com.jianzhang.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jianzhang.app.model.CategoryTotal
import com.jianzhang.app.model.CsvRow
import com.jianzhang.app.model.DayTotal
import com.jianzhang.app.model.MonthAgg
import com.jianzhang.app.model.SummaryRow

data class AccountBalanceRow(
    val id: Long,
    val name: String,
    val kind: String,
    val icon: String,
    val color: String,
    val initialCents: Long,
    val sort: Int,
    val archived: Boolean,
    val createdAt: String,
    val balanceCents: Long,
    val txCount: Int,
)

data class TagUsageRow(
    val id: Long,
    val name: String,
    val color: String,
    val sort: Int,
    val usageCount: Int,
)

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY type, sort, id")
    suspend fun getAll(): List<Category>

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY sort, id")
    suspend fun byType(type: String): List<Category>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): Category?

    @Query("SELECT id FROM categories WHERE type = :type AND name = :name LIMIT 1")
    suspend fun findId(type: String, name: String): Long?

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Insert
    suspend fun insert(category: Category): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(category: Category): Long

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()

    @Query("UPDATE transactions SET category_id = :newId WHERE category_id = :oldId")
    suspend fun reassignTransactions(oldId: Long, newId: Long)

    @Query("UPDATE recurring_rules SET category_id = :newId WHERE category_id = :oldId")
    suspend fun reassignRecurring(oldId: Long, newId: Long?)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE substr(date, 1, 7) = :month ORDER BY date DESC, id DESC")
    suspend fun listByMonth(month: String): List<Transaction>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :start AND :end ORDER BY date, id")
    suspend fun listByRange(start: String, end: String): List<Transaction>

    @Query("SELECT * FROM transactions ORDER BY date, id")
    suspend fun listAll(): List<Transaction>

    @Query(
        "SELECT t.id, t.date, t.type, c.name AS category, COALESCE(a.name, '') AS account, " +
            "t.amount_cents AS amountCents, t.note " +
            "FROM transactions t JOIN categories c ON c.id = t.category_id " +
            "LEFT JOIN accounts a ON a.id = t.account_id " +
            "ORDER BY t.date DESC, t.id DESC"
    )
    suspend fun csvRows(): List<CsvRow>

    @Insert
    suspend fun insert(transaction: Transaction): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(transaction: Transaction): Long

    @Update
    suspend fun update(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM transactions WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("SELECT type, SUM(amount_cents) AS totalCents FROM transactions WHERE substr(date, 1, 7) = :month GROUP BY type")
    suspend fun monthSummary(month: String): List<SummaryRow>

    @Query("SELECT type, SUM(amount_cents) AS totalCents FROM transactions WHERE date BETWEEN :start AND :end GROUP BY type")
    suspend fun rangeSummary(start: String, end: String): List<SummaryRow>

    @Query(
        "SELECT t.type, SUM(t.amount_cents) AS totalCents FROM transactions t " +
            "JOIN transaction_tags tt ON tt.transaction_id = t.id AND tt.tag_id = :tagId " +
            "WHERE t.date BETWEEN :start AND :end GROUP BY t.type"
    )
    suspend fun rangeSummaryByTag(start: String, end: String, tagId: Long): List<SummaryRow>

    @Query(
        "SELECT substr(date, 1, 7) AS ym, type, SUM(amount_cents) AS totalCents " +
            "FROM transactions WHERE date BETWEEN :start AND :end GROUP BY ym, type ORDER BY ym"
    )
    suspend fun monthlyAggregates(start: String, end: String): List<MonthAgg>

    @Query(
        "SELECT substr(t.date, 1, 7) AS ym, t.type, SUM(t.amount_cents) AS totalCents " +
            "FROM transactions t JOIN transaction_tags tt ON tt.transaction_id = t.id AND tt.tag_id = :tagId " +
            "WHERE t.date BETWEEN :start AND :end GROUP BY ym, t.type ORDER BY ym"
    )
    suspend fun monthlyAggregatesByTag(start: String, end: String, tagId: Long): List<MonthAgg>

    @Query(
        "SELECT c.name, c.icon, SUM(t.amount_cents) AS totalCents " +
            "FROM transactions t JOIN categories c ON c.id = t.category_id " +
            "WHERE t.type = :type AND substr(t.date, 1, 7) = :month GROUP BY c.id ORDER BY totalCents DESC"
    )
    suspend fun categoryTotals(month: String, type: String): List<CategoryTotal>

    @Query(
        "SELECT c.name, c.icon, SUM(t.amount_cents) AS totalCents " +
            "FROM transactions t JOIN categories c ON c.id = t.category_id " +
            "WHERE t.type = :type AND t.date BETWEEN :start AND :end GROUP BY c.id ORDER BY totalCents DESC"
    )
    suspend fun categoryTotalsRange(start: String, end: String, type: String): List<CategoryTotal>

    @Query(
        "SELECT c.name, c.icon, SUM(t.amount_cents) AS totalCents " +
            "FROM transactions t JOIN categories c ON c.id = t.category_id " +
            "JOIN transaction_tags tt ON tt.transaction_id = t.id AND tt.tag_id = :tagId " +
            "WHERE t.type = :type AND t.date BETWEEN :start AND :end GROUP BY c.id ORDER BY totalCents DESC"
    )
    suspend fun categoryTotalsRangeByTag(start: String, end: String, type: String, tagId: Long): List<CategoryTotal>

    @Query(
        "SELECT substr(date, 9, 2) AS day, SUM(amount_cents) AS totalCents " +
            "FROM transactions WHERE type = 'expense' AND substr(date, 1, 7) = :month " +
            "GROUP BY day ORDER BY day"
    )
    suspend fun dailyTotals(month: String): List<DayTotal>

    @Query("SELECT COUNT(*) FROM transactions WHERE date BETWEEN :start AND :end")
    suspend fun countInRange(start: String, end: String): Int

    @Query("SELECT DISTINCT type FROM transactions WHERE id IN (:ids)")
    suspend fun typesForIds(ids: List<Long>): List<String>

    @Query("UPDATE transactions SET category_id = :categoryId WHERE id IN (:ids)")
    suspend fun bulkSetCategory(ids: List<Long>, categoryId: Long)

    @Query("UPDATE transactions SET account_id = :accountId WHERE id IN (:ids)")
    suspend fun bulkSetAccount(ids: List<Long>, accountId: Long?)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE month = :month ORDER BY category_id")
    suspend fun byMonth(month: String): List<Budget>

    @Query("SELECT * FROM budgets ORDER BY month, category_id")
    suspend fun all(): List<Budget>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(budget: Budget): Long

    @Query("DELETE FROM budgets WHERE month = :month AND category_id IS NULL")
    suspend fun deleteTotal(month: String)

    @Query("DELETE FROM budgets WHERE month = :month AND category_id = :categoryId")
    suspend fun deleteCategoryBudget(month: String, categoryId: Long)

    @Query("DELETE FROM budgets")
    suspend fun deleteAll()
}

@Dao
interface AccountDao {
    @Query(
        """
        SELECT a.id, a.name, a.kind, a.icon, a.color, a.initial_cents AS initialCents,
            a.sort, a.archived, a.created_at AS createdAt,
            a.initial_cents
            + COALESCE((SELECT SUM(CASE WHEN t.type = 'income' THEN t.amount_cents ELSE -t.amount_cents END)
                FROM transactions t WHERE t.account_id = a.id), 0)
            + COALESCE((SELECT SUM(tr.amount_cents) FROM transfers tr WHERE tr.to_account_id = a.id), 0)
            - COALESCE((SELECT SUM(tr.amount_cents + tr.fee_cents) FROM transfers tr WHERE tr.from_account_id = a.id), 0)
            AS balanceCents,
            COALESCE((SELECT COUNT(*) FROM transactions t WHERE t.account_id = a.id), 0) AS txCount
        FROM accounts a
        WHERE (:includeArchived = 1 OR a.archived = 0)
        ORDER BY a.sort, a.id
        """
    )
    suspend fun balances(includeArchived: Boolean): List<AccountBalanceRow>

    @Query("SELECT * FROM accounts ORDER BY sort, id")
    suspend fun all(): List<Account>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): Account?

    @Query("SELECT id FROM accounts ORDER BY sort, id LIMIT 1")
    suspend fun firstId(): Long?

    /** 首个未停用账户；仅当不存在任何在用账户时才退化为任意账户 */
    @Query("SELECT id FROM accounts WHERE archived = 0 ORDER BY sort, id LIMIT 1")
    suspend fun firstActiveId(): Long?

    /**
     * 删除账户时清理会塌缩成"自转账"的记录：
     * 原 from=被删账户 且 to=接替账户（或反向），重映射后会变成 from==to。
     * 这类记录若不删除，余额只会被扣掉手续费，直接造成余额异常。
     */
    @Query(
        "DELETE FROM transfers WHERE (from_account_id = :removedId AND to_account_id = :fallbackId) " +
            "OR (to_account_id = :removedId AND from_account_id = :fallbackId)"
    )
    suspend fun deleteCollapsingTransfers(removedId: Long, fallbackId: Long)

    @Query("SELECT MAX(sort) FROM accounts")
    suspend fun maxSort(): Int?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(account: Account): Long

    @Update
    suspend fun update(account: Account)

    @Query("UPDATE accounts SET archived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)

    @Query("UPDATE transactions SET account_id = :toId WHERE account_id = :fromId")
    suspend fun reassignTransactions(fromId: Long, toId: Long)

    @Query("UPDATE transfers SET from_account_id = :toId WHERE from_account_id = :fromId")
    suspend fun reassignTransfersFrom(fromId: Long, toId: Long)

    @Query("UPDATE transfers SET to_account_id = :toId WHERE to_account_id = :fromId")
    suspend fun reassignTransfersTo(fromId: Long, toId: Long)

    @Query("UPDATE recurring_rules SET account_id = :toId WHERE account_id = :fromId")
    suspend fun reassignRecurring(fromId: Long, toId: Long)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM accounts")
    suspend fun deleteAll()
}

@Dao
interface TransferDao {
    @Query("SELECT * FROM transfers WHERE substr(date, 1, 7) = :month ORDER BY date DESC, id DESC")
    suspend fun listByMonth(month: String): List<Transfer>

    @Query("SELECT * FROM transfers WHERE date BETWEEN :start AND :end ORDER BY date, id")
    suspend fun listByRange(start: String, end: String): List<Transfer>

    @Query("SELECT * FROM transfers ORDER BY date, id")
    suspend fun all(): List<Transfer>

    @Query("SELECT * FROM transfers WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): Transfer?

    @Insert
    suspend fun insert(transfer: Transfer): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(transfer: Transfer): Long

    @Update
    suspend fun update(transfer: Transfer)

    @Query("DELETE FROM transfers WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM transfers WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM transfers")
    suspend fun deleteAll()
}

@Dao
interface TagDao {
    @Query(
        "SELECT g.id, g.name, g.color, g.sort, " +
            "(SELECT COUNT(*) FROM transaction_tags tt WHERE tt.tag_id = g.id) AS usageCount " +
            "FROM tags g ORDER BY g.sort, g.id"
    )
    suspend fun allWithUsage(): List<TagUsageRow>

    @Query("SELECT * FROM tags ORDER BY sort, id")
    suspend fun all(): List<Tag>

    @Query("SELECT * FROM tags WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): Tag?

    @Query("SELECT id FROM tags WHERE name = :name LIMIT 1")
    suspend fun findId(name: String): Long?

    @Query("SELECT MAX(sort) FROM tags")
    suspend fun maxSort(): Int?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(tag: Tag): Long

    @Update
    suspend fun update(tag: Tag)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM tags")
    suspend fun deleteAll()

    @Query("SELECT tag_id FROM transaction_tags WHERE transaction_id = :transactionId")
    suspend fun tagIdsForTransaction(transactionId: Long): List<Long>

    @Query("SELECT transaction_id, tag_id FROM transaction_tags")
    suspend fun allLinks(): List<TransactionTag>

    @Query("SELECT transaction_id, tag_id FROM transaction_tags WHERE transaction_id IN (:transactionIds)")
    suspend fun linksForTransactions(transactionIds: List<Long>): List<TransactionTag>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLink(link: TransactionTag)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLinks(links: List<TransactionTag>)

    @Query("DELETE FROM transaction_tags WHERE transaction_id = :transactionId")
    suspend fun clearLinks(transactionId: Long)

    @Query("DELETE FROM transaction_tags WHERE tag_id = :tagId")
    suspend fun clearLinksForTag(tagId: Long)

    @Query("DELETE FROM transaction_tags")
    suspend fun clearAllLinks()
}

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring_rules ORDER BY enabled DESC, next_date, id")
    suspend fun all(): List<RecurringRule>

    @Query("SELECT * FROM recurring_rules WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): RecurringRule?

    @Insert
    suspend fun insert(rule: RecurringRule): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(rule: RecurringRule): Long

    @Update
    suspend fun update(rule: RecurringRule)

    @Query("UPDATE recurring_rules SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE recurring_rules SET next_date = :nextDate WHERE id = :id")
    suspend fun setNextDate(id: Long, nextDate: String)

    @Query("DELETE FROM recurring_rules WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM recurring_rules")
    suspend fun deleteAll()
}

@Dao
interface ReminderDao {
    @Query("SELECT key FROM reminder_dismissals")
    suspend fun dismissedKeys(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun dismiss(item: ReminderDismissal)

    @Query("DELETE FROM reminder_dismissals")
    suspend fun clear()
}
