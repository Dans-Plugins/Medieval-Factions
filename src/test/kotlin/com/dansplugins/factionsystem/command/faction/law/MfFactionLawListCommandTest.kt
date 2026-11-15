package com.dansplugins.factionsystem.command.faction.law

import com.dansplugins.factionsystem.MedievalFactions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

/**
 * Covers the `[faction] [page]` arguments of `/f law list`. See https://github.com/Dans-Plugins/Medieval-Factions/issues/1649.
 */
class MfFactionLawListCommandTest {
    private val uut = MfFactionLawListCommand(mock(MedievalFactions::class.java))
    private val existingFactions = setOf("Alpha", "Two Words", "7")
    private val factionExists: (String) -> Boolean = { it in existingFactions }

    private fun parse(vararg args: String) = uut.parseArgs(arrayOf(*args), factionExists)

    @Test
    fun `no arguments lists the first page of the sender's own faction`() {
        assertEquals(MfFactionLawListCommand.ParsedArgs(null, 0), parse())
    }

    @Test
    fun `a single number that is not a faction name is a page of the sender's own faction`() {
        assertEquals(MfFactionLawListCommand.ParsedArgs(null, 1), parse("2"))
    }

    @Test
    fun `a single argument naming a faction lists that faction, even when the name is a number`() {
        assertEquals(MfFactionLawListCommand.ParsedArgs("Alpha", 0), parse("Alpha"))
        assertEquals(MfFactionLawListCommand.ParsedArgs("7", 0), parse("7"))
    }

    @Test
    fun `an unknown faction name is passed on so the not-found message can name it`() {
        assertEquals(MfFactionLawListCommand.ParsedArgs("Nobody", 0), parse("Nobody"))
    }

    @Test
    fun `a faction followed by a number is that faction's page`() {
        assertEquals(MfFactionLawListCommand.ParsedArgs("Alpha", 2), parse("Alpha", "3"))
        assertEquals(MfFactionLawListCommand.ParsedArgs("7", 1), parse("7", "2"))
    }

    @Test
    fun `a quoted faction name with spaces is read as one name`() {
        assertEquals(MfFactionLawListCommand.ParsedArgs("Two Words", 1), parse("\"Two", "Words\"", "2"))
        assertEquals(MfFactionLawListCommand.ParsedArgs("Two Words", 0), parse("\"Two", "Words\""))
    }

    @Test
    fun `page links quote the faction name so it survives spaces`() {
        assertEquals("", MfFactionLawListCommand.ParsedArgs(null, 0).factionCommandArgument)
        assertEquals(" \"Two Words\"", MfFactionLawListCommand.ParsedArgs("Two Words", 0).factionCommandArgument)
    }
}
