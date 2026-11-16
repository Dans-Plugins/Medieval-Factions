package com.dansplugins.factionsystem.slimefun

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.claim.MfClaimService
import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.faction.flag.MfFlag
import com.dansplugins.factionsystem.faction.flag.MfFlagValues
import com.dansplugins.factionsystem.faction.flag.MfFlags
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.player.MfPlayerService
import com.dansplugins.factionsystem.service.Services
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.ProtectionModule
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.type.Door
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID

/**
 * Covers the Slimefun protection module (#1750): Slimefun asks it whether a player may use, break or place one of
 * its blocks, and it must answer with the same claim rules Medieval Factions applies to ordinary blocks.
 * Medieval Factions' services are mocked; no server or Slimefun instance is needed.
 */
class MfSlimefunProtectionPolicyTest {
    private val worldId = UUID.randomUUID()
    private val claimFactionId = MfFactionId.generate()
    private val playerUuid = UUID.randomUUID()
    private val mfPlayerId = MfPlayerId(playerUuid.toString())

    private lateinit var plugin: MedievalFactions
    private lateinit var config: FileConfiguration
    private lateinit var claimService: MfClaimService
    private lateinit var factionService: MfFactionService
    private lateinit var playerService: MfPlayerService
    private lateinit var world: World
    private lateinit var block: Block
    private lateinit var location: Location
    private lateinit var offlinePlayer: OfflinePlayer
    private lateinit var onlinePlayer: Player
    private lateinit var claim: MfClaimedChunk
    private lateinit var claimFaction: MfFaction
    private lateinit var mobProtectionFlag: MfFlag<Boolean>
    private lateinit var uut: MfSlimefunProtectionPolicy

    @BeforeEach
    fun setUp() {
        plugin = mock(MedievalFactions::class.java)
        config = mock(FileConfiguration::class.java)
        `when`(plugin.config).thenReturn(config)
        `when`(config.getBoolean("slimefun.protection.enabled")).thenReturn(true)

        val services = mock(Services::class.java)
        `when`(plugin.services).thenReturn(services)
        claimService = mock(MfClaimService::class.java)
        factionService = mock(MfFactionService::class.java)
        playerService = mock(MfPlayerService::class.java)
        `when`(services.claimService).thenReturn(claimService)
        `when`(services.factionService).thenReturn(factionService)
        `when`(services.playerService).thenReturn(playerService)

        mobProtectionFlag = MfFlag.boolean(plugin, "enableMobProtection", false)
        val flags = mock(MfFlags::class.java)
        `when`(flags.enableMobProtection).thenReturn(mobProtectionFlag)
        `when`(plugin.flags).thenReturn(flags)

        // The location is block (37, 64, -20), in chunk (2, -2), which `claimFactionId` holds
        world = mock(World::class.java)
        `when`(world.uid).thenReturn(worldId)
        block = mock(Block::class.java)
        `when`(block.type).thenReturn(Material.DISPENSER)
        `when`(block.blockData).thenReturn(mock(BlockData::class.java))
        location = mock(Location::class.java)
        `when`(location.world).thenReturn(world)
        `when`(location.blockX).thenReturn(37)
        `when`(location.blockZ).thenReturn(-20)
        `when`(location.block).thenReturn(block)
        claim = MfClaimedChunk(worldId, 2, -2, claimFactionId)
        `when`(claimService.getClaim(world, 2, -2)).thenReturn(claim)
        claimFaction = mock(MfFaction::class.java)
        `when`(claimFaction.flags).thenReturn(MfFlagValues(plugin, mapOf("enableMobProtection" to true)))
        `when`(factionService.getFaction(claimFactionId)).thenReturn(claimFaction)

        offlinePlayer = mock(OfflinePlayer::class.java)
        onlinePlayer = mock(Player::class.java)
        `when`(offlinePlayer.player).thenReturn(onlinePlayer)
        `when`(playerService.getPlayer(offlinePlayer)).thenReturn(MfPlayer(mfPlayerId))

        uut = MfSlimefunProtectionPolicy(plugin)
    }

    @Test
    fun everythingIsAllowedWhenTheOptionIsOff() {
        `when`(config.getBoolean("slimefun.protection.enabled")).thenReturn(false)

        MfSlimefunAction.values().forEach { assertTrue(uut.isAllowed(offlinePlayer, location, it), it.name) }
        verify(claimService, never()).isInteractionAllowed(mfPlayerId, claim)
    }

    @Test
    fun wildernessIsNotRestricted() {
        `when`(claimService.getClaim(world, 2, -2)).thenReturn(null)

        MfSlimefunAction.values().forEach { assertTrue(uut.isAllowed(offlinePlayer, location, it), it.name) }
    }

    @Test
    fun aPlayerTheClaimRulesAllowMayDoAnything() {
        // Members, allies with alliesCanInteractWithLand and enemies with enableWartimeBlockDestruction all pass here
        `when`(claimService.isInteractionAllowed(mfPlayerId, claim)).thenReturn(true)

        MfSlimefunAction.values().forEach { assertTrue(uut.isAllowed(offlinePlayer, location, it), it.name) }
    }

