package com.lifeos.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CalculatorTests {

    @Test
    fun `respects operator precedence`() {
        assertEquals(14.0, Calculator.evaluate("2 + 3 * 4"), 0.0001)
    }

    @Test
    fun `handles parentheses`() {
        assertEquals(20.0, Calculator.evaluate("(2 + 3) * 4"), 0.0001)
    }

    @Test
    fun `handles unary minus and decimals`() {
        assertEquals(-2.5, Calculator.evaluate("-5 / 2"), 0.0001)
        assertEquals(0.5, Calculator.evaluate("0,5"), 0.0001)
    }

    @Test
    fun `handles power and modulo`() {
        assertEquals(8.0, Calculator.evaluate("2 ^ 3"), 0.0001)
        assertEquals(1.0, Calculator.evaluate("10 % 3"), 0.0001)
    }

    @Test
    fun `division by zero is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { Calculator.evaluate("1 / 0") }
    }

    @Test
    fun `malformed expressions are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { Calculator.evaluate("2 +") }
        assertThrows(IllegalArgumentException::class.java) { Calculator.evaluate("(2 + 3") }
        assertThrows(IllegalArgumentException::class.java) { Calculator.evaluate("abc") }
    }

    @Test
    fun `format drops trailing zeros`() {
        assertEquals("6", Calculator.format(6.0))
        assertEquals("2.5", Calculator.format(2.5))
        assertEquals("0.3333333333", Calculator.format(1.0 / 3.0))
    }
}
