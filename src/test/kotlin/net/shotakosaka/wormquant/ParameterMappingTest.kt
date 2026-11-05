package net.shotakosaka.wormquant

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ParameterMappingTest {

    @Test
    fun testSingleIndex() {
        val mapping = ParameterMapping("Auxin", "4")
        assertFalse(mapping.isConstant())
        assertEquals(3..3, mapping.toZeroBasedRange())
    }

    @Test
    fun testRangeWithHyphen() {
        val mapping = ParameterMapping("Genotype", "1-3")
        assertFalse(mapping.isConstant())
        assertEquals(0..2, mapping.toZeroBasedRange())
    }

    @Test
    fun testRangeWithDots() {
        val mapping = ParameterMapping("Genotype", "2..4")
        assertFalse(mapping.isConstant())
        assertEquals(1..3, mapping.toZeroBasedRange())
    }

    @Test
    fun testConstantStringValues() {
        val m1 = ParameterMapping("Species", "C. elegans")
        assertTrue(m1.isConstant())
        assertNull(m1.toZeroBasedRange())
        assertEquals("C. elegans", m1.getConstantValue())

        val m2 = ParameterMapping("Magnification", "20x")
        assertTrue(m2.isConstant())
        assertEquals("20x", m2.getConstantValue())

        // 引用符囲みの数値や'='プレフィックスは定数として扱う
        val m3 = ParameterMapping("NumberConst", "\"4\"")
        assertTrue(m3.isConstant())
        assertEquals("4", m3.getConstantValue())

        val m4 = ParameterMapping("PrefixedConst", "=10")
        assertTrue(m4.isConstant())
        assertEquals("10", m4.getConstantValue())
    }
}
