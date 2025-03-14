
import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.claim.MfClaimService
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.gate.MfGateService
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.placeholder.MedievalFactionsPlaceholderExpansion
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerService
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipService
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType
import com.dansplugins.factionsystem.service.Services
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.*

class MedievalFactionsPlaceholderExpansionTest {

    private lateinit var plugin: MedievalFactions
    private lateinit var placeholderExpansion: MedievalFactionsPlaceholderExpansion
    private lateinit var language: Language

    @BeforeEach
    fun setUp() {
        // Mocking MedievalFactions plugin and Language
        plugin = mock(MedievalFactions::class.java)
        language = mock(Language::class.java)

        // Mock the plugin.name to avoid NullPointerException
        `when`(plugin.name).thenReturn("MedievalFactions")

        // Ensure language locale is non-null
        `when`(plugin.language).thenReturn(language)
        `when`(language.locale).thenReturn(Locale.ENGLISH)

        // Mock the services and its methods
        val services = mock(Services::class.java)
        `when`(plugin.services).thenReturn(services)

        // Initialize the placeholder expansion
        placeholderExpansion = MedievalFactionsPlaceholderExpansion(plugin)
    }

    @Test
    fun testGetIdentifier() {
        val identifier = placeholderExpansion.identifier
        assertEquals("MedievalFactions", identifier)
    }

    @Test
    fun testFactionName() {
        val player = mock(OfflinePlayer::class.java)
        val factionService = mock(MfFactionService::class.java)
        val playerService = mock(MfPlayerService::class.java)
        val faction = mock(MfFaction::class.java)
        val mfPlayer = mock(MfPlayer::class.java)

        `when`(plugin.services.factionService).thenReturn(factionService)
        `when`(plugin.services.playerService).thenReturn(playerService)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(factionService.getFaction(mfPlayer.id)).thenReturn(faction)
        `when`(faction.name).thenReturn("TestFaction")

        val result = placeholderExpansion.onRequest(player, "faction_name")
        assertEquals("TestFaction", result)
    }

    @Test
    fun testFactionPrefix() {
        val player = mock(OfflinePlayer::class.java)
        val factionService = mock(MfFactionService::class.java)
        val playerService = mock(MfPlayerService::class.java)
        val faction = mock(MfFaction::class.java)
        val mfPlayer = mock(MfPlayer::class.java)

        `when`(plugin.services.factionService).thenReturn(factionService)
        `when`(plugin.services.playerService).thenReturn(playerService)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(factionService.getFaction(mfPlayer.id)).thenReturn(faction)
        `when`(faction.prefix).thenReturn("TestPrefix")

        val result = placeholderExpansion.onRequest(player, "faction_prefix")
        assertEquals("TestPrefix", result)
    }

    @Test
    fun testFactionTotalClaimedChunks() {
        val player = mock(OfflinePlayer::class.java)
        val factionService = mock(MfFactionService::class.java)
        val playerService = mock(MfPlayerService::class.java)
        val claimService = mock(MfClaimService::class.java)
        val faction = mock(MfFaction::class.java)
        val mfPlayer = mock(MfPlayer::class.java)

        `when`(plugin.services.factionService).thenReturn(factionService)
        `when`(plugin.services.playerService).thenReturn(playerService)
        `when`(plugin.services.claimService).thenReturn(claimService)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(factionService.getFaction(mfPlayer.id)).thenReturn(faction)
        `when`(claimService.getClaims(faction.id)).thenReturn(listOf())

        val result = placeholderExpansion.onRequest(player, "faction_total_claimed_chunks")
        assertEquals("0", result)
    }

    @Test
    fun testFactionCumulativePower() {
        val player = mock(OfflinePlayer::class.java)
        val factionService = mock(MfFactionService::class.java)
        val playerService = mock(MfPlayerService::class.java)
        val faction = mock(MfFaction::class.java)
        val mfPlayer = mock(MfPlayer::class.java)

        `when`(plugin.services.factionService).thenReturn(factionService)
        `when`(plugin.services.playerService).thenReturn(playerService)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(factionService.getFaction(mfPlayer.id)).thenReturn(faction)
        `when`(faction.power).thenReturn(100.0)

        val result = placeholderExpansion.onRequest(player, "faction_cumulative_power")
        assertEquals("100", result)
    }

    @Test
    fun testFactionAllyCount() {
        val player = mock(OfflinePlayer::class.java)
        val factionService = mock(MfFactionService::class.java)
        val playerService = mock(MfPlayerService::class.java)
        val relationshipService = mock(MfFactionRelationshipService::class.java)
        val faction = mock(MfFaction::class.java)
        val mfPlayer = mock(MfPlayer::class.java)

        `when`(plugin.services.factionService).thenReturn(factionService)
        `when`(plugin.services.playerService).thenReturn(playerService)
        `when`(plugin.services.factionRelationshipService).thenReturn(relationshipService)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(factionService.getFaction(mfPlayer.id)).thenReturn(faction)
        `when`(factionService.factions).thenReturn(listOf(faction))
        `when`(relationshipService.getRelationships(faction.id, faction.id)).thenReturn(listOf())

        val result = placeholderExpansion.onRequest(player, "faction_ally_count")
        assertEquals("0", result)
    }

    @Test
    fun testFactionEnemyCount() {
        val player = mock(OfflinePlayer::class.java)
        val factionService = mock(MfFactionService::class.java)
        val playerService = mock(MfPlayerService::class.java)
        val relationshipService = mock(MfFactionRelationshipService::class.java)
        val faction = mock(MfFaction::class.java)
        val mfPlayer = mock(MfPlayer::class.java)

        `when`(plugin.services.factionService).thenReturn(factionService)
        `when`(plugin.services.playerService).thenReturn(playerService)
        `when`(plugin.services.factionRelationshipService).thenReturn(relationshipService)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(factionService.getFaction(mfPlayer.id)).thenReturn(faction)
        `when`(factionService.factions).thenReturn(listOf(faction))
        `when`(relationshipService.getRelationships(faction.id, faction.id)).thenReturn(listOf())

        val result = placeholderExpansion.onRequest(player, "faction_enemy_count")
        assertEquals("0", result)
    }

