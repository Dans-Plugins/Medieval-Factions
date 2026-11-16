package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.claim.MfClaimService
import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.faction.flag.MfFlagValues
import com.dansplugins.factionsystem.faction.flag.MfFlags
import com.dansplugins.factionsystem.service.Services
import org.bukkit.Chunk
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Enderman
import org.bukkit.entity.FallingBlock
import org.bukkit.entity.Player
import org.bukkit.entity.Zombie
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityInteractEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID

/** Covers the `allowMobGriefing` faction flag (#1614). */
class MobGriefingListenerTest {

    private lateinit var plugin: MedievalFactions
    private lateinit var claimService: MfClaimService
    private lateinit var flags: MfFlags
    private lateinit var faction: MfFaction
    private lateinit var block: Block
    private lateinit var chunk: Chunk
    private lateinit var uut: MobGriefingListener

    @BeforeEach
    fun setUp() {
        plugin = mock(MedievalFactions::class.java)
        `when`(plugin.config).thenReturn(mock(FileConfiguration::class.java))
        flags = MfFlags(plugin)
        `when`(plugin.flags).thenReturn(flags)
        claimService = mock(MfClaimService::class.java)
        val factionService = mock(MfFactionService::class.java)
        val services = mock(Services::class.java)
        `when`(services.claimService).thenReturn(claimService)
        `when`(services.factionService).thenReturn(factionService)
        `when`(plugin.services).thenReturn(services)

        val factionId = MfFactionId("faction")
        faction = mock(MfFaction::class.java)
        `when`(factionService.getFaction(factionId)).thenReturn(faction)
        block = mock(Block::class.java)
        chunk = mock(Chunk::class.java)
        `when`(block.chunk).thenReturn(chunk)
        val claim = MfClaimedChunk(UUID.randomUUID(), 0, 0, factionId)
        `when`(claimService.getClaim(chunk)).thenReturn(claim)
        uut = MobGriefingListener(plugin)
    }

    @Test
    fun `mobs change blocks in a claim whose faction allows mob griefing`() {
        setAllowMobGriefing(true)
        val event = changeBlock(mock(Enderman::class.java))

        uut.onEntityChangeBlock(event)

        verify(event, never()).isCancelled = true
    }

    @Test
    fun `an enderman cannot take a block from a claim whose faction disallows mob griefing`() {
        setAllowMobGriefing(false)
        val event = changeBlock(mock(Enderman::class.java))

        uut.onEntityChangeBlock(event)

        verify(event).isCancelled = true
    }

    @Test
    fun `a zombie cannot break a door in a claim whose faction disallows mob griefing`() {
        setAllowMobGriefing(false)
        val event = changeBlock(mock(Zombie::class.java))

        uut.onEntityChangeBlock(event)

        verify(event).isCancelled = true
    }

    @Test
    fun `falling blocks and players are not mobs`() {
        setAllowMobGriefing(false)
        val falling = changeBlock(mock(FallingBlock::class.java))
        val player = changeBlock(mock(Player::class.java))

        uut.onEntityChangeBlock(falling)
        uut.onEntityChangeBlock(player)

        verify(falling, never()).isCancelled = true
        verify(player, never()).isCancelled = true
    }

    @Test
    fun `wilderness is not affected`() {
        setAllowMobGriefing(false)
        `when`(claimService.getClaim(chunk)).thenReturn(null)
        val event = changeBlock(mock(Enderman::class.java))

        uut.onEntityChangeBlock(event)

        verify(event, never()).isCancelled = true
    }

    @Test
    fun `a mob cannot trample farmland in a claim whose faction disallows mob griefing`() {
        setAllowMobGriefing(false)
        `when`(block.type).thenReturn(Material.FARMLAND)
        val event = mock(EntityInteractEvent::class.java)
        val zombie = mock(Zombie::class.java)
        `when`(event.entity).thenReturn(zombie)
        `when`(event.block).thenReturn(block)

        uut.onEntityInteract(event)

        verify(event).isCancelled = true
    }

    @Test
    fun `a mob pressing a plate is not mob griefing`() {
        setAllowMobGriefing(false)
        `when`(block.type).thenReturn(Material.STONE_PRESSURE_PLATE)
        val event = mock(EntityInteractEvent::class.java)
        val zombie = mock(Zombie::class.java)
        `when`(event.entity).thenReturn(zombie)
        `when`(event.block).thenReturn(block)

        uut.onEntityInteract(event)

        verify(event, never()).isCancelled = true
    }

    private fun setAllowMobGriefing(value: Boolean) {
        val values = MfFlagValues(plugin, mapOf("allowMobGriefing" to value))
        `when`(faction.flags).thenReturn(values)
    }

    private fun changeBlock(entity: org.bukkit.entity.Entity): EntityChangeBlockEvent {
        val event = mock(EntityChangeBlockEvent::class.java)
        `when`(event.entity).thenReturn(entity)
        `when`(event.block).thenReturn(block)
        return event
    }
}
