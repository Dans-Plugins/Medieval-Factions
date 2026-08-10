package com.dansplugins.factionsystem.command.faction.migrate

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.TestUtils
import org.bukkit.ChatColor.RED
import org.bukkit.ChatColor.YELLOW
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

/**
 * Covers the guard rails on `/mf migrate` — the branches that run on the calling thread and never touch
 * a data source. The `toJson` and `toDatabase` paths hand off to the scheduler and open a real database
 * connection, so they are out of scope here.
 *
 * These assertions pin the command's current hardcoded English strings. The command does not use
 * `plugin.language[...]` like its sibling subcommands do; that is a known, deliberately deferred gap.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MfFactionMigrateCommandTest {
    private val testUtils = TestUtils()

    private lateinit var fixture: TestUtils.CommandTestFixture
    private lateinit var plugin: MedievalFactions
    private lateinit var uut: MfFactionMigrateCommand

    @BeforeEach
    fun setUp() {
        fixture = testUtils.createCommandTestFixture()
        plugin = mock(MedievalFactions::class.java)
        uut = MfFactionMigrateCommand(plugin)
    }

    @Test
    fun `rejects a sender without the mf migrate permission`() {
        val sender = fixture.sender
        `when`(sender.hasPermission("mf.migrate")).thenReturn(false)

        val result = uut.onCommand(sender, fixture.command, "label", arrayOf("toJson"))

        assertTrue(result)
        verify(sender).sendMessage("$RED You do not have permission to use this command.")
        // Crucially, no migration may be started for an unauthorised sender.
        verify(plugin, never()).server
    }

    @Test
    fun `prints usage and starts nothing when no type is given`() {
        val sender = fixture.sender
        `when`(sender.hasPermission("mf.migrate")).thenReturn(true)

        val result = uut.onCommand(sender, fixture.command, "label", arrayOf())

        assertTrue(result)
        verify(sender).sendMessage("$YELLOW  Usage: /mf migrate <type>")
        verify(sender).sendMessage("$RED  WARNING: Always backup your data before migrating!")
        verify(plugin, never()).server
    }

    @Test
    fun `rejects an unrecognised migration type`() {
        val sender = fixture.sender
        `when`(sender.hasPermission("mf.migrate")).thenReturn(true)

        val result = uut.onCommand(sender, fixture.command, "label", arrayOf("toXml"))

        assertTrue(result)
        // The command echoes the lowercased argument, not what the sender typed.
        verify(sender).sendMessage("$RED Invalid migration type: toxml")
        verify(sender).sendMessage("$YELLOW  Valid options: toJson, toDatabase")
        verify(plugin, never()).server
    }

    @Test
    fun `does not leak the usage text to a sender without permission`() {
        val sender = fixture.sender
        `when`(sender.hasPermission("mf.migrate")).thenReturn(false)

        uut.onCommand(sender, fixture.command, "label", arrayOf())

        verify(sender).sendMessage(anyString())
        verify(sender).sendMessage("$RED You do not have permission to use this command.")
    }

    @Test
    fun `tab completes both migration types and filters case-insensitively`() {
        val sender = fixture.sender

        assertEquals(
            listOf("toJson", "toDatabase"),
            uut.onTabComplete(sender, fixture.command, "label", arrayOf(""))
        )
        assertEquals(
            listOf("toDatabase"),
            uut.onTabComplete(sender, fixture.command, "label", arrayOf("tod"))
        )
        assertTrue(uut.onTabComplete(sender, fixture.command, "label", arrayOf("xyz")).isEmpty())
        // Only the first argument is completed.
        assertTrue(uut.onTabComplete(sender, fixture.command, "label", arrayOf("toJson", "")).isEmpty())
    }
}