    @Test
    fun testFactionVassalCount() {
        val player = mock(OfflinePlayer::class.java)
        val factionService = mock(MfFactionService::class.java)
        val playerService = mock(MfPlayerService::class.java)
        val relationshipService = mock(MfFactionRelationshipService::class.java)
        val faction = mock(MfFaction::class.java)
        val mfPlayer = mock(MfPlayer::class.java)

        `when`(plugin.services.factionService).thenReturn(factionService)
        `when`(plugin.services.playerService).thenReturn(playerService)
        `when`(plugin.services.factionRelationshipService).thenReturn(relationshipService)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(factionService.getFaction(mfPlayer.id)).thenReturn(faction)
        `when`(factionService.factions).thenReturn(listOf(faction))
        `when`(relationshipService.getRelationships(faction.id, faction.id)).thenReturn(listOf())

        val result = placeholderExpansion.onRequest(player, "faction_vassal_count")
        assertEquals("0", result)
    }

    @Test
    fun testFactionGateCount() {
        val player = mock(OfflinePlayer::class.java)
        val factionService = mock(MfFactionService::class.java)
        val playerService = mock(MfPlayerService::class.java)
        val gateService = mock(MfGateService::class.java)
        val faction = mock(MfFaction::class.java)
        val mfPlayer = mock(MfPlayer::class.java)

        `when`(plugin.services.factionService).thenReturn(factionService)
        `when`(plugin.services.playerService).thenReturn(playerService)
        `when`(plugin.services.gateService).thenReturn(gateService)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(factionService.getFaction(mfPlayer.id)).thenReturn(faction)
        `when`(gateService.getGatesByFaction(faction.id)).thenReturn(listOf())

        val result = placeholderExpansion.onRequest(player, "faction_gate_count")
        assertEquals("0", result)
    }

    @Test
    fun testFactionLiege() {
        val player = mock(OfflinePlayer::class.java)
        val factionService = mock(MfFactionService::class.java)
        val playerService = mock(MfPlayerService::class.java)
        val relationshipService = mock(MfFactionRelationshipService::class.java)
        val faction = mock(MfFaction::class.java)
        val mfPlayer = mock(MfPlayer::class.java)

        `when`(plugin.services.factionService).thenReturn(factionService)
        `when`(plugin.services.playerService).thenReturn(playerService)
        `when`(plugin.services.factionRelationshipService).thenReturn(relationshipService)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(factionService.getFaction(mfPlayer.id)).thenReturn(faction)
        `when`(relationshipService.getRelationships(faction.id, MfFactionRelationshipType.LIEGE)).thenReturn(listOf())

        val result = placeholderExpansion.onRequest(player, "faction_liege")
        assertEquals("N/A", result)
    }

    @Test
    fun testFactionPopulation() {
        val player = mock(OfflinePlayer::class.java)
        val factionService = mock(MfFactionService::class.java)
        val playerService = mock(MfPlayerService::class.java)
        val faction = mock(MfFaction::class.java)
        val mfPlayer = mock(MfPlayer::class.java)

        `when`(plugin.services.factionService).thenReturn(factionService)
        `when`(plugin.services.playerService).thenReturn(playerService)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(factionService.getFaction(mfPlayer.id)).thenReturn(faction)
        `when`(faction.members).thenReturn(listOf())

        val result = placeholderExpansion.onRequest(player, "faction_population")
        assertEquals("0", result)
    }

    @Test
    fun testFactionRank() {
        val player = mock(OfflinePlayer::class.java)
        val factionService = mock(MfFactionService::class.java)
        val playerService = mock(MfPlayerService::class.java)
        val faction = mock(MfFaction::class.java)
        val mfPlayer = mock(MfPlayer::class.java)

        `when`(plugin.services.factionService).thenReturn(factionService)
        `when`(plugin.services.playerService).thenReturn(playerService)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(factionService.getFaction(mfPlayer.id)).thenReturn(faction)
        `when`(faction.getRole(mfPlayer.id)).thenReturn(null)

        val result = placeholderExpansion.onRequest(player, "faction_rank")
        assertEquals("N/A", result)
    }

    @Test
    fun testFactionPlayerPower() {
        val player = mock(OfflinePlayer::class.java)
        val playerService = mock(MfPlayerService::class.java)
        val mfPlayer = mock(MfPlayer::class.java)

        `when`(plugin.services.playerService).thenReturn(playerService)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(mfPlayer.power).thenReturn(10.0)

        val result = placeholderExpansion.onRequest(player, "faction_player_power")
        assertEquals("10", result)
    }

    @Test
    fun testPlayerLocation() {
        val player = mock(OfflinePlayer::class.java)
        val onlinePlayer = mock(Player::class.java)
        val location = mock(Location::class.java)

        `when`(player.player).thenReturn(onlinePlayer)
        `when`(onlinePlayer.location).thenReturn(location)
        `when`(location.blockX).thenReturn(0)
        `when`(location.blockY).thenReturn(0)
        `when`(location.blockZ).thenReturn(0)

        val result = placeholderExpansion.onRequest(player, "player_location")
        assertEquals("0:0:0", result)
    }

    // TODO: add unit tests for the following placeholders: faction_bonus_power, faction_power, faction_player_max_power, faction_player_power_full, faction_at_location, faction_color, player_chunk_location, player_world
}
