package com.dansplugins.factionsystem.map.builders

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.claim.MfClaimService
import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.MfFactionMember
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.faction.role.MfFactionRole
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.player.MfPlayerService
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipService
import com.dansplugins.factionsystem.service.Services
import org.bukkit.configuration.file.FileConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.Locale

class FactionInfoBuilderTest {

    private lateinit var plugin: MedievalFactions
    private lateinit var config: FileConfiguration
    private lateinit var language: Language
    private lateinit var factionService: MfFactionService
    private lateinit var relationshipService: MfFactionRelationshipService
    private lateinit var claimService: MfClaimService
    private lateinit var playerService: MfPlayerService
    private lateinit var faction: MfFaction
    private lateinit var uut: FactionInfoBuilder

    private val factionId = MfFactionId("faction-id")

    @BeforeEach
    fun setUp() {
        plugin = mock(MedievalFactions::class.java)

        // Every dynmap.show* key reads as false unless a test turns it on.
        config = mock(FileConfiguration::class.java)
        `when`(plugin.config).thenReturn(config)

        language = mock(Language::class.java)
        `when`(plugin.language).thenReturn(language)
        `when`(language.locale).thenReturn(Locale.ENGLISH)

        val services = mock(Services::class.java)
        `when`(plugin.services).thenReturn(services)
        factionService = mock(MfFactionService::class.java)
        `when`(services.factionService).thenReturn(factionService)
        relationshipService = mock(MfFactionRelationshipService::class.java)
        `when`(services.factionRelationshipService).thenReturn(relationshipService)
        claimService = mock(MfClaimService::class.java)
        `when`(services.claimService).thenReturn(claimService)
        playerService = mock(MfPlayerService::class.java)
        `when`(services.playerService).thenReturn(playerService)

        faction = mock(MfFaction::class.java)
        `when`(faction.id).thenReturn(factionId)
        `when`(faction.name).thenReturn("Avalon")

        // The decimal format is built from the language locale when the builder is constructed.
        uut = FactionInfoBuilder(plugin)
    }

    @Test
    fun `build with every section off shows only the faction name`() {
        assertEquals("<h1>Avalon</h1>", uut.build(faction))
    }

    @Test
    fun `build shows the description when showDescription is on`() {
        show("showDescription")
        `when`(faction.description).thenReturn("A peaceful realm")

        assertEquals("<h1>Avalon</h1><h2>Description</h2>A peaceful realm", uut.build(faction))
    }

    @Test
    fun `build groups members by role with a count per role`() {
        show("showMembers")
        val owner = MfFactionRole(plugin, name = "Owner")
        val member = MfFactionRole(plugin, name = "Member")
        val alice = player("alice", "Alice")
        val bob = player("bob", "Bob")
        val carol = player("carol", "Carol")
        `when`(faction.members).thenReturn(
            listOf(
                MfFactionMember(alice.id, owner),
                MfFactionMember(bob.id, member),
                MfFactionMember(carol.id, member)
            )
        )

        val result = uut.build(faction)

        assertEquals(
            "<h1>Avalon</h1><h2>Members (3)</h2>" +
                "<h3>Owner (1)</h3>\nAlice" +
                "<br />" +
                "<h3>Member (2)</h3>\nBob, Carol",
            result
        )
    }

    @Test
    fun `build names a member with no player record with the UnknownPlayer message`() {
        show("showMembers")
        `when`(language["UnknownPlayer"]).thenReturn("Unknown player")
        val role = MfFactionRole(plugin, name = "Owner")
        `when`(faction.members).thenReturn(listOf(MfFactionMember(MfPlayerId("missing"), role)))

        val result = uut.build(faction)

        assertEquals("<h1>Avalon</h1><h2>Members (1)</h2><h3>Owner (1)</h3>\nUnknown player", result)
    }

    @Test
    fun `build shows the liege when there is one`() {
        show("showLiege")
        val liege = faction("liege-id", "Camelot")
        `when`(relationshipService.getLiege(factionId)).thenReturn(liege)

        assertEquals("<h1>Avalon</h1><h2>Liege</h2>Camelot<br />", uut.build(faction))
    }

    @Test
    fun `build omits the liege section when there is no liege`() {
        show("showLiege")
        `when`(relationshipService.getLiege(factionId)).thenReturn(null)

        assertEquals("<h1>Avalon</h1>", uut.build(faction))
    }

    @Test
    fun `build omits the liege section when the liege faction no longer exists`() {
        show("showLiege")
        `when`(relationshipService.getLiege(factionId)).thenReturn(MfFactionId("deleted-liege"))

        assertEquals("<h1>Avalon</h1>", uut.build(faction))
    }

    @Test
    fun `build lists vassals and skips ones that no longer exist`() {
        show("showVassals")
        val first = faction("vassal-1", "Lyonesse")
        val second = faction("vassal-2", "Corbenic")
        `when`(relationshipService.getVassals(factionId))
            .thenReturn(listOf(first, MfFactionId("deleted-vassal"), second))

        assertEquals("<h1>Avalon</h1><h2>Vassals</h2>Lyonesse, Corbenic<br />", uut.build(faction))
    }

