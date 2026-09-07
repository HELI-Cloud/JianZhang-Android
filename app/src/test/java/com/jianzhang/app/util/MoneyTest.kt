package com.jianzhang.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {
    @Test
    fun formatCents_basic() {
        assertEquals("0.00", formatCents(0))
        assertEquals("12.34", formatCents(1234))
        assertEquals("1,234.56", formatCents(123456))
        assertEquals("1,000,000.00", formatCents(100000000))
        assertEquals("-12.30", formatCents(-1230))
        assertEquals("0.05", formatCents(5))
    }

    @Test
    fun parseYuanToCents_valid() {
        assertEquals(1234L, parseYuanToCents("12.34"))
        assertEquals(1200L, parseYuanToCents("12"))
        assertEquals(1230L, parseYuanToCents("12.3"))
        assertEquals(5L, parseYuanToCents("0.05"))
        assertEquals(100L, parseYuanToCents("1."))
        assertEquals(100L, parseYuanToCents("01"))
        assertEquals(1234L, parseYuanToCents(" 12.34 "))
    }

    @Test
    fun parseYuanToCents_invalid() {
        assertNull(parseYuanToCents(""))
        assertNull(parseYuanToCents("abc"))
        assertNull(parseYuanToCents("-1"))
        assertNull(parseYuanToCents("1.234"))
        assertNull(parseYuanToCents("1.2.3"))
        assertEquals(0L, parseYuanToCents("0"))
        assertNull(parseYuanToCents("."))
    }

    @Test
    fun sanitizeAmountInput_cleans() {
        assertEquals("12.34", sanitizeAmountInput("12.345"))
        assertEquals("12.34", sanitizeAmountInput("1a2.3b4c"))
        assertEquals("12.34", sanitizeAmountInput("12.34.56"))
        assertEquals("7", sanitizeAmountInput("007"))
        assertEquals("0.5", sanitizeAmountInput("0.5"))
    }

    @Test
    fun yuanAxis_formats() {
        assertEquals("0", yuanAxis(50))
        assertEquals("1.2万", yuanAxis(1234500))
        assertEquals("123.5万", yuanAxis(123450000))
    }
}