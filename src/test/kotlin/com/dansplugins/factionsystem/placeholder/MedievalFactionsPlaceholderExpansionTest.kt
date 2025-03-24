package com.dansplugins.factionsystem.placeholder

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.claim.MfClaimService
import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionMember
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.gate.MfGate
import com.dansplugins.factionsystem.gate.MfGateService
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerService
import com.dansplugins.factionsystem.relationship.MfFactionRelationship
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipService
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType
import com.dansplugins.factionsystem.service.Services
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MedievalFactionsPlaceholderExpansionTest {

    private lateinit var fixture: PlaceholderExpansionTestFixture
    private lateinit var plugin: MedievalFactions
    private lateinit var factionService: MfFactionService
    private lateinit var playerService: MfPlayerService
    private lateinit var gateService: MfGateService
    private lateinit var claimService: MfClaimService
    private lateinit var relationshipService: MfFactionRelationshipService
    private lateinit var language: Language
    private lateinit var uut: MedievalFactionsPlaceholderExpansion

    @BeforeEach
    fun setUp() {
        fixture = createFixture()
        plugin = mock(MedievalFactions::class.java)
        `when`(plugin.name).thenReturn("MedievalFactions")
        mockServices()
        mockLanguageSystem()
        uut = MedievalFactionsPlaceholderExpansion(plugin)
    }

    @Test
    fun testGetIdentifier() {
        val identifier = uut.identifier
        assertEquals("MedievalFactions", identifier)
    }

    @Test
    fun testFactionName() {
        val player = fixture.player
        val faction = fixture.faction
        val mfPlayer = fixture.mfPlayer
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(factionService.getFaction(mfPlayer.id)).thenReturn(faction)
        `when`(faction.name).thenReturn("TestFaction")

        val result = uut.onRequest(player, "faction_name")
        assertEquals("TestFaction", result)
    }

    @Test
    fun testFactionPrefix() {
        val player = fixture.player
        val faction = fixture.faction
        val mfPlayer = fixture.mfPlayer
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(factionService.getFaction(mfPlayer.id)).thenReturn(faction)
        `when`(faction.prefix).thenReturn("TestPrefix")

        val result = uut.onRequest(player, "faction_prefix")
        assertEquals("TestPrefix", result)
    }

    @Test
    fun testFactionTotalClaimedChunks_ZeroChunks() {
        val player = fixture.player
        val faction = fixture.faction
        val mfPlayer = fixture.mfPlayer
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(factionService.getFaction(mfPlayer.id)).thenReturn(faction)
        `when`(claimService.getClaims(faction.id)).thenReturn(listOf())

        val result = uut.onRequest(player, "faction_total_claimed_chunks")
        assertEquals("0", result)
    }

    @Test
    fun testFactionTotalClaimedChunks_NonZeroChunks() {
        val player = fixture.player
        val faction = fixture.faction
        val mfPlayer = fixture.mfPlayer
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(factionService.getFaction(mfPlayer.id)).thenReturn(faction)
        `when`(claimService.getClaims(faction.id)).thenReturn(listOf(mock(MfClaimedChunk::class.java)))

        val result = uut.onRequest(player, "faction_total_claimed_chunks")
        assertEquals("1", result)
    }

    @Test
    fun testFactionCumulativePower() {
        val player = fixture.player
        val faction = fixture.faction
        val mfPlayer = fixture.mfPlayer
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(factionService.getFaction(mfPlayer.id)).thenReturn(faction)
        `when`(faction.power).thenReturn(100.0)

        val result = uut.onRequest(player, "faction_cumulative_power")
        assertEquals("100", result)
    }

    @Test
    fun testFactionAllyCount_ZeroAllies() {
        val player = fixture.player
        val faction = fixture.faction
        val mfPlayer = fixture.mfPlayer
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(factionService.getFaction(mfPlayer.id)).thenReturn(faction)
        `when`(factionService.factions).thenReturn(listOf(faction))
        `when`(relationshipService.getRelationships(faction.id, faction.id)).thenReturn(listOf())

        val result = uut.onRequest(player, "faction_ally_count")
        assertEquals("0", result)
    }

    @Test
    fun testFactionAllyCount_NonZeroAllies() {
        val player = fixture.player
        val faction = fixture.faction
        val mfPlayer = fixture.mfPlayer
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(factionService.getFaction(mfPlayer.id)).thenReturn(faction)
        `when`(factionService.factions).thenReturn(listOf(faction))
        val allyRelationship = mock(MfFactionRelationship::class.java)
        `when`(allyRelationship.type).thenReturn(MfFactionRelationshipType.ALLY)
        `when`(relationshipService.getRelationships(faction.id, faction.id)).thenReturn(listOf(allyRelationship))

        val result = uut.onRequest(player, "faction_ally_count")
        assertEquals("1", result)
    }

    @Test
    fun testFactionEnemyCount_ZeroEnemies() {
        val player = fixture.player
        val faction = fixture.faction
        val mfPlayer = fixture.mfPlayer
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(factionService.getFaction(mfPlayer.id)).thenReturn(faction)
        `when`(factionService.factions).thenReturn(listOf(faction))
        `when`(relationshipService.getRelationships(faction.id, faction.id)).thenReturn(listOf())

        val result = uut.onRequest(player, "faction_enemy_count")
        assertEquals("0", result)
    }

    @Test
    fun testFactionEnemyCount_NonZeroEnemies() {
        val player = fixture.player
        val faction = fixture.faction
        val mfPlayer = fixture.mfPlayer
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(factionService.getFaction(mfPlayer.id)).thenReturn(faction)
        `when`(factionService.factions).thenReturn(listOf(faction))
        val enemyRelationship = mock(MfFactionRelationship::class.java)
        `when`(enemyRelationship.type).thenReturn(MfFactionRelationshipType.AT_WAR)
        `when`(relationshipService.getRelationships(faction.id, faction.id)).thenReturn(listOf(enemyRelationship))

        val result = uut.onRequest(player, "faction_enemy_count")
        assertEquals("1", result)
    }

    @Test
    fun testFactionVassalCount_ZeroVassals() {
        val player = fixture.player
        val faction = fixture.faction
        val mfPlayer = fixture.mfPlayer
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(factionService.getFaction(mfPlayer.id)).thenReturn(faction)
        `when`(factionService.factions).thenReturn(listOf(faction))
        `when`(relationshipService.getRelationships(faction.id, faction.id)).thenReturn(listOf())

        val result = uut.onRequest(player, "faction_vassal_count")
        assertEquals("0", result)
    }

    @Test
    fun testFactionVassalCount_NonZeroVassals() {
        val player = fixture.player
        val faction = fixture.faction
        val mfPlayer = fixture.mfPlayer
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(factionService.getFaction(mfPlayer.id)).thenReturn(faction)
        `when`(factionService.factions).thenReturn(listOf(faction))
        val vassalRelationship = mock(MfFactionRelationship::class.java)
        `when`(vassalRelationship.type).thenReturn(MfFactionRelationshipType.VASSAL)
        `when`(relationshipService.getRelationships(faction.id, faction.id)).thenReturn(listOf(vassalRelationship))

        val result = uut.onRequest(player, "faction_vassal_count")
        assertEquals("0", result)
    }

    @Test
    fun testFactionGateCount_ZeroGates() {
        val player = fixture.player
        val faction = fixture.faction
        val mfPlayer = fixture.mfPlayer
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(factionService.getFaction(mfPlayer.id)).thenReturn(faction)
        `when`(gateService.getGatesByFaction(faction.id)).thenReturn(listOf())

        val result = uut.onRequest(player, "faction_gate_count")
        assertEquals("0", result)
    }

    @Test
    fun testFactionGateCount_NonZeroGates() {
        val player = fixture.player
        val faction = fixture.faction
        val mfPlayer = fixture.mfPlayer
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(factionService.getFaction(mfPlayer.id)).thenReturn(faction)
        val gate = mock(MfGate::class.java)
        `when`(gateService.getGatesByFaction(faction.id)).thenReturn(listOf(gate))

        val result = uut.onRequest(player, "faction_gate_count")
        assertEquals("1", result)
    }

    @Test
    fun testFactionLiege() {
        val player = fixture.player
        val faction = fixture.faction
        val mfPlayer = fixture.mfPlayer
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(factionService.getFaction(mfPlayer.id)).thenReturn(faction)
        `when`(relationshipService.getRelationships(faction.id, MfFactionRelationshipType.LIEGE)).thenReturn(listOf())

        val result = uut.onRequest(player, "faction_liege")
        assertEquals("N/A", result)
    }

    @Test
    fun testFactionPopulation_ZeroPop() {
        val player = fixture.player
        val faction = fixture.faction
        val mfPlayer = fixture.mfPlayer
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(factionService.getFaction(mfPlayer.id)).thenReturn(faction)
        `when`(faction.members).thenReturn(listOf())

        val result = uut.onRequest(player, "faction_population")
        assertEquals("0", result)
    }

    @Test
    fun testFactionPopulation_NonZeroPop() {
        val player = fixture.player
        val faction = fixture.faction
        val mfPlayer = fixture.mfPlayer
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(factionService.getFaction(mfPlayer.id)).thenReturn(faction)
        val factionMember = mock(MfFactionMember::class.java)
        `when`(faction.members).thenReturn(listOf(factionMember))

        val result = uut.onRequest(player, "faction_population")
        assertEquals("1", result)
    }

    @Test
    fun testFactionRank() {
        val player = fixture.player
        val faction = fixture.faction
        val mfPlayer = fixture.mfPlayer
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(factionService.getFaction(mfPlayer.id)).thenReturn(faction)
        `when`(faction.getRole(mfPlayer.id)).thenReturn(null)

        val result = uut.onRequest(player, "faction_rank")
        assertEquals("N/A", result)
    }

    @Test
    fun testFactionPlayerPower() {
        val player = fixture.player
        val mfPlayer = fixture.mfPlayer
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(mfPlayer.power).thenReturn(10.0)

        val result = uut.onRequest(player, "faction_player_power")
        assertEquals("10", result)
    }

    @Test
    fun testPlayerLocation() {
        val player = fixture.player
        val onlinePlayer = mock(Player::class.java)
        val location = mock(Location::class.java)

        `when`(player.player).thenReturn(onlinePlayer)
        `when`(onlinePlayer.location).thenReturn(location)
        `when`(location.blockX).thenReturn(0)
        `when`(location.blockY).thenReturn(0)
        `when`(location.blockZ).thenReturn(0)

        val result = uut.onRequest(player, "player_location")
        assertEquals("0:0:0", result)
    }

    // TODO: add unit tests for the following placeholders: faction_bonus_power, faction_power, faction_player_max_power, faction_player_power_full, faction_at_location, faction_color, player_chunk_location, player_world

    // Helper functions

    private fun createFixture(): PlaceholderExpansionTestFixture {
        val player = mock(OfflinePlayer::class.java)
        val faction = mock(MfFaction::class.java)
        val mfPlayer = mock(MfPlayer::class.java)
        return PlaceholderExpansionTestFixture(player, faction, mfPlayer)
    }

    private data class PlaceholderExpansionTestFixture(
        val player: OfflinePlayer,
        val faction: MfFaction,
        val mfPlayer: MfPlayer
    )

    private fun mockServices() {
        val services = mock(Services::class.java)
        `when`(plugin.services).thenReturn(services)

        factionService = mock(MfFactionService::class.java)
        `when`(services.factionService).thenReturn(factionService)

        playerService = mock(MfPlayerService::class.java)
        `when`(services.playerService).thenReturn(playerService)

        gateService = mock(MfGateService::class.java)
        `when`(services.gateService).thenReturn(gateService)

        claimService = mock(MfClaimService::class.java)
        `when`(services.claimService).thenReturn(claimService)

        relationshipService = mock(MfFactionRelationshipService::class.java)
        `when`(services.factionRelationshipService).thenReturn(relationshipService)
    }

    private fun mockLanguageSystem() {
        language = mock(Language::class.java)
        `when`(plugin.language).thenReturn(language)
        `when`(language.locale).thenReturn(Locale.ENGLISH)
    }
}
