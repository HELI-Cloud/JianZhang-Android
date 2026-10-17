package com.jianzhang.app.data

import com.jianzhang.app.model.AppSettings
import com.jianzhang.app.model.ReminderItem
import com.jianzhang.app.model.ReminderKind
import com.jianzhang.app.model.ReminderLevel
import com.jianzhang.app.util.formatCents
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

object ReminderEngine {
    suspend fun build(repo: JzRepository, settings: AppSettings): List<ReminderItem> {
        val items = mutableListOf<ReminderItem>()
        if (settings.budgetAlert) items += budgetReminders(repo)
        if (settings.billAlert) items += billReminders(repo)
        items += cadenceReminders(repo, settings)
        val order = mapOf(ReminderLevel.DANGER to 0, ReminderLevel.WARN to 1, ReminderLevel.INFO to 2)
        return items.sortedBy { order[it.level] ?: 3 }
    }

    private suspend fun budgetReminders(repo: JzRepository): List<ReminderItem> {
        val month = LocalDate.now().toString().take(7)
        val budgets = repo.budgetsForMonth(month)
        val summary = repo.monthSummary(month)
        val categories = repo.categories().associateBy { it.id }
        val totals = repo.categoryTotalsRange("$month-01", "$month-31", com.jianzhang.app.model.TxType.EXPENSE)
            .associateBy { it.name }
        val out = mutableListOf<ReminderItem>()
        val totalBudget = budgets.firstOrNull { it.categoryId == null }?.amountCents ?: 0L
        if (totalBudget > 0) {
            val used = summary.expenseCents
            val percent = ((used * 100) / totalBudget).toInt()
            if (used > totalBudget) {
                out += ReminderItem(
                    key = "budget-total-over-$month",
                    kind = ReminderKind.BUDGET,
                    title = "本月总预算已超支",
                    body = "已支出 ¥${formatCents(used)}，超出 ¥${formatCents(used - totalBudget)}（$percent%）。",
                    level = ReminderLevel.DANGER,
                    route = "budget",
                )
            } else if (percent >= 80) {
                out += ReminderItem(
                    key = "budget-total-warn-$month",
                    kind = ReminderKind.BUDGET,
                    title = "本月总预算接近上限",
                    body = "已使用 $percent%，剩余 ¥${formatCents(totalBudget - used)}。",
                    level = ReminderLevel.WARN,
                    route = "budget",
                )
            }
        }
        budgets.filter { it.categoryId != null }.forEach { budget ->
            val name = categories[budget.categoryId]?.name ?: return@forEach
            val spent = totals[name]?.totalCents ?: 0L
            if (spent > budget.amountCents) {
                out += ReminderItem(
                    key = "budget-cat-over-$month-${budget.categoryId}",
                    kind = ReminderKind.BUDGET,
                    title = "「$name」分类预算超支",
                    body = "已支出 ¥${formatCents(spent)}，超出 ¥${formatCents(spent - budget.amountCents)}。",
                    level = ReminderLevel.WARN,
                    route = "budget",
                )
            }
        }
        return out
    }

    private suspend fun billReminders(repo: JzRepository): List<ReminderItem> {
        val today = LocalDate.now().toString()
        val horizon = LocalDate.now().plusDays(3).toString()
        return repo.recurringRules().mapNotNull { rule ->
            if (!rule.enabled) return@mapNotNull null
            if (rule.endDate != null && rule.nextDate > rule.endDate) return@mapNotNull null
            if (rule.nextDate > horizon) return@mapNotNull null
            val label = if (rule.type == "expense") "支出" else "收入"
            val overdue = rule.nextDate < today
            if (!rule.autoCreate) {
                ReminderItem(
                    key = "bill-confirm-${rule.id}-${rule.nextDate}",
                    kind = ReminderKind.BILL,
                    title = if (overdue) "周期账目待确认（已到期）" else "周期账目待确认",
                    body = "${rule.nextDate} · $label ¥${formatCents(rule.amountCents)}${if (rule.note.isNotBlank()) " · ${rule.note}" else ""}",
                    level = if (overdue) ReminderLevel.DANGER else ReminderLevel.WARN,
                    route = "recurring",
                    ruleId = rule.id,
                    confirmable = true,
                )
            } else {
                ReminderItem(
                    key = "bill-due-${rule.id}-${rule.nextDate}",
                    kind = ReminderKind.BILL,
                    title = "周期账目即将记账",
                    body = "${rule.nextDate} · $label ¥${formatCents(rule.amountCents)}${if (rule.note.isNotBlank()) " · ${rule.note}" else ""}",
                    level = ReminderLevel.INFO,
                    route = "recurring",
                )
            }
        }
    }

    private suspend fun cadenceReminders(repo: JzRepository, settings: AppSettings): List<ReminderItem> {
        val out = mutableListOf<ReminderItem>()
        val today = LocalDate.now()
        if (settings.dailyReminder && LocalTime.now() >= parseTime(settings.dailyReminderTime)) {
            val count = repo.countTransactionsInRange(today.toString(), today.toString())
            if (count == 0) {
                out += ReminderItem(
                    key = "daily-$today",
                    kind = ReminderKind.DAILY,
                    title = "今天还没有记账",
                    body = "已过 ${settings.dailyReminderTime}，花一分钟补上今天的收支。",
                    level = ReminderLevel.INFO,
                    route = "ledger",
                )
            }
        }
        if (settings.weeklyReminder) {
            val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val sunday = monday.plusDays(6)
            val target = settings.weeklyReminderWeekday
            val reached = if (target == 0) today.dayOfWeek == DayOfWeek.SUNDAY
            else today.dayOfWeek.value >= target.coerceIn(1, 7)
            if (reached) {
                val count = repo.countTransactionsInRange(monday.toString(), sunday.toString())
                if (count == 0) {
                    out += ReminderItem(
                        key = "weekly-$monday",
                        kind = ReminderKind.WEEKLY,
                        title = "本周账本还是空的",
                        body = "本周（$monday 至 $sunday）还没有任何记录，记得补记。",
                        level = ReminderLevel.INFO,
                        route = "ledger",
                    )
                }
            }
        }
        return out
    }

    fun parseTime(value: String): LocalTime = runCatching {
        val parts = value.split(':')
        LocalTime.of(parts[0].toInt().coerceIn(0, 23), parts.getOrNull(1)?.toInt()?.coerceIn(0, 59) ?: 0)
    }.getOrDefault(LocalTime.of(21, 0))
}
