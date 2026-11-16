package com.dansplugins.factionsystem.command.faction.vassalage

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.command.faction.declareindependence.MfFactionDeclareIndependenceCommand
import com.dansplugins.factionsystem.command.faction.declinevassalization.MfFactionDeclineVassalizationCommand
import com.dansplugins.factionsystem.command.faction.grantindependence.MfFactionGrantIndependenceCommand
import com.dansplugins.factionsystem.command.faction.swearfealty.MfFactionSwearFealtyCommand
import com.dansplugins.factionsystem.command.faction.vassalize.MfFactionVassalizeCommand
import com.dansplugins.factionsystem.lang.Language
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`

/** Covers the `/faction vassalage` dispatcher (#1639). */
class MfFactionVassalageCommandTest {

    private lateinit var plugin: MedievalFactions
    private lateinit var sender: CommandSender
    private lateinit var command: Command
    private lateinit var offer: MfFactionVassalizeCommand
    private lateinit var accept: MfFactionSwearFealtyCommand
    private lateinit var decline: MfFactionDeclineVassalizationCommand
    private lateinit var revolt: MfFactionDeclareIndependenceCommand
    private lateinit var release: MfFactionGrantIndependenceCommand
    private lateinit var rescind: MfFactionVassalageRescindCommand
    private lateinit var uut: MfFactionVassalageCommand

    @BeforeEach
    fun setUp() {
        plugin = mock(MedievalFactions::class.java)
        val language = mock(Language::class.java) { invocation -> invocation.arguments.joinToString("|") }
        `when`(plugin.language).thenReturn(language)
        sender = mock(CommandSender::class.java)
        command = mock(Command::class.java)
        offer = mock(MfFactionVassalizeCommand::class.java)
        accept = mock(MfFactionSwearFealtyCommand::class.java)
        decline = mock(MfFactionDeclineVassalizationCommand::class.java)
        revolt = mock(MfFactionDeclareIndependenceCommand::class.java)
        release = mock(MfFactionGrantIndependenceCommand::class.java)
        rescind = mock(MfFactionVassalageRescindCommand::class.java)
        uut = MfFactionVassalageCommand(plugin, offer, accept, decline, revolt, release, rescind)
    }

    @Test
    fun eachSubcommandRoutesToItsCommandWithTheRemainingArguments() {
        val routes = listOf<Pair<String, CommandExecutor>>(
            "offer" to offer,
            "accept" to accept,
            "decline" to decline,
            "release" to release,
            "rescind" to rescind
        )
        routes.forEach { (subcommand, delegate) ->
            uut.onCommand(sender, command, "f", arrayOf(subcommand, "Red", "Kingdom"))
            verify(delegate).onCommand(sender, command, "f", arrayOf("Red", "Kingdom"))
        }
    }

    @Test
    fun revoltRoutesToDeclareIndependenceWithNoArguments() {
        uut.onCommand(sender, command, "f", arrayOf("revolt"))

        verify(revolt).onCommand(sender, command, "f", arrayOf())
    }

    @Test
    fun subcommandsAreMatchedCaseInsensitively() {
        uut.onCommand(sender, command, "f", arrayOf("OFFER", "South"))

        verify(offer).onCommand(sender, command, "f", arrayOf("South"))
    }

    @Test
    fun theDelegatesResultIsReturned() {
        `when`(accept.onCommand(sender, command, "f", arrayOf("South"))).thenReturn(false)

        assertEquals(false, uut.onCommand(sender, command, "f", arrayOf("accept", "South")))
    }

    @Test
    fun noSubcommandShowsTheUsage() {
        assertTrue(uut.onCommand(sender, command, "f", arrayOf()))

        verify(sender).sendMessage("${RED}CommandFactionVassalageUsage")
        verifyNothingDelegated()
    }

    @Test
    fun anUnknownSubcommandShowsTheUsage() {
        assertTrue(uut.onCommand(sender, command, "f", arrayOf("conquer", "South")))

        verify(sender).sendMessage("${RED}CommandFactionVassalageUsage")
        verifyNothingDelegated()
    }

    @Test
    fun tabCompletionListsTheSubcommands() {
        val completions = uut.onTabComplete(sender, command, "f", arrayOf(""))

        assertTrue(completions.containsAll(listOf("offer", "accept", "decline", "revolt", "release", "rescind")))
        assertEquals(listOf("revolt", "release", "rescind"), uut.onTabComplete(sender, command, "f", arrayOf("RE")))
    }

    @Test
    fun tabCompletionAfterASubcommandIsHandedToItsCommand() {
        `when`(rescind.onTabComplete(sender, command, "f", arrayOf("So"))).thenReturn(listOf("South"))

        assertEquals(listOf("South"), uut.onTabComplete(sender, command, "f", arrayOf("rescind", "So")))
    }

    private fun verifyNothingDelegated() {
        verifyNoInteractions(offer, accept, decline, revolt, release, rescind)
    }
}
