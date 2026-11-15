package com.dansplugins.factionsystem.command.faction

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.faction.flag.MfFlagValues
import com.dansplugins.factionsystem.faction.flag.MfFlags
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.relationship.MfFactionRelationship
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipId
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipRepository
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipService
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.ALLY
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.AT_WAR
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.LIEGE
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.VASSAL
import com.dansplugins.factionsystem.service.Services
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.Server
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.PluginManager
import org.bukkit.scheduler.BukkitScheduler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.logging.Logger

/**
 * Covers forced diplomacy (#1898): `/f <diplomacy command> <faction> <target>` with the command's `mf.force.*` node acts
 * for both factions at once, and arguments the normal command already accepts keep their normal meaning.
 */
class MfForcedDiplomacyTest {

    private lateinit var plugin: MedievalFactions
    private lateinit var factionService: MfFactionService
    private lateinit var relationshipService: MfFactionRelationshipService
    private val stored = mutableMapOf<MfFactionRelationshipId, MfFactionRelationship>()
    private lateinit var sender: CommandSender
    private lateinit var north: MfFaction
    private lateinit var south: MfFaction
    private lateinit var east: MfFaction
    private lateinit var uut: MfForcedDiplomacy

    @BeforeEach
    fun setUp() {
        plugin = mock(MedievalFactions::class.java)
        `when`(plugin.logger).thenReturn(mock(Logger::class.java))
        // Every message is its key followed by its parameters, so the tests can see which message was sent.
        val language = mock(Language::class.java) { invocation -> invocation.arguments.joinToString("|") }
        `when`(plugin.language).thenReturn(language)
        `when`(plugin.config).thenReturn(mock(FileConfiguration::class.java))
        val flags = MfFlags(plugin)
        `when`(plugin.flags).thenReturn(flags)

        val server = mock(Server::class.java)
        `when`(plugin.server).thenReturn(server)
        `when`(server.pluginManager).thenReturn(mock(PluginManager::class.java))
        `when`(server.scheduler).thenReturn(mock(BukkitScheduler::class.java))

        val services = mock(Services::class.java)
        `when`(plugin.services).thenReturn(services)
        factionService = mock(MfFactionService::class.java)
        `when`(services.factionService).thenReturn(factionService)
        relationshipService = MfFactionRelationshipService(plugin, InMemoryRelationshipRepository())
        `when`(services.factionRelationshipService).thenReturn(relationshipService)

        north = faction("North")
        south = faction("South")
        east = faction("East Kingdom")
        sender = mock(CommandSender::class.java)
        `when`(sender.name).thenReturn("Admin")
        uut = MfForcedDiplomacy(plugin)
    }

    // Parsing

    @Test
    fun parse_withoutTheForcePermissionIsNeverForced() {
        assertEquals(MfForcedDiplomacy.Parse.NotForced, uut.parse(sender, arrayOf("North", "South"), hasForcePermission = false))
    }

    @Test
    fun parse_aTargetTheNormalCommandAcceptsKeepsItsNormalMeaning() {
        // "East Kingdom" is one faction, so an op in a faction declaring war on it gets the normal command, as before
        assertEquals(MfForcedDiplomacy.Parse.NotForced, uut.parse(sender, arrayOf("East", "Kingdom"), hasForcePermission = true))
    }

    @Test
    fun parse_aTargetBeforeTheReasonSeparatorKeepsItsNormalMeaning() {
        assertEquals(MfForcedDiplomacy.Parse.NotForced, uut.parse(sender, arrayOf("East", "Kingdom", "--", "border", "raids"), hasForcePermission = true))
    }

    @Test
    fun parse_aSingleArgumentIsNotForced() {
        assertEquals(MfForcedDiplomacy.Parse.NotForced, uut.parse(sender, arrayOf("Nowhere"), hasForcePermission = true))
    }

    @Test
    fun parse_twoFactionsAreResolvedAndQuotedNamesAndIdsWork() {
        assertEquals(
            MfForcedDiplomacy.Parse.Resolved(north, east),
            uut.parse(sender, arrayOf("North", "\"East", "Kingdom\""), hasForcePermission = true)
        )
        assertEquals(
            MfForcedDiplomacy.Parse.Resolved(east, south),
            uut.parse(sender, arrayOf(east.id.value, "South"), hasForcePermission = true)
        )
        // An unquoted multi-word target after the first faction is joined, as the normal commands do
        assertEquals(
            MfForcedDiplomacy.Parse.Resolved(north, east),
            uut.parse(sender, arrayOf("North", "East", "Kingdom", "--", "reason"), hasForcePermission = true)
        )
    }

