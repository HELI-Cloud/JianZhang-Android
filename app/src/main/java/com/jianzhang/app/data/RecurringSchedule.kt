package com.jianzhang.app.data

import java.time.LocalDate
import java.time.YearMonth

/**
 * Recurring dates are always expanded from the original anchor. That prevents a
 * 31st-of-the-month rule from drifting to the 28th after February.
 */
object RecurringSchedule {
    fun occurrenceAt(anchor: String, index: Int, frequency: String, interval: Int): String {
        val base = LocalDate.parse(anchor)
        val step = interval.coerceAtLeast(1)
        return when (frequency) {
            "daily" -> base.plusDays(index.toLong() * step)
            "weekly" -> base.plusWeeks(index.toLong() * step)
            "yearly" -> {
                val targetYear = base.year + index * step
                val month = base.monthValue
                val day = base.dayOfMonth.coerceAtMost(YearMonth.of(targetYear, month).lengthOfMonth())
                LocalDate.of(targetYear, month, day)
            }
            else -> {
                val target = YearMonth.from(base).plusMonths(index.toLong() * step)
                val day = base.dayOfMonth.coerceAtMost(target.lengthOfMonth())
                target.atDay(day)
            }
        }.toString()
    }

    /** Strictly greater than [after]. */
    fun nextOccurrence(anchor: String, after: String, frequency: String, interval: Int): String {
        for (index in 0..2000) {
            val candidate = occurrenceAt(anchor, index, frequency, interval)
            if (candidate > after) return candidate
        }
        return occurrenceAt(anchor, 2001, frequency, interval)
    }

    /** First occurrence on or after [target]. */
    fun firstOccurrenceOnOrAfter(anchor: String, target: String, frequency: String, interval: Int): String {
        for (index in 0..2000) {
            val candidate = occurrenceAt(anchor, index, frequency, interval)
            if (candidate >= target) return candidate
        }
        return occurrenceAt(anchor, 2001, frequency, interval)
    }

    fun label(frequency: String, interval: Int): String {
        val every = if (interval > 1) "每 $interval " else "每"
        return when (frequency) {
            "daily" -> "${every}天"
            "weekly" -> "${every}周"
            "yearly" -> "${every}年"
            else -> "${every}月"
        }
    }

    /** Approximate monthly load used by the recurring overview. */
    fun monthlyEquivalentCents(rule: RecurringRule): Long {
        val interval = rule.intervalCount.coerceAtLeast(1).toLong()
        return when (rule.frequency) {
            "daily" -> rule.amountCents * 30 / interval
            "weekly" -> (rule.amountCents * 52 / 12) / interval
            "yearly" -> (rule.amountCents / 12) / interval
            else -> rule.amountCents / interval
        }
    }
}
