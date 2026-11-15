package com.dansplugins.factionsystem.claim

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfChunkPosition
import com.dansplugins.factionsystem.faction.MfFactionId
import org.bukkit.Server
import org.bukkit.World
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.UUID
import java.util.logging.Logger

class MfClaimServicePowerCostTest {
    private val overworldId = UUID.randomUUID()
    private val netherId = UUID.randomUUID()
    private val factionId = MfFactionId.generate()

    private lateinit var config: FileConfiguration
    private lateinit var uut: MfClaimService

    @BeforeEach
    fun setUp() {
        val plugin = mock(MedievalFactions::class.java)
        config = mock(FileConfiguration::class.java)
        `when`(plugin.config).thenReturn(config)
        `when`(plugin.logger).thenReturn(mock(Logger::class.java))
        val server = mock(Server::class.java)
        `when`(plugin.server).thenReturn(server)
        val overworld = mock(World::class.java)
        `when`(overworld.name).thenReturn("world")
        `when`(server.getWorld(overworldId)).thenReturn(overworld)
        val nether = mock(World::class.java)
        `when`(nether.name).thenReturn("world_nether")
        `when`(server.getWorld(netherId)).thenReturn(nether)
        `when`(config.getDouble("factions.claimPowerCost", 1.0)).thenReturn(1.0)
        val repository = mock(MfClaimedChunkRepository::class.java)
        `when`(repository.getClaims()).thenReturn(
            listOf(
                MfClaimedChunk(overworldId, 0, 0, factionId),
                MfClaimedChunk(overworldId, 0, 1, factionId),
                MfClaimedChunk(netherId, 0, 0, factionId)
            )
        )
        uut = MfClaimService(plugin, repository)
    }

    private fun stubWorldCosts(costs: Map<String, Any>) {
        val section = mock(ConfigurationSection::class.java)
        `when`(section.getValues(false)).thenReturn(costs)
        `when`(config.getConfigurationSection("factions.claimPowerCostByWorld")).thenReturn(section)
    }

    @Test
    fun defaultCostIsOnePowerPerChunk() {
        assertEquals(1.0, uut.getClaimPowerCost(overworldId))
        assertEquals(3.0, uut.getClaimPowerUsed(factionId))
    }

    @Test
    fun defaultClaimLimitIsThePowerRoundedDown() {
        assertEquals(7.0, uut.getClaimLimit(factionId, 7.9, overworldId))
    }

    @Test
    fun globalCostAppliesToEveryWorld() {
        `when`(config.getDouble("factions.claimPowerCost", 1.0)).thenReturn(2.0)

        assertEquals(6.0, uut.getClaimPowerUsed(factionId))
        assertEquals(4.0, uut.getClaimPowerCost(listOf(MfChunkPosition(overworldId, 5, 5), MfChunkPosition(netherId, 5, 5))))
    }

    @Test
    fun worldOverrideAppliesOnlyToThatWorld() {
        stubWorldCosts(mapOf("world_nether" to 4))

        assertEquals(1.0, uut.getClaimPowerCost(overworldId))
        assertEquals(4.0, uut.getClaimPowerCost(netherId))
        assertEquals(6.0, uut.getClaimPowerUsed(factionId))
    }

    @Test
    fun claimLimitCountsRemainingPowerAtTheWorldsCost() {
        stubWorldCosts(mapOf("world_nether" to 4.0))

        // 10 power, 6 used: 4 left buys 4 overworld chunks (7 in all) or 1 nether chunk (4 in all)
        assertEquals(7.0, uut.getClaimLimit(factionId, 10.0, overworldId))
        assertEquals(4.0, uut.getClaimLimit(factionId, 10.0, netherId))
    }

    @Test
    fun negativeCostIsTreatedAsFree() {
        `when`(config.getDouble("factions.claimPowerCost", 1.0)).thenReturn(-3.0)

        assertEquals(0.0, uut.getClaimPowerUsed(factionId))
    }
}
