package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.faction.flag.MfFlag
import com.dansplugins.factionsystem.faction.flag.MfFlagValues
import com.dansplugins.factionsystem.faction.flag.MfFlags
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.player.MfPlayerService
import com.dansplugins.factionsystem.service.Services
import org.bukkit.World
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID

class AsyncPlayerChatListenerTest {
    private lateinit var plugin: MedievalFactions
    private lateinit var config: FileConfiguration
    private lateinit var speaker: Player
    private lateinit var factionsWorldReader: Player
    private lateinit var otherWorldReader: Player

    @BeforeEach
    fun setUp() {
        plugin = mock(MedievalFactions::class.java)
        config = mock(FileConfiguration::class.java)
        `when`(plugin.config).thenReturn(config)
        `when`(config.getBoolean("chat.enableDefaultChatFormatting")).thenReturn(true)

        val services = mock(Services::class.java)
        `when`(plugin.services).thenReturn(services)
        val playerService = mock(MfPlayerService::class.java)
        `when`(services.playerService).thenReturn(playerService)
        val factionService = mock(MfFactionService::class.java)
        `when`(services.factionService).thenReturn(factionService)

        speaker = playerIn("Factions")
        `when`(speaker.displayName).thenReturn("Alice")
        factionsWorldReader = playerIn("Factions")
        otherWorldReader = playerIn("SMP")

        val mfPlayer = mock(MfPlayer::class.java)
        val mfPlayerId = MfPlayerId(UUID.randomUUID().toString())
        `when`(mfPlayer.id).thenReturn(mfPlayerId)
        `when`(playerService.getPlayer(speaker)).thenReturn(mfPlayer)
        val faction = mock(MfFaction::class.java)
        `when`(faction.name).thenReturn("Alpha")
        `when`(factionService.getFaction(mfPlayerId)).thenReturn(faction)

        @Suppress("UNCHECKED_CAST")
        val colorFlag = mock(MfFlag::class.java) as MfFlag<String>
        val flags = mock(MfFlags::class.java)
        `when`(plugin.flags).thenReturn(flags)
        `when`(flags.color).thenReturn(colorFlag)
        val flagValues = mock(MfFlagValues::class.java)
        `when`(faction.flags).thenReturn(flagValues)
        `when`(flagValues[colorFlag]).thenReturn("#ffcc00")
    }

    private fun playerIn(worldName: String): Player {
        val world = mock(World::class.java)
        `when`(world.name).thenReturn(worldName)
        val player = mock(Player::class.java)
        `when`(player.world).thenReturn(world)
        return player
    }

    private fun chatEvent() = AsyncPlayerChatEvent(
        true,
        speaker,
        "hi",
        mutableSetOf(speaker, factionsWorldReader, otherWorldReader)
    )

    @Test
    fun everyoneSeesThePrefixByDefault() {
        `when`(config.getStringList("chat.prefixWorlds")).thenReturn(emptyList())
        val event = chatEvent()

        AsyncPlayerChatListener(plugin).onAsyncPlayerChat(event)

        assertEquals(setOf(speaker, factionsWorldReader, otherWorldReader), event.recipients)
        assertTrue(event.format.contains("Alpha"))
        verify(otherWorldReader, never()).sendMessage("<Alice> hi")
    }

    @Test
    fun readersOutsideThePrefixWorldsGetTheMessageWithoutThePrefix() {
        `when`(config.getStringList("chat.prefixWorlds")).thenReturn(listOf("Factions"))
        val event = chatEvent()

        AsyncPlayerChatListener(plugin).onAsyncPlayerChat(event)

        assertEquals(setOf(speaker, factionsWorldReader), event.recipients)
        assertTrue(event.format.contains("Alpha"))
        verify(otherWorldReader).sendMessage("<Alice> hi")
        verify(factionsWorldReader, never()).sendMessage("<Alice> hi")
    }
}
