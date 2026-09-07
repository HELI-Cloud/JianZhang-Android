package com.jianzhang.app.data

import com.jianzhang.app.model.CategoryTotal
import com.jianzhang.app.model.CsvRow
import com.jianzhang.app.model.DayTotal
import com.jianzhang.app.model.MonthAgg
import com.jianzhang.app.model.MonthSummary
import com.jianzhang.app.model.TxType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** 统一数据入口：分类、交易、统计、预算、备份、设置 */
class JzRepository(private val db: AppDatabase) {
    private val catDao = db.categoryDao()
    private val txDao = db.transactionDao()
    private val budgetDao = db.budgetDao()

    suspend fun categories(): List<Category> = catDao.getAll()

    suspend fun categoriesByType(type: TxType): List<Category> = catDao.byType(type.db)

    suspend fun listByMonth(month: String): List<Transaction> = txDao.listByMonth(month)

    suspend fun csvRows(): List<CsvRow> = txDao.csvRows()

    suspend fun addTransaction(tx: Transaction): Long = txDao.insert(tx)

    suspend fun updateTransaction(tx: Transaction) = txDao.update(tx)

    suspend fun deleteTransaction(id: Long) = txDao.delete(id)

    suspend fun monthSummary(month: String): MonthSummary {
        val rows = txDao.monthSummary(month)
        var income = 0L
        var expense = 0L
        for (r in rows) {
            if (r.type == "income") income = r.totalCents else expense = r.totalCents
        }
        return MonthSummary(incomeCents = income, expenseCents = expense)
    }

    suspend fun trend(months: List<String>): List<MonthAgg> {
        val start = "${months.first()}-01"
        val end = "${months.last()}-31"
        return txDao.monthlyAggregates(start, end)
    }

    suspend fun categoryTotals(month: String, type: TxType): List<CategoryTotal> =
        txDao.categoryTotals(month, type.db)

    suspend fun dailyTotals(month: String): List<DayTotal> = txDao.dailyTotals(month)

    suspend fun addCategory(type: TxType, name: String, icon: String): Long =
        catDao.insert(Category(type = type.db, name = name, icon = icon, sort = 50))

    /** 删除自定义分类：其下账目自动归入"其他" */
    suspend fun deleteCategory(cat: Category) {
        val otherId = catDao.findId(cat.type, "其他") ?: return
        catDao.reassignTransactions(cat.id, otherId)
        catDao.delete(cat.id)
    }

    suspend fun categoryCount(): Int = catDao.count()

    // ---------- 预算 ----------

    suspend fun budgetsForMonth(month: String): List<Budget> = budgetDao.byMonth(month)

    suspend fun saveTotalBudget(month: String, amountCents: Long) {
        budgetDao.deleteTotal(month)
        if (amountCents > 0) {
            budgetDao.insertIgnore(Budget(month = month, categoryId = null, amountCents = amountCents))
        }
    }

    suspend fun saveCategoryBudget(month: String, categoryId: Long, amountCents: Long) {
        budgetDao.deleteCategoryBudget(month, categoryId)
        if (amountCents > 0) {
            budgetDao.insertIgnore(Budget(month = month, categoryId = categoryId, amountCents = amountCents))
        }
    }

    // ---------- 导出 / 导入（与桌面版 jianzhang-backup 格式互通） ----------

    suspend fun buildBackupJson(): String {
        val root = JSONObject()
        root.put("format", "jianzhang-backup")
        root.put("version", 1)
        root.put("exportedAt", java.time.Instant.now().toString())

        val cats = org.json.JSONArray()
        catDao.getAll().forEach { c ->
            cats.put(
                JSONObject().apply {
                    put("id", c.id)
                    put("type", c.type)
                    put("name", c.name)
                    put("icon", c.icon)
                    put("sort", c.sort)
                    put("builtin", if (c.builtin) 1 else 0)
                }
            )
        }

        val txs = org.json.JSONArray()
        txDao.listAll().forEach { t ->
            txs.put(
                JSONObject().apply {
                    put("id", t.id)
                    put("type", t.type)
                    put("amount_cents", t.amountCents)
                    put("category_id", t.categoryId)
                    put("note", t.note)
                    put("date", t.date)
                    put("created_at", t.createdAt)
                }
            )
        }

        val bds = org.json.JSONArray()
        budgetDao.all().forEach { b ->
            bds.put(
                JSONObject().apply {
                    put("id", b.id)
                    put("month", b.month)
                    put("category_id", b.categoryId ?: JSONObject.NULL)
                    put("amount_cents", b.amountCents)
                }
            )
        }

        root.put("categories", cats)
        root.put("transactions", txs)
        root.put("budgets", bds)
        return root.toString(2)
    }

    suspend fun buildCsv(): String {
        val sb = StringBuilder()
        sb.append('\uFEFF')
        sb.append("日期,类型,分类,金额(元),备注\r\n")
        txDao.csvRows().forEach { r ->
            sb.append(csvCell(r.date)).append(',')
                .append(if (r.type == "expense") "支出" else "收入").append(',')
                .append(csvCell(r.category)).append(',')
                .append(r.amountCents / 100).append('.').append((r.amountCents % 100).toString().padStart(2, '0')).append(',')
                .append(csvCell(r.note)).append("\r\n")
        }
        return sb.toString()
    }