    @Test
    fun `build omits the vassals section when there are none`() {
        show("showVassals")
        `when`(relationshipService.getVassals(factionId)).thenReturn(emptyList())

        assertEquals("<h1>Avalon</h1>", uut.build(faction))
    }

    @Test
    fun `build lists allies`() {
        show("showAllies")
        val ally = faction("ally-id", "Gaul")
        `when`(relationshipService.getAllies(factionId)).thenReturn(listOf(ally))

        assertEquals("<h1>Avalon</h1><h2>Allies</h2>Gaul<br />", uut.build(faction))
    }

    @Test
    fun `build omits the allies section when every ally no longer exists`() {
        show("showAllies")
        `when`(relationshipService.getAllies(factionId)).thenReturn(listOf(MfFactionId("deleted-ally")))

        assertEquals("<h1>Avalon</h1>", uut.build(faction))
    }

    @Test
    fun `build lists the factions it is at war with`() {
        show("showAtWarWith")
        val first = faction("enemy-1", "Saxony")
        val second = faction("enemy-2", "Pictland")
        `when`(relationshipService.getFactionsAtWarWith(factionId)).thenReturn(listOf(first, second))

        assertEquals("<h1>Avalon</h1><h2>At war with</h2>Saxony, Pictland<br />", uut.build(faction))
    }

    @Test
    fun `build omits the at war section when there are no wars`() {
        show("showAtWarWith")
        `when`(relationshipService.getFactionsAtWarWith(factionId)).thenReturn(emptyList())

        assertEquals("<h1>Avalon</h1>", uut.build(faction))
    }

    @Test
    fun `build rounds power down`() {
        show("showPower")
        `when`(faction.power).thenReturn(12.9)

        assertEquals("<h1>Avalon</h1><h2>Power</h2>12<br />", uut.build(faction))
    }

    @Test
    fun `build shows demesne as claim count over power rounded down`() {
        show("showDemesne")
        `when`(faction.power).thenReturn(10.7)
        `when`(claimService.getClaims(factionId)).thenReturn(List(3) { mock(MfClaimedChunk::class.java) })

        assertEquals("<h1>Avalon</h1><h2>Demesne</h2>3/10", uut.build(faction))
    }

    // Characterizes current behaviour, which issue #2159 reports as a bug: names and descriptions are inserted
    // into the popup HTML unescaped. The fix for #2159 must change this assertion to the escaped output.
    @Test
    fun `build inserts the name and description without escaping_issue2159`() {
        show("showDescription")
        `when`(faction.name).thenReturn("<b>Avalon</b>")
        `when`(faction.description).thenReturn("<i>desc</i> & more")

        assertEquals("<h1><b>Avalon</b></h1><h2>Description</h2><i>desc</i> & more", uut.build(faction))
    }

    @Test
    fun `build emits sections in a fixed order when all are on`() {
        listOf(
            "showDescription",
            "showMembers",
            "showLiege",
            "showVassals",
            "showAllies",
            "showAtWarWith",
            "showPower",
            "showDemesne"
        ).forEach(::show)
        `when`(faction.description).thenReturn("desc")
        `when`(faction.members).thenReturn(emptyList())
        `when`(faction.power).thenReturn(5.0)
        val liege = faction("liege-id", "Camelot")
        val vassal = faction("vassal-id", "Lyonesse")
        val ally = faction("ally-id", "Gaul")
        val enemy = faction("enemy-id", "Saxony")
        `when`(relationshipService.getLiege(factionId)).thenReturn(liege)
        `when`(relationshipService.getVassals(factionId)).thenReturn(listOf(vassal))
        `when`(relationshipService.getAllies(factionId)).thenReturn(listOf(ally))
        `when`(relationshipService.getFactionsAtWarWith(factionId)).thenReturn(listOf(enemy))
        `when`(claimService.getClaims(factionId)).thenReturn(emptyList())

        val result = uut.build(faction)

        val headings = listOf(
            "<h1>Avalon</h1>",
            "<h2>Description</h2>",
            "<h2>Members (0)</h2>",
            "<h2>Liege</h2>",
            "<h2>Vassals</h2>",
            "<h2>Allies</h2>",
            "<h2>At war with</h2>",
            "<h2>Power</h2>",
            "<h2>Demesne</h2>"
        )
        val positions = headings.map { result.indexOf(it) }
        assertFalse(positions.contains(-1), "every heading should be present in $result")
        assertEquals(positions.sorted(), positions, "headings should appear in order in $result")
        assertTrue(result.endsWith("<h2>Demesne</h2>0/5"))
    }

    private fun show(section: String) {
        `when`(config.getBoolean("dynmap.$section")).thenReturn(true)
    }

    private fun player(id: String, name: String): MfPlayer {
        val player = MfPlayer(MfPlayerId(id), name = name)
        `when`(playerService.getPlayer(player.id)).thenReturn(player)
        return player
    }

    // Returns the id rather than the mock, so callers never read a mock's property inside another stubbing call.
    private fun faction(id: String, name: String): MfFactionId {
        val other = mock(MfFaction::class.java)
        val otherId = MfFactionId(id)
        `when`(other.id).thenReturn(otherId)
        `when`(other.name).thenReturn(name)
        `when`(factionService.getFaction(otherId)).thenReturn(other)
        return otherId
    }
}
