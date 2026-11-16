package com.dansplugins.factionsystem.command.faction.role

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Covers how `/f role setprefix [role] (prefix)` reads the prefix. See https://github.com/Dans-Plugins/Medieval-Factions/issues/1525.
 */
class MfFactionRoleSetPrefixCommandTest {
    @Test
    fun `the words after the role are joined into the prefix`() {
        assertEquals("&6[Sir Knight]", MfFactionRoleSetPrefixCommand.parsePrefix(listOf("&6[Sir", "Knight]")))
    }

    @Test
    fun `no words after the role clears the prefix`() {
        assertNull(MfFactionRoleSetPrefixCommand.parsePrefix(emptyList()))
    }

    @Test
    fun `only spaces after the role clears the prefix`() {
        assertNull(MfFactionRoleSetPrefixCommand.parsePrefix(listOf(" ", "")))
    }
}
