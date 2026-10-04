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
import org.bukkit.ChatColor.RED
import org.bukkit.Chunk
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class WorldActionProtectionTest {
    private lateinit var plugin: MedievalFactions
    private lateinit var claimService: MfClaimService
    private lateinit var factionService: MfFactionService
    private lateinit var playerService: MfPlayerService
    private lateinit var config: FileConfiguration
    private lateinit var player: Player
    private lateinit var block: Block
    private lateinit var chunk: Chunk
    private lateinit var event: Cancellable
    private lateinit var mfPlayer: MfPlayer
    private lateinit var claim: MfClaimedChunk
    private val mfPlayerId = MfPlayerId("player")
    private lateinit var uut: WorldActionProtection

    @BeforeEach
    fun setUp() {
        plugin = mock(MedievalFactions::class.java)
        claimService = mock(MfClaimService::class.java)
        factionService = mock(MfFactionService::class.java)
        playerService = mock(MfPlayerService::class.java)
        val services = mock(Services::class.java)
        `when`(plugin.services).thenReturn(services)
        `when`(services.claimService).thenReturn(claimService)
        `when`(services.factionService).thenReturn(factionService)
        `when`(services.playerService).thenReturn(playerService)
        config = mock(FileConfiguration::class.java)
        `when`(plugin.config).thenReturn(config)
        `when`(config.getBoolean("wilderness.interaction.alert", true)).thenReturn(true)
        val language = mock(Language::class.java)
        `when`(language["CannotInteractBlockInWilderness"]).thenReturn("wilderness")
        `when`(language.get(anyString(), anyString())).thenReturn("territory")
        `when`(plugin.language).thenReturn(language)

        player = mock(Player::class.java)
        block = mock(Block::class.java)
        chunk = mock(Chunk::class.java)
        `when`(block.chunk).thenReturn(chunk)
        event = mock(Cancellable::class.java)
        mfPlayer = mock(MfPlayer::class.java)
        `when`(mfPlayer.id).thenReturn(mfPlayerId)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)

        claim = mock(MfClaimedChunk::class.java)
        val claimFactionId = MfFactionId("claim-faction")
        `when`(claim.factionId).thenReturn(claimFactionId)
        val claimFaction = mock(MfFaction::class.java)
        `when`(claimFaction.name).thenReturn("Enemy")
        `when`(factionService.getFaction(claimFactionId)).thenReturn(claimFaction)

        uut = WorldActionProtection(plugin)
    }

    private fun inEnemyClaim() {
        `when`(claimService.getClaim(chunk)).thenReturn(claim)
        `when`(claimService.isInteractionAllowed(mfPlayerId, claim)).thenReturn(false)
    }

    @Test
    fun protect_UnknownPlayer_ShouldCancelWithoutMessage() {
        `when`(playerService.getPlayer(player)).thenReturn(null)

        assertTrue(uut.protect(event, player, block, Material.FLINT_AND_STEEL))

        verify(event).isCancelled = true
        verify(player, never()).sendMessage(anyString())
    }

    @Test
    fun protect_Wilderness_PreventOff_ShouldAllow() {
        `when`(claimService.getClaim(chunk)).thenReturn(null)

        assertFalse(uut.protect(event, player, block, Material.FLINT_AND_STEEL))

        verify(event, never()).isCancelled = true
    }

    @Test
    fun protect_Wilderness_PreventOn_ShouldCancelAndInform() {
        `when`(claimService.getClaim(chunk)).thenReturn(null)
        `when`(config.getBoolean("wilderness.interaction.prevent", false)).thenReturn(true)

        assertTrue(uut.protect(event, player, block, Material.BONE_MEAL))

        verify(event).isCancelled = true
        verify(player).sendMessage("${RED}wilderness")
    }

    @Test
    fun protect_ClaimWherePlayerMayInteract_ShouldAllow() {
        `when`(claimService.getClaim(chunk)).thenReturn(claim)
        `when`(claimService.isInteractionAllowed(mfPlayerId, claim)).thenReturn(true)

        assertFalse(uut.protect(event, player, block, Material.FLINT_AND_STEEL))

        verify(event, never()).isCancelled = true
    }

    @Test
    fun protect_EnemyClaim_ShouldCancelAndInform() {
        inEnemyClaim()

        assertTrue(uut.protect(event, player, block, Material.FLINT_AND_STEEL))

        verify(event).isCancelled = true
        verify(player).sendMessage("${RED}territory")
    }

    @Test
    fun protect_EnemyClaim_Bypass_ShouldAllowWithoutMessage() {
        inEnemyClaim()
        `when`(mfPlayer.isBypassEnabled).thenReturn(true)
        `when`(player.hasPermission("mf.bypass")).thenReturn(true)

        assertFalse(uut.protect(event, player, block, Material.FLINT_AND_STEEL))

        verify(event, never()).isCancelled = true
        verify(player, never()).sendMessage(anyString())
    }

    @Test
    fun protect_EnemyClaim_BypassToggledWithoutPermission_ShouldCancel() {
        inEnemyClaim()
        `when`(mfPlayer.isBypassEnabled).thenReturn(true)

        assertTrue(uut.protect(event, player, block, Material.FLINT_AND_STEEL))

        verify(event).isCancelled = true
    }

    @Test
    fun protect_EnemyClaim_AtWarWithWartimePlaceableItem_ShouldAllow() {
        inEnemyClaim()
        `when`(claimService.isWartimePlaceableBlock(mfPlayerId, claim, Material.FLINT_AND_STEEL)).thenReturn(true)

        assertFalse(uut.protect(event, player, block, Material.FLINT_AND_STEEL))

        verify(event, never()).isCancelled = true
    }

    @Test
    fun protect_EnemyClaim_NoItem_ShouldCancel() {
        inEnemyClaim()

        assertTrue(uut.protect(event, player, block, null))

        verify(event).isCancelled = true
    }

    @Test
    fun protect_ClaimOfUnknownFaction_ShouldAllow() {
        val orphan = mock(MfClaimedChunk::class.java)
        `when`(orphan.factionId).thenReturn(MfFactionId("gone"))
        `when`(claimService.getClaim(chunk)).thenReturn(orphan)

        assertFalse(uut.protect(event, player, block, Material.FLINT_AND_STEEL))

        verify(event, never()).isCancelled = true
    }
}
