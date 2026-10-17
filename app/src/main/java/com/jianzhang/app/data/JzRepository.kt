package com.jianzhang.app.data

import androidx.room.withTransaction
import com.jianzhang.app.model.AccountBalance
import com.jianzhang.app.model.CategoryTotal
import com.jianzhang.app.model.CsvRow
import com.jianzhang.app.model.DayTotal
import com.jianzhang.app.model.LedgerEntry
import com.jianzhang.app.model.MonthAgg
import com.jianzhang.app.model.MonthSummary
import com.jianzhang.app.model.TagUsage
import com.jianzhang.app.model.TxType
import com.jianzhang.app.util.isValidDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

data class ImportResult(
    val categories: Int,
    val transactions: Int,
    val budgets: Int,
    val accounts: Int,
    val transfers: Int,
    val tags: Int,
    val recurringRules: Int,
)

/** 统一数据入口：分类、账户、转账、标签、周期、统计、预算、提醒与备份。 */
class JzRepository(private val db: AppDatabase) {
    private val recurringMutex = Mutex()
    private val catDao = db.categoryDao()
    private val txDao = db.transactionDao()
    private val budgetDao = db.budgetDao()
    private val accountDao = db.accountDao()
    private val transferDao = db.transferDao()
    private val tagDao = db.tagDao()
    private val recurringDao = db.recurringDao()
    private val reminderDao = db.reminderDao()

    // ---------- 分类 ----------

    suspend fun categories(): List<Category> = catDao.getAll()

    suspend fun categoriesByType(type: TxType): List<Category> = catDao.byType(type.db)

    suspend fun categoryCount(): Int = catDao.count()

    suspend fun addCategory(type: TxType, name: String, icon: String): Long =
        catDao.insert(Category(type = type.db, name = name, icon = icon, sort = 50))

    /** 删除自定义分类：账目与周期规则都会安全归入同类型的“其他”。 */
    suspend fun deleteCategory(cat: Category) {
        val otherId = catDao.findId(cat.type, "其他")
            ?: throw IllegalStateException("系统缺少“其他”分类，无法安全删除")
        db.withTransaction {
            catDao.reassignTransactions(cat.id, otherId)
            catDao.reassignRecurring(cat.id, otherId)
            catDao.delete(cat.id)
        }
    }

    // ---------- 账目与合并流水 ----------

    suspend fun listByMonth(month: String): List<Transaction> = txDao.listByMonth(month)

    suspend fun listByRange(start: String, end: String): List<Transaction> = txDao.listByRange(start, end)

    suspend fun tagIdsForTransaction(id: Long): List<Long> = tagDao.tagIdsForTransaction(id)

    suspend fun addTransaction(tx: Transaction, tagIds: List<Long> = emptyList()): Long {
        validateTransaction(tx)
        val accountId = resolveAccountForInsert(tx.accountId)
        return db.withTransaction {
            val id = txDao.insert(tx.copy(accountId = accountId))
            setTagsInternal(id, tagIds)
            id
        }
    }

    suspend fun updateTransaction(tx: Transaction, tagIds: List<Long>? = null) {
        validateTransaction(tx)
        db.withTransaction {
            txDao.update(tx.copy(accountId = resolveAccountForUpdate(tx.accountId)))
            if (tagIds != null) setTagsInternal(tx.id, tagIds)
        }
    }

    suspend fun deleteTransaction(id: Long) {
        db.withTransaction {
            tagDao.clearLinks(id)
            txDao.delete(id)
        }
    }

    suspend fun listLedger(month: String): List<LedgerEntry> {
        val rows = txDao.listByMonth(month)
        val links = linksFor(rows)
        val transactions = rows.map { it.toLedger(links[it.id].orEmpty()) }
        val transfers = transferDao.listByMonth(month).map { it.toLedger() }
        return (transactions + transfers).sortedWith(
            compareByDescending<LedgerEntry> { it.date }.thenByDescending { it.id },
        )
    }

    suspend fun listLedgerRange(start: String, end: String): List<LedgerEntry> {
        val rows = txDao.listByRange(start, end)
        val links = linksFor(rows)
        val transactions = rows.map { it.toLedger(links[it.id].orEmpty()) }
        val transfers = transferDao.listByRange(start, end).map { it.toLedger() }
        return (transactions + transfers).sortedWith(
            compareByDescending<LedgerEntry> { it.date }.thenByDescending { it.id },
        )
    }

    suspend fun csvRows(): List<CsvRow> = txDao.csvRows()

    private suspend fun validateTransaction(tx: Transaction) {
        if (tx.type != "expense" && tx.type != "income") throw IllegalArgumentException("账目类型不正确")
        if (tx.amountCents <= 0) throw IllegalArgumentException("请输入有效金额")
        val category = catDao.byId(tx.categoryId) ?: throw IllegalArgumentException("分类不存在")
        if (category.type != tx.type) throw IllegalArgumentException("分类与收支类型不匹配")
        if (!isValidDate(tx.date)) throw IllegalArgumentException("请输入有效日期")
    }

    /** 新增：未选账户时挂到首个在用账户（绝不落入已停用账户，否则余额会“消失”） */
    private suspend fun resolveAccountForInsert(accountId: Long?): Long? {
        val id = accountId ?: accountDao.firstActiveId() ?: accountDao.firstId()
        if (id != null && accountDao.byId(id) == null) throw IllegalArgumentException("账户不存在")
        return id
    }

