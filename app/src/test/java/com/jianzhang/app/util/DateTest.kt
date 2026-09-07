package com.jianzhang.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class DateTest {
    @Test
    fun shiftMonth_works() {
        assertEquals("2026-08", shiftMonth("2026-08", 0))
        assertEquals("2026-07", shiftMonth("2026-08", -1))
        assertEquals("2026-09", shiftMonth("2026-08", 1))
        assertEquals("2027-01", shiftMonth("2026-12", 1))
        assertEquals("2025-12", shiftMonth("2026-01", -1))
    }

    @Test
    fun lastNMonths_works() {
        assertEquals(
            listOf("2026-03", "2026-04", "2026-05", "2026-06", "2026-07", "2026-08"),
            lastNMonths(6, "2026-08"),
        )
    }

    @Test
    fun labels_work() {
        assertEquals("2026年8月", monthLabel("2026-08"))
        assertEquals("8月", shortMonth("2026-08"))
        assertEquals("8月9日 周日", dateLabel("2026-08-09"))
        assertEquals(31, monthDays("2026-08"))
        assertEquals(28, monthDays("2026-02"))
    }
}