    @Test
    fun parse_anUnknownFactionIsRefused() {
        assertEquals(MfForcedDiplomacy.Parse.Refused, uut.parse(sender, arrayOf("Nowhere", "South"), hasForcePermission = true))
        verify(sender).sendMessage("${RED}CommandFactionForceInvalidFaction|Nowhere")
        assertEquals(MfForcedDiplomacy.Parse.Refused, uut.parse(sender, arrayOf("North", "Nowhere"), hasForcePermission = true))
        verify(sender).sendMessage("${RED}CommandFactionForceInvalidFaction|Nowhere")
    }

    @Test
    fun parse_aFactionWithItselfIsRefused() {
        assertEquals(MfForcedDiplomacy.Parse.Refused, uut.parse(sender, arrayOf("North", north.id.value), hasForcePermission = true))
        verify(sender).sendMessage("${RED}CommandFactionForceSameFaction")
    }

    // War and peace

    @Test
    fun declareWar_putsBothFactionsAtWarWithoutConsentOrApproval() {
        uut.declareWar(sender, north, south)

        assertEquals(setOf(rel(north, south, AT_WAR), rel(south, north, AT_WAR)), relationships())
        verify(sender).sendMessage("${GREEN}CommandFactionForceDeclareWarSuccess|North|South")
    }

    @Test
    fun declareWar_isRefusedBetweenAllies() {
        save(north, south, ALLY)
        save(south, north, ALLY)

        uut.declareWar(sender, north, south)

        assertEquals(setOf(rel(north, south, ALLY), rel(south, north, ALLY)), relationships())
        verify(sender).sendMessage("${RED}CommandFactionForceDeclareWarAllied|North|South")
    }

    @Test
    fun declareWar_isRefusedBetweenLiegeAndVassalInEitherOrder() {
        save(south, north, VASSAL)
        save(north, south, LIEGE)

        uut.declareWar(sender, north, south)

        assertEquals(2, relationships().size)
        verify(sender).sendMessage("${RED}CommandFactionForceDeclareWarVassal|North|South")
    }

    @Test
    fun declareWar_isRefusedAgainstANeutralFaction() {
        `when`(south.flags).thenReturn(MfFlagValues(plugin, mapOf("neutral" to true)))

        uut.declareWar(sender, north, south)

        assertEquals(emptySet<Triple<String, String, MfFactionRelationshipType>>(), relationships())
        verify(sender).sendMessage("${RED}CommandFactionForceDeclareWarNeutral|South|North")
    }

    @Test
    fun declareWar_completesAOneSidedWar() {
        save(north, south, AT_WAR)

        uut.declareWar(sender, north, south)

        assertEquals(setOf(rel(north, south, AT_WAR), rel(south, north, AT_WAR)), relationships())
    }

    @Test
    fun makePeace_endsTheWarForBothSidesAtOnce() {
        save(north, south, AT_WAR)
        save(south, north, AT_WAR)
        save(north, east, AT_WAR)

        uut.makePeace(sender, north, south)

        assertEquals(setOf(rel(north, east, AT_WAR)), relationships())
        verify(sender).sendMessage("${GREEN}CommandFactionForceMakePeaceSuccess|North|South")
    }

    @Test
    fun makePeace_isRefusedWhenNotAtWar() {
        uut.makePeace(sender, north, south)

        verify(sender).sendMessage("${RED}CommandFactionForceMakePeaceNotAtWar|North|South")
    }

    // Alliances

    @Test
    fun ally_formsTheAllianceForBothSidesAtOnce() {
        save(south, north, ALLY) // a pending request from South is completed, not duplicated

        uut.ally(sender, north, south)

        assertEquals(setOf(rel(north, south, ALLY), rel(south, north, ALLY)), relationships())
        assertEquals(2, stored.size)
        verify(sender).sendMessage("${GREEN}CommandFactionForceAllySuccess|North|South")
    }

    @Test
    fun ally_isRefusedWhileAtWar() {
        save(south, north, AT_WAR)

        uut.ally(sender, north, south)

        assertEquals(setOf(rel(south, north, AT_WAR)), relationships())
        verify(sender).sendMessage("${RED}CommandFactionForceAllyAtWar|North|South")
    }

    @Test
    fun breakAlliance_removesBothSides() {
        save(north, south, ALLY)
        save(south, north, ALLY)

        uut.breakAlliance(sender, north, south)

        assertEquals(emptySet<Triple<String, String, MfFactionRelationshipType>>(), relationships())
        verify(sender).sendMessage("${GREEN}CommandFactionForceBreakAllianceSuccess|North|South")
    }

