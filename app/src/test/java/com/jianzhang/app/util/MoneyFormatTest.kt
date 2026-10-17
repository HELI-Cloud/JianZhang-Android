package com.jianzhang.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 回归测试：金额输入框回填不得出现科学计数法（旧实现 (cents / 100.0).toString()
 * 在 1900 亿元级别会输出 1.9E10，编辑保存后金额即被写错）。
 */
class MoneyFormatTest {

    @Test
    fun yuanPlainKeepsPlainDecimalForHugeAmounts() {
        assertEquals("190000000000", yuanPlain(19_000_000_000_000L))
        assertEquals("1234.56", yuanPlain(123_456L))
    }

    @Test
    fun yuanPlainTrimsOnlyTrailingZeros() {
        assertEquals("10", yuanPlain(1_000L))
        assertEquals("10.5", yuanPlain(1_050L))
        assertEquals("10.05", yuanPlain(1_005L))
        assertEquals("", yuanPlain(0L))
        assertEquals("-3.2", yuanPlain(-320L))
    }

    @Test
    fun yuanPlainRoundTripsThroughParse() {
        val samples = listOf(0L, 1L, 99L, 100L, 1_005L, 1_050L, 123_456L, 19_000_000_000_000L)
        for (cents in samples) {
            val text = yuanPlain(cents)
            val back = if (text.isEmpty()) 0L else parseYuanToCents(text)
            assertEquals("round trip failed for $cents", cents, back)
        }
    }

    @Test
    fun yuanPlainIsParseableAsInput() {
        for (cents in listOf(1L, 100L, 1_005L, 1_050L, 19_000_000_000_000L)) {
            val text = yuanPlain(cents)
            assertEquals(text, sanitizeAmountInput(text))
        }
    }
}
