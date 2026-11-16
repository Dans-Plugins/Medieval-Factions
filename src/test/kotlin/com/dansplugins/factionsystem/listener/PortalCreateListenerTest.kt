package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.claim.MfClaimService
import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.player.MfPlayerService
import com.dansplugins.factionsystem.service.Services
import org.bukkit.ChatColor
import org.bukkit.World
import org.bukkit.block.BlockState
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.world.PortalCreateEvent
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID

class PortalCreateListenerTest {
    private val worldId = UUID.randomUUID()
    private val factionId = MfFactionId.generate()
    private val playerId = MfPlayerId(UUID.randomUUID().toString())

    private lateinit var plugin: MedievalFactions
    private lateinit var config: FileConfiguration
    private lateinit var claimService: MfClaimService
    private lateinit var playerService: MfPlayerService
    private lateinit var world: World
    private lateinit var player: Player
    private lateinit var claim: MfClaimedChunk
    private lateinit var uut: PortalCreateListener

    @BeforeEach
    fun setUp() {
        plugin = mock(MedievalFactions::class.java)
        config = mock(FileConfiguration::class.java)
        `when`(plugin.config).thenReturn(config)
        val services = mock(Services::class.java)
        `when`(plugin.services).thenReturn(services)
        claimService = mock(MfClaimService::class.java)
        `when`(services.claimService).thenReturn(claimService)
        playerService = mock(MfPlayerService::class.java)
        `when`(services.playerService).thenReturn(playerService)
        val factionService = mock(MfFactionService::class.java)
        `when`(services.factionService).thenReturn(factionService)
        val faction = mock(MfFaction::class.java)
        `when`(faction.name).thenReturn("Alpha")
        `when`(factionService.getFaction(factionId)).thenReturn(faction)
        val language = mock(Language::class.java)
        `when`(language["CannotGeneratePortalInFactionTerritory", "Alpha"]).thenReturn("No portal in Alpha")
        `when`(plugin.language).thenReturn(language)

        world = mock(World::class.java)
        `when`(world.uid).thenReturn(worldId)
        player = mock(Player::class.java)
        val mfPlayer = mock(MfPlayer::class.java)
        `when`(mfPlayer.id).thenReturn(playerId)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        // The portal's blocks sit in chunk (2, 3)
        claim = MfClaimedChunk(worldId, 2, 3, factionId)
        `when`(claimService.getClaim(worldId, 2, 3)).thenReturn(claim)

        uut = PortalCreateListener(plugin)
    }

    private fun portalEvent(reason: PortalCreateEvent.CreateReason = PortalCreateEvent.CreateReason.NETHER_PAIR): PortalCreateEvent {
        val blocks = listOf(32 to 48, 33 to 48).map { (x, z) ->
            val state = mock(BlockState::class.java)
            `when`(state.x).thenReturn(x)
            `when`(state.z).thenReturn(z)
            state
        }
        return PortalCreateEvent(blocks, world, player, reason)
    }

    @Test
    fun generatedPortalInClaimIsAllowedWhenTheOptionIsOff() {
        val event = portalEvent()

        uut.onPortalCreate(event)

        assertFalse(event.isCancelled)
    }

    @Test
    fun generatedPortalInAnotherFactionsClaimIsRefusedWhenTheOptionIsOn() {
        `when`(config.getBoolean("factions.preventPortalGenerationInClaims")).thenReturn(true)
        val event = portalEvent()

        uut.onPortalCreate(event)

        assertTrue(event.isCancelled)
        verify(player).sendMessage("${ChatColor.RED}No portal in Alpha")
    }

    @Test
    fun generatedPortalIsAllowedForAPlayerWhoCouldBuildThere() {
        `when`(config.getBoolean("factions.preventPortalGenerationInClaims")).thenReturn(true)
        `when`(claimService.isInteractionAllowed(playerId, claim)).thenReturn(true)
        val event = portalEvent()

        uut.onPortalCreate(event)

        assertFalse(event.isCancelled)
    }

    @Test
    fun generatedPortalInTheWildernessIsAllowed() {
        `when`(config.getBoolean("factions.preventPortalGenerationInClaims")).thenReturn(true)
        `when`(claimService.getClaim(worldId, 2, 3)).thenReturn(null)
        val event = portalEvent()

        uut.onPortalCreate(event)

        assertFalse(event.isCancelled)
    }

    @Test
    fun portalLitByAPlayerIsNotCovered() {
        `when`(config.getBoolean("factions.preventPortalGenerationInClaims")).thenReturn(true)
        val event = portalEvent(PortalCreateEvent.CreateReason.FIRE)

        uut.onPortalCreate(event)

        assertFalse(event.isCancelled)
    }
}