    @Test
    fun breakAlliance_isRefusedWhenNotAllied() {
        uut.breakAlliance(sender, north, south)

        verify(sender).sendMessage("${RED}CommandFactionForceBreakAllianceNotAllies|North|South")
    }

    // Vassalage

    @Test
    fun vassalize_makesTheTargetAVassalAtOnce() {
        uut.vassalize(sender, north, south)

        assertEquals(setOf(rel(north, south, VASSAL), rel(south, north, LIEGE)), relationships())
        assertEquals(listOf(south.id), relationshipService.getVassals(north.id))
        verify(sender).sendMessage("${GREEN}CommandFactionForceVassalizeSuccess|North|South")
    }

    @Test
    fun vassalize_isRefusedWhenTheTargetAlreadyHasALiege() {
        save(east, south, VASSAL)
        save(south, east, LIEGE)

        uut.vassalize(sender, north, south)

        assertEquals(2, relationships().size)
        verify(sender).sendMessage("${RED}CommandFactionForceVassalizeHasLiege|South|East Kingdom")
    }

    @Test
    fun vassalize_isRefusedWhenItWouldCreateACycle() {
        // North is South's vassal, so South cannot become North's vassal
        save(south, north, VASSAL)
        save(north, south, LIEGE)

        uut.vassalize(sender, north, south)

        assertEquals(2, relationships().size)
        verify(sender).sendMessage("${RED}CommandFactionForceVassalizeWouldCreateCycle|North|South")
    }

    @Test
    fun grantIndependence_releasesTheVassal() {
        save(north, south, VASSAL)
        save(south, north, LIEGE)

        uut.grantIndependence(sender, north, south)

        assertEquals(emptySet<Triple<String, String, MfFactionRelationshipType>>(), relationships())
        verify(sender).sendMessage("${GREEN}CommandFactionForceGrantIndependenceSuccess|North|South")
    }

    @Test
    fun grantIndependence_isRefusedWhenTheTargetIsNotItsVassal() {
        save(north, south, VASSAL) // only a pending request

        uut.grantIndependence(sender, north, south)

        assertEquals(setOf(rel(north, south, VASSAL)), relationships())
        verify(sender).sendMessage("${RED}CommandFactionForceGrantIndependenceNotAVassal|North|South")
    }

    // Helpers

    private fun faction(name: String): MfFaction {
        val faction = mock(MfFaction::class.java)
        val id = MfFactionId.generate()
        `when`(faction.id).thenReturn(id)
        `when`(faction.name).thenReturn(name)
        `when`(faction.flags).thenReturn(MfFlagValues(plugin, emptyMap()))
        `when`(faction.members).thenReturn(emptyList())
        `when`(factionService.getFaction(name)).thenReturn(faction)
        `when`(factionService.getFaction(id)).thenReturn(faction)
        return faction
    }

    private fun save(faction: MfFaction, target: MfFaction, type: MfFactionRelationshipType) {
        relationshipService.save(MfFactionRelationship(factionId = faction.id, targetId = target.id, type = type))
    }

    private fun rel(faction: MfFaction, target: MfFaction, type: MfFactionRelationshipType) = Triple(faction.name, target.name, type)

    private fun relationships(): Set<Triple<String, String, MfFactionRelationshipType>> {
        val names = listOf(north, south, east).associate { it.id to it.name }
        return stored.values.map { Triple(names.getValue(it.factionId), names.getValue(it.targetId), it.type) }.toSet()
    }

    private inner class InMemoryRelationshipRepository : MfFactionRelationshipRepository {
        override fun getFactionRelationship(relationshipId: MfFactionRelationshipId) = stored[relationshipId]
        override fun getFactionRelationships(factionId: MfFactionId, targetId: MfFactionId) =
            stored.values.filter { it.factionId == factionId && it.targetId == targetId }
        override fun getFactionRelationships(factionId: MfFactionId, type: MfFactionRelationshipType) =
            stored.values.filter { it.factionId == factionId && it.type == type }
        override fun getFactionRelationships(factionId: MfFactionId) = stored.values.filter { it.factionId == factionId }
        override fun getFactionRelationships() = stored.values.toList()
        override fun upsert(relationship: MfFactionRelationship): MfFactionRelationship {
            stored[relationship.id] = relationship
            return relationship
        }
        override fun delete(relationshipId: MfFactionRelationshipId) {
            stored.remove(relationshipId)
        }
    }
}
