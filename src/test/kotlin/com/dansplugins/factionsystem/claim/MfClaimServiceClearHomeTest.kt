package com.dansplugins.factionsystem.claim

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfPosition
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.faction.flag.MfFlagValues
import com.dansplugins.factionsystem.faction.role.MfFactionRole
import com.dansplugins.factionsystem.faction.role.MfFactionRoles
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.locks.MfLockService
import com.dansplugins.factionsystem.service.Services
import dev.forkhandles.result4k.Success
import org.bukkit.Server
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.PluginManager
import org.bukkit.scheduler.BukkitScheduler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.RETURNS_DEFAULTS
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.UUID
import java.util.logging.Logger

/** Covers `factions.clearHomeOnChunkLoss`: a faction's home goes when the faction stops holding its chunk. */
class MfClaimServiceClearHomeTest {
    private val worldId = UUID.randomUUID()
    private val otherWorldId = UUID.randomUUID()

    private lateinit var plugin: MedievalFactions
    private lateinit var config: FileConfiguration
    private lateinit var repository: MfClaimedChunkRepository
    private val factions = mutableMapOf<MfFactionId, MfFaction>()
    private val saved = mutableListOf<MfFaction>()

    private lateinit var owner: MfFaction
    private lateinit var conqueror: MfFaction

    // Home at block (40.5, 64, -20.5): chunk (2, -2).
    private val homeChunk get() = MfClaimedChunk(worldId, 2, -2, owner.id)

    @BeforeEach
    fun setUp() {
        plugin = mock(MedievalFactions::class.java)
        config = mock(FileConfiguration::class.java)
        repository = mock(MfClaimedChunkRepository::class.java)
        `when`(plugin.config).thenReturn(config)
        `when`(plugin.logger).thenReturn(mock(Logger::class.java))
        val server = mock(Server::class.java)
        `when`(plugin.server).thenReturn(server)
        `when`(server.scheduler).thenReturn(mock(BukkitScheduler::class.java))
        `when`(server.pluginManager).thenReturn(mock(PluginManager::class.java))
        val language = mock(Language::class.java)
        `when`(language["FactionHomeClearedOnChunkLossNotificationTitle"]).thenReturn("Faction home lost")
        `when`(language["FactionHomeClearedOnChunkLossNotificationBody"]).thenReturn("Your faction's home was removed.")
        `when`(plugin.language).thenReturn(language)

        // MfFactionId is a value class, so the mangled JVM methods are answered by name rather than with matchers.
        val factionService = mock(MfFactionService::class.java) { invocation ->
            when (invocation.method.name) {
                "getFactionByFactionId" -> factions[MfFactionId(invocation.arguments[0] as String)]
                "updateFactionByFactionId" -> {
                    val id = MfFactionId(invocation.arguments[0] as String)

                    @Suppress("UNCHECKED_CAST")
                    val change = invocation.arguments[1] as (MfFaction) -> MfFaction
                    val updated = change(factions.getValue(id))
                    saved += updated
                    factions[id] = updated
                    Success(updated)
                }
                else -> RETURNS_DEFAULTS.answer(invocation)
            }
        }
        val services = mock(Services::class.java)
        `when`(services.factionService).thenReturn(factionService)
        `when`(services.lockService).thenReturn(mock(MfLockService::class.java))
        `when`(plugin.services).thenReturn(services)

        owner = faction("Owner", MfPosition(worldId, 40.5, 64.0, -20.5, 0f, 0f))
        conqueror = faction("Conqueror", null)
    }

    private fun faction(name: String, home: MfPosition?): MfFaction {
        val role = MfFactionRole(plugin, name = "Member")
        val faction = MfFaction(
            plugin = plugin,
            name = name,
            flags = MfFlagValues(plugin, emptyMap()),
            roles = MfFactionRoles(role.id, listOf(role)),
            defaultPermissionsByName = emptyMap(),
            home = home
        )
        factions[faction.id] = faction
        return faction
    }

    private fun service(vararg claims: MfClaimedChunk): MfClaimService {
        `when`(repository.getClaims()).thenReturn(claims.toList())
        return MfClaimService(plugin, repository)
    }

    private fun optionOn() {
        `when`(config.getBoolean("factions.clearHomeOnChunkLoss", false)).thenReturn(true)
    }

    private fun ownerHome() = factions.getValue(owner.id).home

    @Test
    fun `option off - unclaiming the home chunk keeps the home`() {
        service(homeChunk).delete(homeChunk)

        assertEquals(owner.home, ownerHome())
        assertTrue(saved.isEmpty())
    }

    @Test
    fun `option on - unclaiming the home chunk clears the home`() {
        optionOn()

        service(homeChunk).delete(homeChunk)

        assertNull(ownerHome())
        assertEquals(1, saved.size)
    }

    @Test
    fun `option on - another faction taking the home chunk clears the home`() {
        optionOn()
        val taken = homeChunk.copy(factionId = conqueror.id)
        `when`(repository.upsert(taken)).thenReturn(taken)

        service(homeChunk).save(taken)

        assertNull(ownerHome())
        assertNull(factions.getValue(conqueror.id).home)
    }

    @Test
    fun `option on - re-saving the home chunk for the same faction keeps the home`() {
        optionOn()
        `when`(repository.upsert(homeChunk)).thenReturn(homeChunk)

        service(homeChunk).save(homeChunk)

        assertEquals(owner.home, ownerHome())
        assertTrue(saved.isEmpty())
    }

    @Test
    fun `option on - unclaiming every chunk clears the home`() {
        optionOn()
        val other = MfClaimedChunk(worldId, 5, 5, owner.id)

        service(homeChunk, other).deleteAllClaims(owner.id)

        assertNull(ownerHome())
    }

    @Test
    fun `option on - disbanding removes claims without saving the faction`() {
        optionOn()

        service(homeChunk).deleteAllClaims(owner.id, clearHome = false)

        assertTrue(saved.isEmpty())
    }

    @Test
    fun `option on - losing a chunk without the home keeps the home`() {
        optionOn()
        val other = MfClaimedChunk(worldId, 3, -2, owner.id)

        service(homeChunk, other).delete(other)

        assertEquals(owner.home, ownerHome())
        assertTrue(saved.isEmpty())
    }

    @Test
    fun `option on - same chunk coordinates in another world keep the home`() {
        optionOn()
        val elsewhere = MfClaimedChunk(otherWorldId, 2, -2, owner.id)

        service(homeChunk, elsewhere).delete(elsewhere)

        assertEquals(owner.home, ownerHome())
    }

    @Test
    fun `option on - negative block coordinates map to the right chunk`() {
        optionOn()
        // Block z -20.5 lies in chunk -2, not -1 (integer division would give -1).
        val wrongChunk = MfClaimedChunk(worldId, 2, -1, owner.id)

        service(homeChunk, wrongChunk).delete(wrongChunk)

        assertEquals(owner.home, ownerHome())
    }
}
