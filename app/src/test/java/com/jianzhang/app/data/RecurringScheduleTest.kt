package com.jianzhang.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RecurringScheduleTest {
    @Test
    fun monthlyRuleKeepsOriginalMonthEndAnchor() {
        val anchor = "2026-01-31"
        assertEquals("2026-02-28", RecurringSchedule.occurrenceAt(anchor, 1, "monthly", 1))
        assertEquals("2026-03-31", RecurringSchedule.occurrenceAt(anchor, 2, "monthly", 1))
        assertEquals("2026-04-30", RecurringSchedule.occurrenceAt(anchor, 3, "monthly", 1))
    }

    @Test
    fun leapYearAndYearlyRuleStayStable() {
        assertEquals("2028-02-29", RecurringSchedule.occurrenceAt("2024-02-29", 4, "yearly", 1))
        assertEquals("2027-02-28", RecurringSchedule.occurrenceAt("2024-02-29", 3, "yearly", 1))
    }

    @Test
    fun nextOccurrenceIsStrictlyAfterDate() {
        assertEquals("2026-06-10", RecurringSchedule.nextOccurrence("2026-05-10", "2026-05-10", "monthly", 1))
        assertEquals("2026-05-17", RecurringSchedule.nextOccurrence("2026-05-10", "2026-05-11", "weekly", 1))
    }
}
