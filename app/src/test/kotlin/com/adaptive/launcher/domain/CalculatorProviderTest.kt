package com.adaptive.launcher.domain

import com.adaptive.launcher.domain.search.CalculatorProvider
import org.junit.Assert.*
import org.junit.Test

class CalculatorProviderTest {
    private val calc = CalculatorProvider()
    @Test fun add(){ assertEquals("4", calc.evaluate("2+2")) }
    @Test fun multiply(){ assertEquals("228", calc.evaluate("12*19")) }
    @Test fun divide(){ assertEquals("5", calc.evaluate("10/2")) }
    @Test fun withSpaces(){ assertEquals("7", calc.evaluate("3 + 4")) }
    @Test fun invalid_returnsNull(){ assertNull(calc.evaluate("hello")) }
    @Test fun empty_returnsNull(){ assertNull(calc.evaluate("")) }
    @Test fun divideByZero_null(){ assertNull(calc.evaluate("5/0")) }
    @Test fun floatResult(){ assertNotNull(calc.evaluate("5/2")) }
}
