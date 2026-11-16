package com.dansplugins.factionsystem.claim

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfChunkPosition
import com.dansplugins.factionsystem.faction.MfFactionId
import org.bukkit.Server
import org.bukkit.World
import org.bukkit.configuration.file.FileConfiguration
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.UUID
import java.util.logging.Logger

class MfClaimServiceRestrictionTest {
    private val worldId = UUID.randomUUID()
    private val otherWorldId = UUID.randomUUID()
    private val factionId = MfFactionId.generate()
    private val enemyId = MfFactionId.generate()

    private lateinit var plugin: MedievalFactions
    private lateinit var config: FileConfiguration
    private lateinit var repository: MfClaimedChunkRepository

    @BeforeEach
    fun setUp() {
        plugin = mock(MedievalFactions::class.java)
        config = mock(FileConfiguration::class.java)
        repository = mock(MfClaimedChunkRepository::class.java)
        `when`(plugin.config).thenReturn(config)
        `when`(plugin.logger).thenReturn(mock(Logger::class.java))
        val server = mock(Server::class.java)
        `when`(plugin.server).thenReturn(server)
        val world = mock(World::class.java)
        `when`(world.name).thenReturn("world")
        `when`(server.getWorld(worldId)).thenReturn(world)
        `when`(config.getInt("factions.maxClaimDistance", -1)).thenReturn(-1)
    }

    private fun service(vararg claims: MfClaimedChunk): MfClaimService {
        `when`(repository.getClaims()).thenReturn(claims.toList())
        return MfClaimService(plugin, repository)
    }

    @Test
    fun noChunkIsBlockedByDefault() {
        `when`(config.getStringList("factions.blockedClaimChunks")).thenReturn(emptyList())

        assertFalse(service().isChunkClaimBlocked("world", 0, 0))
    }

    @Test
    fun singleChunkEntryBlocksOnlyThatChunk() {
        `when`(config.getStringList("factions.blockedClaimChunks")).thenReturn(listOf("world:3:-4"))
        val uut = service()

        assertTrue(uut.isChunkClaimBlocked("world", 3, -4))
        assertFalse(uut.isChunkClaimBlocked("world", 3, -3))
        assertFalse(uut.isChunkClaimBlocked("world_nether", 3, -4))
    }

    @Test
    fun rectangleEntryBlocksEveryChunkInsideItWhicheverCornerComesFirst() {
        `when`(config.getStringList("factions.blockedClaimChunks")).thenReturn(listOf("world:1:1:-2:-2"))
        val uut = service()

        assertTrue(uut.isChunkClaimBlocked("world", -2, -2))
        assertTrue(uut.isChunkClaimBlocked("world", 0, 1))
        assertTrue(uut.isChunkClaimBlocked("world", 1, 1))
        assertFalse(uut.isChunkClaimBlocked("world", 2, 0))
    }

    @Test
    fun malformedEntriesAreIgnored() {
        `when`(config.getStringList("factions.blockedClaimChunks")).thenReturn(listOf("world", "world:a:b", "world:1:2:3"))

        assertFalse(service().isChunkClaimBlocked("world", 1, 2))
    }

    @Test
    fun chunkPositionIsMatchedByItsWorldName() {
        `when`(config.getStringList("factions.blockedClaimChunks")).thenReturn(listOf("world:0:0"))
        val uut = service()

        assertTrue(uut.isChunkClaimBlocked(MfChunkPosition(worldId, 0, 0)))
        // A world the server does not know cannot match a name
        assertFalse(uut.isChunkClaimBlocked(MfChunkPosition(otherWorldId, 0, 0)))
    }

    @Test
    fun claimDistanceIsUnlimitedByDefault() {
        val uut = service(MfClaimedChunk(worldId, 0, 0, factionId))

        assertFalse(uut.exceedsMaxClaimDistance(factionId, MfChunkPosition(worldId, 1000, 1000)))
    }

    @Test
    fun claimWithinTheDistanceIsAllowedAndOneBeyondItIsNot() {
        `when`(config.getInt("factions.maxClaimDistance", -1)).thenReturn(5)
        val uut = service(MfClaimedChunk(worldId, 0, 0, factionId))

        assertFalse(uut.exceedsMaxClaimDistance(factionId, MfChunkPosition(worldId, 3, 4)))
        assertTrue(uut.exceedsMaxClaimDistance(factionId, MfChunkPosition(worldId, 4, 4)))
    }

    @Test
    fun anotherFactionsClaimsDoNotCountTowardsTheDistance() {
        `when`(config.getInt("factions.maxClaimDistance", -1)).thenReturn(5)
        val uut = service(MfClaimedChunk(worldId, 0, 0, factionId), MfClaimedChunk(worldId, 20, 20, enemyId))

        assertTrue(uut.exceedsMaxClaimDistance(factionId, MfChunkPosition(worldId, 21, 21)))
    }

    @Test
    fun firstClaimInAWorldIsNotLimited() {
        `when`(config.getInt("factions.maxClaimDistance", -1)).thenReturn(5)
        val uut = service(MfClaimedChunk(worldId, 0, 0, factionId))

        assertFalse(uut.exceedsMaxClaimDistance(factionId, MfChunkPosition(otherWorldId, 500, 500)))
    }

    @Test
    fun multiChunkClaimIsAllowedIfAnyChunkIsCloseEnough() {
        `when`(config.getInt("factions.maxClaimDistance", -1)).thenReturn(2)
        val uut = service(MfClaimedChunk(worldId, 0, 0, factionId))

        assertFalse(uut.exceedsMaxClaimDistance(factionId, MfChunkPosition(worldId, 2, 0), MfChunkPosition(worldId, 9, 0)))
    }

    @Test
    fun surroundedChunkCanBeConqueredWhenTheOptionIsOff() {
        val uut = service(*surroundedByEnemy())

        assertFalse(uut.isConquestBlockedBySurroundingClaims(MfChunkPosition(worldId, 0, 0), enemyId))
    }

    @Test
    fun surroundedChunkCannotBeConqueredWhenTheOptionIsOn() {
        `when`(config.getBoolean("factions.preventConqueringSurroundedChunks")).thenReturn(true)
        val uut = service(*surroundedByEnemy())

        assertTrue(uut.isConquestBlockedBySurroundingClaims(MfChunkPosition(worldId, 0, 0), enemyId))
    }

    @Test
    fun borderChunkCanBeConqueredWhenTheOptionIsOn() {
        `when`(config.getBoolean("factions.preventConqueringSurroundedChunks")).thenReturn(true)
        // The eastern neighbour belongs to the attacker, so the chunk is on the edge of the enemy's territory
        val claims = surroundedByEnemy().filterNot { it.x == 1 && it.z == 0 } + MfClaimedChunk(worldId, 1, 0, factionId)
        val uut = service(*claims.toTypedArray())

        assertFalse(uut.isConquestBlockedBySurroundingClaims(MfChunkPosition(worldId, 0, 0), enemyId))
    }

    private fun surroundedByEnemy() = arrayOf(
        MfClaimedChunk(worldId, 0, 0, enemyId),
        MfClaimedChunk(worldId, -1, 0, enemyId),
        MfClaimedChunk(worldId, 1, 0, enemyId),
        MfClaimedChunk(worldId, 0, -1, enemyId),
        MfClaimedChunk(worldId, 0, 1, enemyId)
    )
}
