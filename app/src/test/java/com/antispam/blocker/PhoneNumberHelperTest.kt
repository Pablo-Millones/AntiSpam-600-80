package com.antispam.blocker

import com.antispam.blocker.util.PhoneNumberHelper
import org.junit.Assert.*
import org.junit.Test

class PhoneNumberHelperTest {

    private val defaultPrefixes = listOf("600", "80")

    @Test
    fun testBlock600Numbers() {
        val test1 = PhoneNumberHelper.checkIsBlocked("6003004000", defaultPrefixes)
        assertTrue(test1.isBlocked)
        assertEquals("600", test1.matchedPrefix)

        val test2 = PhoneNumberHelper.checkIsBlocked("+56 600 123 4567", defaultPrefixes)
        assertTrue(test2.isBlocked)
        assertEquals("600", test2.matchedPrefix)

        val test3 = PhoneNumberHelper.checkIsBlocked("06001234567", defaultPrefixes)
        assertTrue(test3.isBlocked)
        assertEquals("600", test3.matchedPrefix)
    }

    @Test
    fun testBlock80Numbers() {
        val test1 = PhoneNumberHelper.checkIsBlocked("800123456", defaultPrefixes)
        assertTrue(test1.isBlocked)
        assertEquals("80", test1.matchedPrefix)

        val test2 = PhoneNumberHelper.checkIsBlocked("+56 80 999 8888", defaultPrefixes)
        assertTrue(test2.isBlocked)
        assertEquals("80", test2.matchedPrefix)

        val test3 = PhoneNumberHelper.checkIsBlocked("802345678", defaultPrefixes)
        assertTrue(test3.isBlocked)
        assertEquals("80", test3.matchedPrefix)
    }

    @Test
    fun testAllowLegitimateNumbers() {
        val test1 = PhoneNumberHelper.checkIsBlocked("+56 9 8765 4321", defaultPrefixes)
        assertFalse(test1.isBlocked)

        val test2 = PhoneNumberHelper.checkIsBlocked("223456789", defaultPrefixes)
        assertFalse(test2.isBlocked)

        val test3 = PhoneNumberHelper.checkIsBlocked("+1 555 123 4567", defaultPrefixes)
        assertFalse(test3.isBlocked)
    }
}
