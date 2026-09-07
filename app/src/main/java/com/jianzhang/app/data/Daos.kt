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

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY type, sort, id")
    suspend fun getAll(): List<Category>

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY sort, id")
    suspend fun byType(type: String): List<Category>

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
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE substr(date, 1, 7) = :month ORDER BY date DESC, id DESC")
    suspend fun listByMonth(month: String): List<Transaction>

    @Query("SELECT * FROM transactions ORDER BY date, id")
    suspend fun listAll(): List<Transaction>

    @Query(
        "SELECT t.id, t.date, t.type, c.name AS category, t.amount_cents AS amountCents, t.note " +
            "FROM transactions t JOIN categories c ON c.id = t.category_id " +
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

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("SELECT type, SUM(amount_cents) AS totalCents FROM transactions WHERE substr(date, 1, 7) = :month GROUP BY type")
    suspend fun monthSummary(month: String): List<SummaryRow>

    @Query(
        "SELECT substr(date, 1, 7) AS ym, type, SUM(amount_cents) AS totalCents " +
            "FROM transactions WHERE date BETWEEN :start AND :end GROUP BY ym, type"
    )
    suspend fun monthlyAggregates(start: String, end: String): List<MonthAgg>

    @Query(
        "SELECT c.name, c.icon, SUM(t.amount_cents) AS totalCents " +
            "FROM transactions t JOIN categories c ON c.id = t.category_id " +
            "WHERE t.type = :type AND substr(t.date, 1, 7) = :month GROUP BY c.id ORDER BY totalCents DESC"
    )
    suspend fun categoryTotals(month: String, type: String): List<CategoryTotal>

    @Query(
        "SELECT substr(date, 9, 2) AS day, SUM(amount_cents) AS totalCents " +
            "FROM transactions WHERE type = 'expense' AND substr(date, 1, 7) = :month " +
            "GROUP BY day ORDER BY day"
    )
    suspend fun dailyTotals(month: String): List<DayTotal>
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
