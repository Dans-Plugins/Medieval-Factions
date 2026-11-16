package com.dansplugins.factionsystem.command.faction.law

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Covers how `/f law section [number] [section]` reads the section name. See https://github.com/Dans-Plugins/Medieval-Factions/issues/1650.
 */
class MfFactionLawSectionCommandTest {
    @Test
    fun `the words after the law are joined into the section name`() {
        assertEquals("Trade and Taxes", MfFactionLawSectionCommand.parseSection(listOf("Trade", "and", "Taxes")))
    }

    @Test
    fun `no words after the law clears the section`() {
        assertNull(MfFactionLawSectionCommand.parseSection(emptyList()))
    }

    @Test
    fun `only spaces after the law clears the section`() {
        assertNull(MfFactionLawSectionCommand.parseSection(listOf("", " ")))
    }
}
