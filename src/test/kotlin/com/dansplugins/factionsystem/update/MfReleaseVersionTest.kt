package com.dansplugins.factionsystem.update

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MfReleaseVersionTest {

    private fun v(text: String) = MfReleaseVersion.parse(text) ?: error("unparseable: $text")

    @Test
    fun testParseStripsLeadingV() {
        assertEquals(MfReleaseVersion(7, 0, 0), v("v7.0.0"))
        assertEquals(MfReleaseVersion(7, 0, 0), v("7.0.0"))
        assertEquals(MfReleaseVersion(7, 0, 0), v("V7.0.0"))
    }

    @Test
    fun testParsePreReleaseAndMissingPatch() {
        assertEquals(MfReleaseVersion(7, 0, 1, listOf("SNAPSHOT")), v("7.0.1-SNAPSHOT"))
        assertEquals(MfReleaseVersion(5, 8, 0), v("5.8"))
        assertEquals(MfReleaseVersion(6, 0, 0, listOf("rc", "1")), v("6.0.0-rc.1+build.5"))
    }

    @Test
    fun testParseRejectsNonVersions() {
        assertNull(MfReleaseVersion.parse("dev"))
        assertNull(MfReleaseVersion.parse(""))
        assertNull(MfReleaseVersion.parse(null))
        assertNull(MfReleaseVersion.parse("7"))
        assertNull(MfReleaseVersion.parse("7.0.0 beta"))
    }

    @Test
    fun testNumericComponentsCompareNumerically() {
        assertTrue(v("5.10.0") > v("5.9.9"))
        assertTrue(v("10.0.0") > v("9.99.99"))
        assertTrue(v("7.0.1") > v("7.0.0"))
        assertTrue(v("6.1.0") > v("6.0.9"))
        assertEquals(0, v("v7.0.0").compareTo(v("7.0.0")))
    }

    @Test
    fun testPreReleaseSortsBeforeItsReleaseButAfterEarlierOnes() {
        assertTrue(v("7.0.1-SNAPSHOT") < v("7.0.1"))
        assertTrue(v("7.0.1-SNAPSHOT") > v("7.0.0"))
        assertTrue(v("6.0.0-rc.2") > v("6.0.0-rc.1"))
        assertTrue(v("6.0.0-rc.10") > v("6.0.0-rc.9"))
        assertTrue(v("6.0.0-alpha") < v("6.0.0-alpha.1"))
        assertTrue(v("6.0.0-1") < v("6.0.0-alpha"))
    }

    @Test
    fun testToStringIsNormalised() {
        assertEquals("7.0.0", v("v7.0.0").toString())
        assertEquals("7.0.1-SNAPSHOT", v("7.0.1-SNAPSHOT").toString())
    }
}