    private fun csvCell(v: String): String =
        if (v.contains(',') || v.contains('"') || v.contains('\n') || v.contains('\r')) {
            "\"" + v.replace("\"", "\"\"") + "\""
        } else {
            v
        }

    /**
     * 导入备份（桌面版或安卓版导出的 jianzhang-backup）。
     * replace = true 先清空再恢复；false 按分类名称去重合并。
     * @return (分类数, 账目数, 预算数)
     */
    suspend fun importBackup(raw: String, replace: Boolean): Triple<Int, Int, Int> {
        val root = JSONObject(raw)
        if (root.optString("format") != "jianzhang-backup") {
            throw IllegalArgumentException("文件格式不正确，请选择由简账导出的备份文件")
        }
        val catArr = root.optJSONArray("categories") ?: org.json.JSONArray()
        val txArr = root.optJSONArray("transactions") ?: org.json.JSONArray()
        val bdArr = root.optJSONArray("budgets") ?: org.json.JSONArray()

        if (replace) {
            txDao.deleteAll()
            budgetDao.deleteAll()
            catDao.deleteAll()
            for (i in 0 until catArr.length()) {
                val j = catArr.getJSONObject(i)
                catDao.insertIgnore(
                    Category(
                        id = j.optLong("id", 0),
                        type = j.optString("type", "expense"),
                        name = j.optString("name"),
                        icon = j.optString("icon", "more-horizontal"),
                        sort = j.optInt("sort", 0),
                        builtin = jsonBuiltin(j),
                    )
                )
            }
            for (i in 0 until txArr.length()) {
                val j = txArr.getJSONObject(i)
                txDao.insertIgnore(
                    Transaction(
                        id = j.optLong("id", 0),
                        type = j.optString("type", "expense"),
                        amountCents = j.optLong("amount_cents", 0),
                        categoryId = j.optLong("category_id", 0),
                        note = j.optString("note"),
                        date = j.optString("date"),
                        createdAt = j.optString("created_at"),
                    )
                )
            }
            for (i in 0 until bdArr.length()) {
                val j = bdArr.getJSONObject(i)
                budgetDao.insertIgnore(
                    Budget(
                        id = j.optLong("id", 0),
                        month = j.optString("month"),
                        categoryId = if (j.isNull("category_id")) null else j.optLong("category_id"),
                        amountCents = j.optLong("amount_cents", 0),
                    )
                )
            }
        } else {
            val catMap = HashMap<Long, Long>()
            for (i in 0 until catArr.length()) {
                val j = catArr.getJSONObject(i)
                val type = j.optString("type", "expense")
                val name = j.optString("name")
                var target = catDao.findId(type, name)
                if (target == null) {
                    catDao.insert(
                        Category(
                            type = type,
                            name = name,
                            icon = j.optString("icon", "more-horizontal"),
                            sort = j.optInt("sort", 0),
                            builtin = jsonBuiltin(j),
                        )
                    )
                    target = catDao.findId(type, name)
                }
                if (target != null) catMap[j.optLong("id")] = target
            }
            for (i in 0 until txArr.length()) {
                val j = txArr.getJSONObject(i)
                txDao.insertIgnore(
                    Transaction(
                        id = j.optLong("id", 0),
                        type = j.optString("type", "expense"),
                        amountCents = j.optLong("amount_cents", 0),
                        categoryId = catMap[j.optLong("category_id")] ?: 0L,
                        note = j.optString("note"),
                        date = j.optString("date"),
                        createdAt = j.optString("created_at"),
                    )
                )
            }
            for (i in 0 until bdArr.length()) {
                val j = bdArr.getJSONObject(i)
                val cid = if (j.isNull("category_id")) null else catMap[j.optLong("category_id")]
                if (j.isNull("category_id") || cid != null) {
                    budgetDao.insertIgnore(
                        Budget(
                            id = j.optLong("id", 0),
                            month = j.optString("month"),
                            categoryId = cid,
                            amountCents = j.optLong("amount_cents", 0),
                        )
                    )
                }
            }
        }
        return Triple(catArr.length(), txArr.length(), bdArr.length())
    }

    private fun jsonBuiltin(j: JSONObject): Boolean {
        if (!j.has("builtin")) return false
        val v = j.opt("builtin")
        return if (v is Boolean) v else j.optInt("builtin", 0) != 0
    }

    /** 重置全部数据：清空账目/分类/预算，恢复内置分类（忘记密码时调用） */
    suspend fun resetAllData() {
        txDao.deleteAll()
        budgetDao.deleteAll()
        catDao.deleteAll()
        withContext(Dispatchers.IO) {
            db.openHelper.writableDatabase.execSQL(SEED_CATEGORIES_SQL)
        }
    }
}
