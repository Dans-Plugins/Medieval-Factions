package com.dansplugins.factionsystem.utils

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MfServerVersionTest {

    private lateinit var savedProvider: () -> String

    @BeforeEach
    fun setUp() {
        savedProvider = MfServerVersion.versionProvider
    }

    @AfterEach
    fun tearDown() {
        MfServerVersion.versionProvider = savedProvider
    }

    private fun setVersion(version: String) {
        MfServerVersion.versionProvider = { version }
    }

    @Test
    fun isAtLeast_1_21_on_1_21_returnsTrue() {
        setVersion("1.21-R0.1-SNAPSHOT")
        assertTrue(MfServerVersion.isAtLeast(1, 21))
    }

    @Test
    fun isAtLeast_1_21_on_1_22_returnsTrue() {
        setVersion("1.22-R0.1-SNAPSHOT")
        assertTrue(MfServerVersion.isAtLeast(1, 21))
    }

    @Test
    fun isAtLeast_1_21_on_1_20_returnsFalse() {
        setVersion("1.20-R0.1-SNAPSHOT")
        assertFalse(MfServerVersion.isAtLeast(1, 21))
    }

    @Test
    fun isAtLeast_1_21_on_1_17_returnsFalse() {
        setVersion("1.17-R0.1-SNAPSHOT")
        assertFalse(MfServerVersion.isAtLeast(1, 21))
    }

    @Test
    fun isAtLeast_majorOnly_2_on_1_21_returnsFalse() {
        setVersion("1.21-R0.1-SNAPSHOT")
        assertFalse(MfServerVersion.isAtLeast(2))
    }

    @Test
    fun isAtLeast_1_20_on_1_21_3_returnsTrue() {
        setVersion("1.21.3-R0.1-SNAPSHOT")
        assertTrue(MfServerVersion.isAtLeast(1, 20))
    }

    @Test
    fun isAtLeast_1_19_3_on_1_19_3_returnsTrue() {
        setVersion("1.19.3-R0.1-SNAPSHOT")
        assertTrue(MfServerVersion.isAtLeast(1, 19, 3))
    }

    @Test
    fun isAtLeast_1_19_3_on_1_19_4_returnsTrue() {
        setVersion("1.19.4-R0.1-SNAPSHOT")
        assertTrue(MfServerVersion.isAtLeast(1, 19, 3))
    }

    @Test
    fun isAtLeast_1_19_3_on_1_20_returnsTrue() {
        setVersion("1.20-R0.1-SNAPSHOT")
        assertTrue(MfServerVersion.isAtLeast(1, 19, 3))
    }

    @Test
    fun isAtLeast_1_19_3_on_1_19_2_returnsFalse() {
        setVersion("1.19.2-R0.1-SNAPSHOT")
        assertFalse(MfServerVersion.isAtLeast(1, 19, 3))
    }

    @Test
    fun isAtLeast_1_19_3_on_1_19_returnsFalse() {
        setVersion("1.19-R0.1-SNAPSHOT")
        assertFalse(MfServerVersion.isAtLeast(1, 19, 3))
    }
}