    @Test
    fun anOutsiderCannotUseBreakOrPlaceInAClaim() {
        assertFalse(uut.isAllowed(offlinePlayer, location, MfSlimefunAction.INTERACT_BLOCK))
        assertFalse(uut.isAllowed(offlinePlayer, location, MfSlimefunAction.BREAK_BLOCK))
        assertFalse(uut.isAllowed(offlinePlayer, location, MfSlimefunAction.PLACE_BLOCK))
        assertFalse(uut.isAllowed(offlinePlayer, location, MfSlimefunAction.INTERACT_ENTITY))
    }

    @Test
    fun aPlayerMedievalFactionsHasNotSeenIsAnOutsider() {
        `when`(playerService.getPlayer(offlinePlayer)).thenReturn(null)

        assertFalse(uut.isAllowed(offlinePlayer, location, MfSlimefunAction.INTERACT_BLOCK))
    }

    @Test
    fun attackingAPlayerIsLeftToThePvpRules() {
        assertTrue(uut.isAllowed(offlinePlayer, location, MfSlimefunAction.ATTACK_PLAYER))
    }

    @Test
    fun attackingAMobFollowsTheMobProtectionFlag() {
        assertFalse(uut.isAllowed(offlinePlayer, location, MfSlimefunAction.ATTACK_ENTITY))

        `when`(claimFaction.flags).thenReturn(MfFlagValues(plugin, mapOf("enableMobProtection" to false)))
        assertTrue(uut.isAllowed(offlinePlayer, location, MfSlimefunAction.ATTACK_ENTITY))
    }

    @Test
    fun bypassNeedsBothTheToggleAndThePermission() {
        `when`(playerService.getPlayer(offlinePlayer)).thenReturn(MfPlayer(mfPlayerId, isBypassEnabled = true))
        assertFalse(uut.isAllowed(offlinePlayer, location, MfSlimefunAction.INTERACT_BLOCK))

        `when`(onlinePlayer.hasPermission("mf.bypass")).thenReturn(true)
        assertTrue(uut.isAllowed(offlinePlayer, location, MfSlimefunAction.INTERACT_BLOCK))

        `when`(playerService.getPlayer(offlinePlayer)).thenReturn(MfPlayer(mfPlayerId, isBypassEnabled = false))
        assertFalse(uut.isAllowed(offlinePlayer, location, MfSlimefunAction.INTERACT_BLOCK))
    }

    @Test
    fun wartimeListsApplyToSlimefunBlocksAsToOrdinaryBlocks() {
        `when`(claimService.isWartimeBreakableBlock(mfPlayerId, claim, Material.DISPENSER)).thenReturn(true)
        `when`(claimService.isWartimeInteractableBlock(mfPlayerId, claim, Material.DISPENSER)).thenReturn(true)

        assertTrue(uut.isAllowed(offlinePlayer, location, MfSlimefunAction.BREAK_BLOCK))
        assertTrue(uut.isAllowed(offlinePlayer, location, MfSlimefunAction.INTERACT_BLOCK))
        // There is no material to check a Slimefun placement against, so it stays refused
        assertFalse(uut.isAllowed(offlinePlayer, location, MfSlimefunAction.PLACE_BLOCK))
    }

    @Test
    fun nonMembersCanInteractWithDoorsCoversDoorsOnly() {
        `when`(config.getBoolean("factions.nonMembersCanInteractWithDoors")).thenReturn(true)
        assertFalse(uut.isAllowed(offlinePlayer, location, MfSlimefunAction.INTERACT_BLOCK))

        `when`(block.blockData).thenReturn(mock(Door::class.java))
        assertTrue(uut.isAllowed(offlinePlayer, location, MfSlimefunAction.INTERACT_BLOCK))

        `when`(config.getBoolean("factions.nonMembersCanInteractWithDoors")).thenReturn(false)
        assertFalse(uut.isAllowed(offlinePlayer, location, MfSlimefunAction.INTERACT_BLOCK))
    }

    @Test
    fun aLocationWithoutAWorldIsNotRestricted() {
        `when`(location.world).thenReturn(null)

        assertTrue(uut.isAllowed(offlinePlayer, location, MfSlimefunAction.BREAK_BLOCK))
    }

    // MfSlimefunProtectionModule

    @Test
    fun everySlimefunInteractionMapsToTheActionOfTheSameName() {
        Interaction.values().forEach { interaction ->
            assertEquals(interaction.name, MfSlimefunProtectionModule.toAction(interaction).name)
        }
    }

    @Test
    fun theModuleAsksThePolicyAndNamesMedievalFactionsAsItsPlugin() {
        val owningPlugin = mock(Plugin::class.java)
        val module: ProtectionModule = MfSlimefunProtectionModule(owningPlugin, uut)

        assertSame(owningPlugin, module.plugin)
        assertFalse(module.hasPermission(offlinePlayer, location, Interaction.BREAK_BLOCK))
        `when`(claimService.isInteractionAllowed(mfPlayerId, claim)).thenReturn(true)
        assertTrue(module.hasPermission(offlinePlayer, location, Interaction.BREAK_BLOCK))
    }
}