    /**
     * 更新：账户归属原样保留（含显式 null）。
     * 旧实现会把 null 静默换成 firstId()，导致编辑一笔旧账目就凭空搬动两个账户的余额。
     */
    private suspend fun resolveAccountForUpdate(accountId: Long?): Long? {
        if (accountId == null) return null
        if (accountDao.byId(accountId) == null) throw IllegalArgumentException("账户不存在")
        return accountId
    }

    private suspend fun linksFor(transactions: List<Transaction>): Map<Long, List<Long>> {
        if (transactions.isEmpty()) return emptyMap()
        return tagDao.linksForTransactions(transactions.map { it.id })
            .groupBy({ it.transactionId }, { it.tagId })
    }

    suspend fun buildCsv(): String {
        val sb = StringBuilder()
        sb.append('\uFEFF')
        sb.append("日期,类型,分类,账户,金额(元),备注\r\n")
        txDao.csvRows().forEach { r ->
            sb.append(csvCell(r.date)).append(',')
                .append(if (r.type == "expense") "支出" else "收入").append(',')
                .append(csvCell(r.category)).append(',')
                .append(csvCell(r.account)).append(',')
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

    // ---------- 账户与转账 ----------

    suspend fun accounts(includeArchived: Boolean = false): List<AccountBalance> =
        accountDao.balances(includeArchived).map {
            AccountBalance(
                account = Account(
                    id = it.id,
                    name = it.name,
                    kind = it.kind,
                    icon = it.icon,
                    color = it.color,
                    initialCents = it.initialCents,
                    sort = it.sort,
                    archived = it.archived,
                    createdAt = it.createdAt,
                ),
                balanceCents = it.balanceCents,
                txCount = it.txCount,
            )
        }

    suspend fun addAccount(account: Account): Long {
        val clean = validateAccount(account)
        if (accountDao.all().any { it.name.equals(clean.name, ignoreCase = true) }) {
            throw IllegalArgumentException("账户名称已存在")
        }
        val sort = (accountDao.maxSort() ?: -1) + 1
        val id = accountDao.insertIgnore(clean.copy(sort = sort, createdAt = clean.createdAt.ifEmpty { Instant.now().toString() }))
        if (id <= 0) throw IllegalArgumentException("账户名称已存在")
        return id
    }

    suspend fun updateAccount(account: Account) {
        if (account.id <= 0 || accountDao.byId(account.id) == null) throw IllegalArgumentException("账户不存在")
        val clean = validateAccount(account)
        if (accountDao.all().any { it.id != clean.id && it.name.equals(clean.name, ignoreCase = true) }) {
            throw IllegalArgumentException("账户名称已存在")
        }
        accountDao.update(clean)
    }

    private fun validateAccount(account: Account): Account {
        val cleanName = account.name.trim()
        if (cleanName.isEmpty()) throw IllegalArgumentException("请输入账户名称")
        return account.copy(name = cleanName)
    }

    suspend fun setAccountArchived(id: Long, archived: Boolean) {
        accountDao.setArchived(id, archived)
    }

    /**
     * 删除账户并把历史账目/转账/周期规则接续到另一个账户。
     * 修复三个会造成“余额异常”的缺陷：
     * 1. 接替账户优先选未停用账户，避免余额被搬进隐藏账户；
     * 2. 接替账户若已停用则强制启用（否则用户看不到接过来的余额）；
     * 3. 重映射后会塌缩成自转账（from == to）的记录先删除，否则余额只会被扣掉手续费。
     */
    suspend fun deleteAccount(id: Long) {
        val all = accountDao.all()
        if (all.none { it.id == id }) return
        val fallback = all.firstOrNull { it.id != id && !it.archived }
            ?: all.firstOrNull { it.id != id }
            ?: throw IllegalStateException("至少需要保留一个账户")
        db.withTransaction {
            if (fallback.archived) accountDao.setArchived(fallback.id, false)
            accountDao.deleteCollapsingTransfers(id, fallback.id)
            accountDao.reassignTransactions(id, fallback.id)
            accountDao.reassignTransfersFrom(id, fallback.id)
            accountDao.reassignTransfersTo(id, fallback.id)
            accountDao.reassignRecurring(id, fallback.id)
            accountDao.delete(id)
        }
    }

    suspend fun netWorthCents(): Long = accountDao.balances(true).sumOf { it.balanceCents }

    suspend fun transfers(month: String): List<Transfer> = transferDao.listByMonth(month)

    suspend fun transfer(id: Long): Transfer? = transferDao.byId(id)

    suspend fun addTransfer(transfer: Transfer): Long {
        validateTransfer(transfer)
        return transferDao.insert(transfer.copy(createdAt = transfer.createdAt.ifEmpty { Instant.now().toString() }))
    }

    suspend fun updateTransfer(transfer: Transfer) {
        validateTransfer(transfer)
        transferDao.update(transfer)
    }

    suspend fun deleteTransfer(id: Long) = transferDao.delete(id)

    private suspend fun validateTransfer(transfer: Transfer) {
        if (transfer.fromAccountId <= 0 || transfer.toAccountId <= 0) throw IllegalArgumentException("请选择转出与转入账户")
        if (accountDao.byId(transfer.fromAccountId) == null || accountDao.byId(transfer.toAccountId) == null) {
            throw IllegalArgumentException("账户不存在")
        }
        if (transfer.fromAccountId == transfer.toAccountId) throw IllegalArgumentException("转出与转入账户不能相同")
        if (transfer.amountCents <= 0) throw IllegalArgumentException("请输入有效的转账金额")
        if (transfer.feeCents < 0) throw IllegalArgumentException("手续费不能为负数")
        if (!isValidDate(transfer.date)) throw IllegalArgumentException("请输入有效转账日期")
    }

    // ---------- 标签 ----------

    suspend fun tags(): List<TagUsage> =
        tagDao.allWithUsage().map {
            TagUsage(
                tag = Tag(id = it.id, name = it.name, color = it.color, sort = it.sort),
                usageCount = it.usageCount,
            )
        }

    suspend fun addTag(name: String, color: String): Long {
        val clean = name.trim()
        if (clean.isEmpty()) throw IllegalArgumentException("请输入标签名称")
        val sort = (tagDao.maxSort() ?: -1) + 1
        val id = tagDao.insertIgnore(Tag(name = clean, color = color, sort = sort))
        if (id <= 0) throw IllegalArgumentException("标签名称已存在")
        return id
    }

    suspend fun updateTag(tag: Tag) = tagDao.update(tag.copy(name = tag.name.trim()))

    suspend fun deleteTag(id: Long) {
        db.withTransaction {
            tagDao.clearLinksForTag(id)
            tagDao.delete(id)
        }
    }

    suspend fun setTransactionTags(transactionId: Long, tagIds: List<Long>) {
        db.withTransaction { setTagsInternal(transactionId, tagIds) }
    }

    private suspend fun setTagsInternal(transactionId: Long, tagIds: List<Long>) {
        tagDao.clearLinks(transactionId)
        tagDao.insertLinks(tagIds.distinct().map { TransactionTag(transactionId, it) })
    }

    // ---------- 搜索与批量操作 ----------

    suspend fun bulkDeleteTransactions(ids: List<Long>) {
        if (ids.isEmpty()) return
        db.withTransaction {
            ids.forEach { tagDao.clearLinks(it) }
            txDao.deleteByIds(ids)
        }
    }

    suspend fun bulkSetCategory(ids: List<Long>, categoryId: Long) {
        if (ids.isEmpty()) return
        val category = catDao.byId(categoryId) ?: throw IllegalArgumentException("分类不存在")
        val types = txDao.typesForIds(ids)
        if (types.size != 1 || types.first() != category.type) {
            throw IllegalArgumentException("所选账目包含多种收支类型，无法批量改为同一分类")
        }
        txDao.bulkSetCategory(ids, categoryId)
    }

    suspend fun bulkSetAccount(ids: List<Long>, accountId: Long?) {
        if (ids.isEmpty()) return
        if (accountId != null) {
            val account = accountDao.byId(accountId) ?: throw IllegalArgumentException("账户不存在")
            if (account.archived) throw IllegalArgumentException("「${account.name}」已停用，请先启用再批量移入")
        }
        txDao.bulkSetAccount(ids, accountId)
    }

    suspend fun bulkAddTag(ids: List<Long>, tagId: Long) {
        if (ids.isEmpty()) return
        if (tagDao.byId(tagId) == null) throw IllegalArgumentException("标签不存在")
        tagDao.insertLinks(ids.distinct().map { TransactionTag(it, tagId) })
    }

    // ---------- 区间统计 ----------

    suspend fun monthSummary(month: String): MonthSummary =
        summarize(txDao.monthSummary(month))

    suspend fun rangeSummary(start: String, end: String, tagId: Long? = null): MonthSummary {
        val rows = if (tagId == null) txDao.rangeSummary(start, end) else txDao.rangeSummaryByTag(start, end, tagId)
        return summarize(rows)
    }

    suspend fun trend(months: List<String>): List<MonthAgg> {
        require(months.isNotEmpty()) { "统计月份不能为空" }
        return txDao.monthlyAggregates("${months.first()}-01", "${months.last()}-31")
    }

    suspend fun monthlyAggregatesRange(start: String, end: String, tagId: Long? = null): List<MonthAgg> =
        if (tagId == null) txDao.monthlyAggregates(start, end) else txDao.monthlyAggregatesByTag(start, end, tagId)

    suspend fun categoryTotals(month: String, type: TxType): List<CategoryTotal> =
        txDao.categoryTotals(month, type.db)

    suspend fun categoryTotalsRange(start: String, end: String, type: TxType, tagId: Long? = null): List<CategoryTotal> =
        if (tagId == null) txDao.categoryTotalsRange(start, end, type.db)
        else txDao.categoryTotalsRangeByTag(start, end, type.db, tagId)

    suspend fun dailyTotals(month: String): List<DayTotal> = txDao.dailyTotals(month)

    suspend fun countTransactionsInRange(start: String, end: String): Int = txDao.countInRange(start, end)

    private fun summarize(rows: List<com.jianzhang.app.model.SummaryRow>): MonthSummary {
        var income = 0L
        var expense = 0L
        for (r in rows) {
            if (r.type == "income") income = r.totalCents else expense = r.totalCents
        }
        return MonthSummary(incomeCents = income, expenseCents = expense)
    }

    // ---------- 周期记账 ----------

    suspend fun recurringRules(): List<RecurringRule> = recurringDao.all()

    suspend fun addRecurringRule(rule: RecurringRule): Long {
        validateRecurring(rule)
        return recurringDao.insert(rule.copy(createdAt = rule.createdAt.ifEmpty { Instant.now().toString() }))
    }

    suspend fun updateRecurringRule(rule: RecurringRule) {
        if (rule.id <= 0) throw IllegalArgumentException("周期规则不存在")
        val existing = recurringDao.byId(rule.id) ?: throw IllegalArgumentException("周期规则不存在")
        validateRecurring(rule)
        val scheduleChanged = existing.frequency != rule.frequency ||
            existing.intervalCount != rule.intervalCount ||
            existing.startDate != rule.startDate
        val nextDate = if (scheduleChanged) {
            RecurringSchedule.firstOccurrenceOnOrAfter(
                rule.startDate,
                java.time.LocalDate.now().toString(),
                rule.frequency,
                rule.intervalCount,
            )
        } else {
            existing.nextDate
        }
        recurringDao.update(rule.copy(nextDate = nextDate))
    }

    suspend fun setRecurringEnabled(id: Long, enabled: Boolean) = recurringDao.setEnabled(id, enabled)

    suspend fun deleteRecurringRule(id: Long) = recurringDao.delete(id)

    suspend fun runRecurring(autoOnly: Boolean = true): Int = recurringMutex.withLock {
        val today = java.time.LocalDate.now().toString()
        var created = 0
        for (snapshot in recurringDao.all()) {
            if (!snapshot.enabled) continue
            if (autoOnly && !snapshot.autoCreate) continue
            var guard = 0
            while (guard < 400) {
                val didCreate = db.withTransaction {
                    val current = recurringDao.byId(snapshot.id) ?: return@withTransaction false
                    if (!current.enabled || (autoOnly && !current.autoCreate) || current.nextDate > today) {
                        return@withTransaction false
                    }
                    if (current.endDate != null && current.nextDate > current.endDate) {
                        recurringDao.setEnabled(current.id, false)
                        return@withTransaction false
                    }
                    validateRecurring(current)
                    insertRecurringTransaction(current, current.nextDate)
                    val next = RecurringSchedule.nextOccurrence(
                        current.startDate,
                        current.nextDate,
                        current.frequency,
                        current.intervalCount,
                    )
                    recurringDao.setNextDate(current.id, next)
                    if (current.endDate != null && next > current.endDate) {
                        recurringDao.setEnabled(current.id, false)
                    }
                    true
                }
                if (!didCreate) break
                created++
                guard++
            }
        }
        created
    }

    suspend fun confirmRecurring(rule: RecurringRule) = recurringMutex.withLock {
        db.withTransaction {
            val current = recurringDao.byId(rule.id) ?: throw IllegalArgumentException("周期规则不存在")
            validateRecurring(current)
            if (!current.enabled) throw IllegalStateException("该周期账目已停用")
            if (current.endDate != null && current.nextDate > current.endDate) {
                recurringDao.setEnabled(current.id, false)
                throw IllegalStateException("该周期账目已超过结束日期，已自动停用")
            }
            insertRecurringTransaction(current, current.nextDate)
            val next = RecurringSchedule.nextOccurrence(
                current.startDate,
                current.nextDate,
                current.frequency,
                current.intervalCount,
            )
            recurringDao.setNextDate(current.id, next)
            if (current.endDate != null && next > current.endDate) {
                recurringDao.setEnabled(current.id, false)
            }
        }
    }

    private suspend fun insertRecurringTransaction(rule: RecurringRule, date: String) {
        val categoryId = rule.categoryId ?: fallbackCategoryId(rule.type)
        if (categoryId <= 0) throw IllegalStateException("周期规则缺少有效分类")
        val accountId = rule.accountId ?: accountDao.firstActiveId() ?: accountDao.firstId()
        if (accountId == null) throw IllegalStateException("周期规则缺少有效账户")
        txDao.insert(
            Transaction(
                type = rule.type,
                amountCents = rule.amountCents,
                categoryId = categoryId,
                accountId = accountId,
                note = rule.note,
                date = date,
                recurringId = rule.id,
            )
        )
    }

    private suspend fun fallbackCategoryId(type: String): Long =
        catDao.findId(type, "其他") ?: catDao.byType(type).firstOrNull()?.id ?: 0L

    private suspend fun validateRecurring(rule: RecurringRule) {
        if (rule.type != "expense" && rule.type != "income") throw IllegalArgumentException("周期类型不正确")
        if (rule.amountCents <= 0) throw IllegalArgumentException("请输入有效金额")
        if (rule.frequency !in setOf("daily", "weekly", "monthly", "yearly")) {
            throw IllegalArgumentException("周期频率不正确")
        }
        if (rule.intervalCount !in 1..60) throw IllegalArgumentException("周期间隔需在 1 到 60 之间")
        val category = rule.categoryId?.let { catDao.byId(it) } ?: throw IllegalArgumentException("请选择分类")
        if (category.type != rule.type) throw IllegalArgumentException("分类与收支类型不匹配")
        if (rule.accountId != null && accountDao.byId(rule.accountId) == null) {
            throw IllegalArgumentException("账户不存在")
        }
        if (!isValidDate(rule.startDate)) throw IllegalArgumentException("请输入有效开始日期")
        if (rule.endDate != null && !isValidDate(rule.endDate)) throw IllegalArgumentException("请输入有效结束日期")
        if (rule.endDate != null && rule.endDate < rule.startDate) {
            throw IllegalArgumentException("结束日期不能早于开始日期")
        }
        if (!isValidDate(rule.nextDate)) throw IllegalArgumentException("下一次入账日期无效")
        if (rule.nextDate < rule.startDate) throw IllegalArgumentException("下一次入账日期不能早于开始日期")
    }

    // ---------- 提醒记录 ----------

    suspend fun dismissedReminderKeys(): Set<String> = reminderDao.dismissedKeys().toSet()

    suspend fun dismissReminder(key: String) =
        reminderDao.dismiss(ReminderDismissal(key, Instant.now().toString()))

    suspend fun clearDismissedReminders() = reminderDao.clear()

    // ---------- 备份 v2（兼容桌面版与旧 v1） ----------

    suspend fun buildBackupJson(): String {
        val root = JSONObject()
        root.put("format", "jianzhang-backup")
        root.put("version", 2)
        root.put("exportedAt", Instant.now().toString())

        root.put("categories", JSONArray().apply {
            catDao.getAll().forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    put("type", c.type)
                    put("name", c.name)
                    put("icon", c.icon)
                    put("sort", c.sort)
                    put("builtin", c.builtin)
                })
            }
        })
        root.put("transactions", JSONArray().apply {
            txDao.listAll().forEach { t ->
                put(JSONObject().apply {
                    put("id", t.id)
                    put("type", t.type)
                    put("amount_cents", t.amountCents)
                    put("category_id", t.categoryId)
                    put("account_id", t.accountId ?: JSONObject.NULL)
                    put("note", t.note)
                    put("date", t.date)
                    put("created_at", t.createdAt)
                    put("recurring_id", t.recurringId ?: JSONObject.NULL)
                })
            }
        })
        root.put("budgets", JSONArray().apply {
            budgetDao.all().forEach { b ->
                put(JSONObject().apply {
                    put("id", b.id)
                    put("month", b.month)
                    put("category_id", b.categoryId ?: JSONObject.NULL)
                    put("amount_cents", b.amountCents)
                })
            }
        })
        root.put("accounts", JSONArray().apply {
            accountDao.all().forEach { a ->
                put(JSONObject().apply {
                    put("id", a.id)
                    put("name", a.name)
                    put("kind", a.kind)
                    put("icon", a.icon)
                    put("color", a.color)
                    put("initial_cents", a.initialCents)
                    put("sort", a.sort)
                    put("archived", a.archived)
                    put("created_at", a.createdAt)
                })
            }
        })
        root.put("transfers", JSONArray().apply {
            transferDao.all().forEach { t ->
                put(t.toJson())
            }
        })
        root.put("tags", JSONArray().apply {
            tagDao.all().forEach { t ->
                put(JSONObject().apply {
                    put("id", t.id)
                    put("name", t.name)
                    put("color", t.color)
                    put("sort", t.sort)
                })
            }
        })
        root.put("transactionTags", JSONArray().apply {
            tagDao.allLinks().forEach { link ->
                put(JSONObject().apply {
                    put("transaction_id", link.transactionId)
                    put("tag_id", link.tagId)
                })
            }
        })
        root.put("recurringRules", JSONArray().apply {
            recurringDao.all().forEach { r -> put(r.toJson()) }
        })
        return root.toString(2)
    }

    suspend fun importBackup(raw: String, replace: Boolean): ImportResult {
        val root = JSONObject(raw)
        if (root.optString("format") != "jianzhang-backup") {
            throw IllegalArgumentException("文件格式不正确，请选择由简账导出的备份文件")
        }
        val cats = root.optJSONArray("categories") ?: JSONArray()
        val txs = root.optJSONArray("transactions") ?: JSONArray()
        val budgets = root.optJSONArray("budgets") ?: JSONArray()
        val accounts = root.optJSONArray("accounts") ?: JSONArray()
        val transfers = root.optJSONArray("transfers") ?: JSONArray()
        val tags = root.optJSONArray("tags") ?: JSONArray()
        val links = root.optJSONArray("transactionTags") ?: JSONArray()
        val recurring = root.optJSONArray("recurringRules") ?: JSONArray()
        val now = Instant.now().toString()

        if (replace) {
            db.withTransaction {
                tagDao.clearAllLinks()
                transferDao.deleteAll()
                recurringDao.deleteAll()
                reminderDao.clear()
                txDao.deleteAll()
                budgetDao.deleteAll()
                tagDao.deleteAll()
                accountDao.deleteAll()
                catDao.deleteAll()

                val defaultAccountId = importAccounts(accounts, now)
                importCategories(cats)
                importTags(tags)
                importRecurring(recurring, now, null, defaultAccountId)
                importTransactions(txs, now, null, defaultAccountId, null)
                importLinks(links, null, null)
                importTransfers(transfers, now, null, defaultAccountId)
                importBudgets(budgets, null)
            }
        } else {
            db.withTransaction {
                val catMap = mergeCategories(cats)
                val accountMap = mergeAccounts(accounts, now)
                val fallbackAccountId = accountDao.firstActiveId() ?: accountDao.firstId() ?: 0L
                val tagMap = mergeTags(tags)
                val ruleMap = mergeRecurring(recurring, now, catMap, accountMap, fallbackAccountId)
                val txMap = mergeTransactions(txs, now, catMap, accountMap, fallbackAccountId, ruleMap)
                importLinks(links, txMap, tagMap)
                mergeTransfers(transfers, now, accountMap, fallbackAccountId)
                mergeBudgets(budgets, catMap)
            }
        }
        return ImportResult(
            categories = cats.length(),
            transactions = txs.length(),
            budgets = budgets.length(),
            accounts = accounts.length().takeIf { it > 0 } ?: 1,
            transfers = transfers.length(),
            tags = tags.length(),
            recurringRules = recurring.length(),
        )
    }

    private suspend fun importAccounts(arr: JSONArray, now: String): Long {
        var firstId = 0L
        for (i in 0 until arr.length()) {
            val j = arr.getJSONObject(i)
            val id = j.optLong("id", 0L)
            accountDao.insertIgnore(
                Account(
                    id = id,
                    name = j.optString("name"),
                    kind = j.optString("kind", "other"),
                    icon = j.optString("icon", "wallet"),
                    color = j.optString("color", "#1fd0ff"),
                    initialCents = j.optLong("initial_cents", 0L),
                    sort = j.optInt("sort", i),
                    archived = jsonBoolean(j, "archived"),
                    createdAt = j.optString("created_at", now),
                )
            )
            if (firstId == 0L) firstId = id
        }
        if (firstId == 0L) {
            firstId = accountDao.insertIgnore(
                Account(name = "默认账户", kind = "other", icon = "wallet", color = "#1fd0ff", createdAt = now)
            )
        }
        return firstId
    }

    private suspend fun importCategories(arr: JSONArray) {
        for (i in 0 until arr.length()) {
            val j = arr.getJSONObject(i)
            catDao.insertIgnore(
                Category(
                    id = j.optLong("id", 0L),
                    type = j.optString("type", "expense"),
                    name = j.optString("name"),
                    icon = j.optString("icon", "more-horizontal"),
                    sort = j.optInt("sort", 0),
                    builtin = jsonBoolean(j, "builtin"),
                )
            )
        }
    }

    private suspend fun importTags(arr: JSONArray) {
        for (i in 0 until arr.length()) {
            val j = arr.getJSONObject(i)
            tagDao.insertIgnore(
                Tag(
                    id = j.optLong("id", 0L),
                    name = j.optString("name"),
                    color = j.optString("color", "#1fd0ff"),
                    sort = j.optInt("sort", i),
                )
            )
        }
    }

    private suspend fun importRecurring(
        arr: JSONArray,
        now: String,
        catMap: Map<Long, Long>?,
        fallbackAccountId: Long,
    ) {
        for (i in 0 until arr.length()) {
            val j = arr.getJSONObject(i)
            val rawCat = jsonLong(j, "category_id")
            recurringDao.insertIgnore(
                RecurringRule(
                    id = j.optLong("id", 0L),
                    type = j.optString("type", "expense"),
                    amountCents = j.optLong("amount_cents", 0L),
                    categoryId = if (catMap == null) rawCat else rawCat?.let { catMap[it] },
                    accountId = jsonLong(j, "account_id") ?: fallbackAccountId,
                    note = j.optString("note"),
                    frequency = j.optString("frequency", "monthly"),
                    intervalCount = j.optInt("interval_count", 1).coerceAtLeast(1),
                    weekday = jsonIntOrNull(j, "weekday"),
                    dayOfMonth = jsonIntOrNull(j, "day_of_month"),
                    monthOfYear = jsonIntOrNull(j, "month_of_year"),
                    startDate = j.optString("start_date"),
                    endDate = jsonStringOrNull(j, "end_date"),
                    nextDate = j.optString("next_date", j.optString("start_date")),
                    autoCreate = jsonBoolean(j, "auto_create"),
                    enabled = if (j.has("enabled")) jsonBoolean(j, "enabled") else true,
                    createdAt = j.optString("created_at", now),
                )
            )
        }
    }

    private suspend fun importTransactions(
        arr: JSONArray,
        now: String,
        catMap: Map<Long, Long>?,
        fallbackAccountId: Long,
        ruleMap: Map<Long, Long>?,
    ) {
        for (i in 0 until arr.length()) {
            val j = arr.getJSONObject(i)
            val rawCategory = j.optLong("category_id", 0L)
            val rawAccount = jsonLong(j, "account_id") ?: fallbackAccountId
            val rawRecurring = jsonLong(j, "recurring_id")
            txDao.insertIgnore(
                Transaction(
                    id = j.optLong("id", 0L),
                    type = j.optString("type", "expense"),
                    amountCents = j.optLong("amount_cents", 0L),
                    categoryId = catMap?.get(rawCategory) ?: rawCategory,
                    accountId = if (catMap == null) rawAccount else fallbackAccountId,
                    note = j.optString("note"),
                    date = j.optString("date"),
                    createdAt = j.optString("created_at", now),
                    recurringId = if (ruleMap == null) rawRecurring else rawRecurring?.let { ruleMap[it] },
                )
            )
        }
    }

    private suspend fun importLinks(
        links: JSONArray,
        txMap: Map<Long, Long>?,
        tagMap: Map<Long, Long>?,
    ) {
        for (i in 0 until links.length()) {
            val j = links.getJSONObject(i)
            val rawTx = j.optLong("transaction_id", 0L)
            val rawTag = j.optLong("tag_id", 0L)
            val tx = txMap?.get(rawTx) ?: rawTx
            val tag = tagMap?.get(rawTag) ?: rawTag
            if (tx > 0 && tag > 0) tagDao.insertLink(TransactionTag(tx, tag))
        }
    }

    private suspend fun importTransfers(arr: JSONArray, now: String, accountMap: Map<Long, Long>?, fallbackAccountId: Long) {
        for (i in 0 until arr.length()) {
            val t = arr.getJSONObject(i).toTransfer(now)
            val from = accountMap?.get(t.fromAccountId) ?: t.fromAccountId
            val to = accountMap?.get(t.toAccountId) ?: t.toAccountId
            transferDao.insertIgnore(t.copy(fromAccountId = from, toAccountId = to))
        }
    }

    private suspend fun importBudgets(arr: JSONArray, catMap: Map<Long, Long>?) {
        for (i in 0 until arr.length()) {
            val j = arr.getJSONObject(i)
            val rawCat = jsonLong(j, "category_id")
            budgetDao.insertIgnore(
                Budget(
                    id = j.optLong("id", 0L),
                    month = j.optString("month"),
                    categoryId = if (catMap == null) rawCat else rawCat?.let { catMap[it] },
                    amountCents = j.optLong("amount_cents", 0L),
                )
            )
        }
    }

    private suspend fun mergeCategories(arr: JSONArray): Map<Long, Long> {
        val map = HashMap<Long, Long>()
        for (i in 0 until arr.length()) {
            val j = arr.getJSONObject(i)
            val type = j.optString("type", "expense")
            val name = j.optString("name")
            val existing = catDao.findId(type, name)
            val target = existing ?: catDao.insert(
                Category(
                    type = type,
                    name = name,
                    icon = j.optString("icon", "more-horizontal"),
                    sort = j.optInt("sort", 0),
                    builtin = jsonBoolean(j, "builtin"),
                )
            )
            map[j.optLong("id", 0L)] = target
        }
        return map
    }

    private suspend fun mergeAccounts(arr: JSONArray, now: String): Map<Long, Long> {
        val map = HashMap<Long, Long>()
        for (i in 0 until arr.length()) {
            val j = arr.getJSONObject(i)
            val existing = accountDao.all().firstOrNull { it.name == j.optString("name") }
            val target = existing?.id ?: accountDao.insertIgnore(
                Account(
                    name = j.optString("name"),
                    kind = j.optString("kind", "other"),
                    icon = j.optString("icon", "wallet"),
                    color = j.optString("color", "#1fd0ff"),
                    initialCents = j.optLong("initial_cents", 0L),
                    sort = j.optInt("sort", i),
                    archived = jsonBoolean(j, "archived"),
                    createdAt = j.optString("created_at", now),
                )
            )
            map[j.optLong("id", 0L)] = target
        }
        return map
    }

    private suspend fun mergeTags(arr: JSONArray): Map<Long, Long> {
        val map = HashMap<Long, Long>()
        for (i in 0 until arr.length()) {
            val j = arr.getJSONObject(i)
            val name = j.optString("name")
            val existing = tagDao.findId(name)
            val target = existing ?: tagDao.insertIgnore(
                Tag(
                    name = name,
                    color = j.optString("color", "#1fd0ff"),
                    sort = j.optInt("sort", i),
                )
            )
            map[j.optLong("id", 0L)] = target
        }
        return map
    }

    private suspend fun mergeRecurring(
        arr: JSONArray,
        now: String,
        catMap: Map<Long, Long>,
        accountMap: Map<Long, Long>,
        fallbackAccountId: Long,
    ): Map<Long, Long> {
        val map = HashMap<Long, Long>()
        for (i in 0 until arr.length()) {
            val j = arr.getJSONObject(i)
            val rawCat = jsonLong(j, "category_id")
            val rawAccount = jsonLong(j, "account_id")
            val target = recurringDao.insert(
                RecurringRule(
                    type = j.optString("type", "expense"),
                    amountCents = j.optLong("amount_cents", 0L),
                    categoryId = rawCat?.let { catMap[it] },
                    accountId = rawAccount?.let { accountMap[it] } ?: fallbackAccountId,
                    note = j.optString("note"),
                    frequency = j.optString("frequency", "monthly"),
                    intervalCount = j.optInt("interval_count", 1).coerceAtLeast(1),
                    weekday = jsonIntOrNull(j, "weekday"),
                    dayOfMonth = jsonIntOrNull(j, "day_of_month"),
                    monthOfYear = jsonIntOrNull(j, "month_of_year"),
                    startDate = j.optString("start_date"),
                    endDate = jsonStringOrNull(j, "end_date"),
                    nextDate = j.optString("next_date", j.optString("start_date")),
                    autoCreate = jsonBoolean(j, "auto_create"),
                    enabled = if (j.has("enabled")) jsonBoolean(j, "enabled") else true,
                    createdAt = j.optString("created_at", now),
                )
            )
            map[j.optLong("id", 0L)] = target
        }
        return map
    }

    private suspend fun mergeTransactions(
        arr: JSONArray,
        now: String,
        catMap: Map<Long, Long>,
        accountMap: Map<Long, Long>,
        fallbackAccountId: Long,
        ruleMap: Map<Long, Long>,
    ): Map<Long, Long> {
        val map = HashMap<Long, Long>()
        for (i in 0 until arr.length()) {
            val j = arr.getJSONObject(i)
            val rawCat = j.optLong("category_id", 0L)
            val rawAccount = jsonLong(j, "account_id")
            val rawRecurring = jsonLong(j, "recurring_id")
            val target = txDao.insert(
                Transaction(
                    type = j.optString("type", "expense"),
                    amountCents = j.optLong("amount_cents", 0L),
                    categoryId = catMap[rawCat] ?: fallbackCategoryId(j.optString("type", "expense")),
                    accountId = rawAccount?.let { accountMap[it] } ?: fallbackAccountId,
                    note = j.optString("note"),
                    date = j.optString("date"),
                    createdAt = j.optString("created_at", now),
                    recurringId = rawRecurring?.let { ruleMap[it] },
                )
            )
            map[j.optLong("id", 0L)] = target
        }
        return map
    }

    private suspend fun mergeTransfers(
        arr: JSONArray,
        now: String,
        accountMap: Map<Long, Long>,
        fallbackAccountId: Long,
    ) {
        for (i in 0 until arr.length()) {
            val t = arr.getJSONObject(i).toTransfer(now)
            transferDao.insert(
                t.copy(
                    fromAccountId = accountMap[t.fromAccountId] ?: fallbackAccountId,
                    toAccountId = accountMap[t.toAccountId] ?: fallbackAccountId,
                )
            )
        }
    }

    private suspend fun mergeBudgets(arr: JSONArray, catMap: Map<Long, Long>) {
        for (i in 0 until arr.length()) {
            val j = arr.getJSONObject(i)
            val rawCat = jsonLong(j, "category_id")
            val categoryId = rawCat?.let { catMap[it] }
            if (rawCat == null || categoryId != null) {
                budgetDao.insertIgnore(
                    Budget(
                        month = j.optString("month"),
                        categoryId = categoryId,
                        amountCents = j.optLong("amount_cents", 0L),
                    )
                )
            }
        }
    }

    private fun jsonBoolean(j: JSONObject, key: String): Boolean {
        if (!j.has(key)) return false
        val value = j.opt(key)
        return if (value is Boolean) value else j.optInt(key, 0) != 0
    }

    private fun jsonLong(j: JSONObject, key: String): Long? =
        if (!j.has(key) || j.isNull(key)) null else j.optLong(key)

    private fun jsonIntOrNull(j: JSONObject, key: String): Int? =
        if (!j.has(key) || j.isNull(key)) null else j.optInt(key)

    private fun jsonStringOrNull(j: JSONObject, key: String): String? =
        if (!j.has(key) || j.isNull(key)) null else j.optString(key).takeIf { it.isNotBlank() }

    private fun Transaction.toLedger(tagIds: List<Long>) = LedgerEntry(
        kind = type,
        id = id,
        date = date,
        amountCents = amountCents,
        note = note,
        categoryId = categoryId,
        accountId = accountId,
        toAccountId = null,
        feeCents = 0L,
        createdAt = createdAt,
        tagIds = tagIds,
    )

    private fun Transfer.toLedger() = LedgerEntry(
        kind = "transfer",
        id = id,
        date = date,
        amountCents = amountCents,
        note = note,
        categoryId = null,
        accountId = fromAccountId,
        toAccountId = toAccountId,
        feeCents = feeCents,
        createdAt = createdAt,
    )

    private fun Transfer.toJson() = JSONObject().apply {
        put("id", id)
        put("from_account_id", fromAccountId)
        put("to_account_id", toAccountId)
        put("amount_cents", amountCents)
        put("fee_cents", feeCents)
        put("note", note)
        put("date", date)
        put("created_at", createdAt)
    }

    private fun RecurringRule.toJson() = JSONObject().apply {
        put("id", id)
        put("type", type)
        put("amount_cents", amountCents)
        put("category_id", categoryId ?: JSONObject.NULL)
        put("account_id", accountId ?: JSONObject.NULL)
        put("note", note)
        put("frequency", frequency)
        put("interval_count", intervalCount)
        put("weekday", weekday ?: JSONObject.NULL)
        put("day_of_month", dayOfMonth ?: JSONObject.NULL)
        put("month_of_year", monthOfYear ?: JSONObject.NULL)
        put("start_date", startDate)
        put("end_date", endDate ?: JSONObject.NULL)
        put("next_date", nextDate)
        put("auto_create", autoCreate)
        put("enabled", enabled)
        put("created_at", createdAt)
    }

    private fun JSONObject.toTransfer(now: String) = Transfer(
        id = optLong("id", 0L),
        fromAccountId = optLong("from_account_id", 0L),
        toAccountId = optLong("to_account_id", 0L),
        amountCents = optLong("amount_cents", 0L),
        feeCents = optLong("fee_cents", 0L),
        note = optString("note"),
        date = optString("date"),
        createdAt = optString("created_at", now),
    )

    /** 清空全部数据并恢复内置分类与默认账户（忘记密码时调用）。 */
    suspend fun resetAllData() {
        db.withTransaction {
            tagDao.clearAllLinks()
            transferDao.deleteAll()
            recurringDao.deleteAll()
            reminderDao.clear()
            txDao.deleteAll()
            budgetDao.deleteAll()
            tagDao.deleteAll()
            accountDao.deleteAll()
            catDao.deleteAll()
            withContext(Dispatchers.IO) {
                db.openHelper.writableDatabase.execSQL(SEED_CATEGORIES_SQL)
                db.openHelper.writableDatabase.execSQL(SEED_DEFAULT_ACCOUNT_SQL)
            }
        }
    }
}
