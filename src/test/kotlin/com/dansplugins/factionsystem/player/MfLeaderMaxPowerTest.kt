package com.dansplugins.factionsystem.player

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionMember
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.faction.flag.MfFlagValues
import com.dansplugins.factionsystem.faction.permission.MfFactionPermissions
import com.dansplugins.factionsystem.faction.role.MfFactionRole
import com.dansplugins.factionsystem.faction.role.MfFactionRoleId
import com.dansplugins.factionsystem.faction.role.MfFactionRoles
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.player.JooqMfPlayerRepositoryTest.Companion.migratedH2Dsl
import com.dansplugins.factionsystem.player.JooqMfPlayerRepositoryTest.Companion.powerConfiguredPlugin
import com.dansplugins.factionsystem.service.Services
import com.dansplugins.factionsystem.storage.json.JsonMfPlayerRepository
import com.dansplugins.factionsystem.storage.json.JsonStorageManager
import dev.forkhandles.result4k.Success
import org.bukkit.configuration.Configuration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.nio.file.Path
import java.util.UUID

/**
 * Covers `players.leaderMaxPower` (#1945): a separate maximum power for faction leaders (members with the Owner role).
 * Unset, leaders use `players.maxPower` exactly as before.
 */
class MfLeaderMaxPowerTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var plugin: MedievalFactions
    private lateinit var config: Configuration
    private lateinit var factionService: MfFactionService
    private lateinit var repository: JooqMfPlayerRepository
    private lateinit var service: MfPlayerService

    private val ownerRoleId = MfFactionRoleId("owner")
    private val memberRoleId = MfFactionRoleId("member")
    private val leaderId = newId()
    private val memberId = newId()
    private val factionlessId = newId()

    @BeforeEach
    fun setUp() {
        plugin = powerConfiguredPlugin()
        config = plugin.config
        val language = mock(Language::class.java) { invocation -> invocation.arguments.firstOrNull() as? String ?: "" }
        `when`(plugin.language).thenReturn(language)
        val factionPermissions = MfFactionPermissions(plugin)
        `when`(plugin.factionPermissions).thenReturn(factionPermissions)
        val services = mock(Services::class.java)
        `when`(services.mapService).thenReturn(null)
        factionService = mock(MfFactionService::class.java)
        `when`(services.factionService).thenReturn(factionService)
        `when`(plugin.services).thenReturn(services)
        repository = JooqMfPlayerRepository(plugin, migratedH2Dsl())
        service = MfPlayerService(plugin, repository)
    }

    // --- MfMaxPower --------------------------------------------------------------------------------------------------

    @Test
    fun `unset, a leader uses players maxPower`() {
        val faction = faction()
        assertEquals(null, MfMaxPower.leaderMaxPower(config))
        assertEquals(20.0, MfMaxPower.forPlayer(config, faction, leaderId))
        assertEquals(emptySet<MfPlayerId>(), MfMaxPower.leaderIds(config, listOf(faction)))
    }

    @Test
    fun `set, leaders use leaderMaxPower and everyone else maxPower`() {
        setLeaderMaxPower(30)
        val faction = faction()
        assertEquals(30.0, MfMaxPower.forPlayer(config, faction, leaderId))
        assertEquals(20.0, MfMaxPower.forPlayer(config, faction, memberId))
        assertEquals(20.0, MfMaxPower.forPlayer(config, null, factionlessId))
        assertEquals(setOf(leaderId), MfMaxPower.leaderIds(config, listOf(faction)))
    }

    @Test
    fun `the Owner role is matched regardless of case`() {
        setLeaderMaxPower(30.0)
        assertEquals(30.0, MfMaxPower.forPlayer(config, faction(ownerRoleName = "OWNER"), leaderId))
    }

    @Test
    fun `a value of 0 or less, or one that is not a number, counts as unset`() {
        listOf<Any>(0, -4.0, "thirty").forEach { value ->
            `when`(config.get(MfMaxPower.LEADER_MAX_POWER_PATH)).thenReturn(value)
            assertEquals(null, MfMaxPower.leaderMaxPower(config), "value $value")
            assertEquals(20.0, MfMaxPower.forPlayer(config, faction(), leaderId), "value $value")
        }
    }

    @Test
    fun `a faction's max member power counts each leader at leaderMaxPower`() {
        val faction = faction()
        assertEquals(40.0, faction.maxMemberPower)
        setLeaderMaxPower(30)
        assertEquals(50.0, faction.maxMemberPower)
    }

    // --- MfPlayerService.getMaxPower ---------------------------------------------------------------------------------

    @Test
    fun `getMaxPower gives a leader leaderMaxPower and a factionless player maxPower`() {
        assertEquals(20.0, service.getMaxPower(leaderId))
        setLeaderMaxPower(30)
        givenFactions(faction())
        assertEquals(30.0, service.getMaxPower(leaderId))
        assertEquals(20.0, service.getMaxPower(memberId))
        assertEquals(20.0, service.getMaxPower(factionlessId))
    }

    // --- Power update, database storage ------------------------------------------------------------------------------

    @Test
    fun `unset, a leader at maxPower gains nothing and nobody is clamped`() {
        saved(MfPlayer(leaderId, power = 20.0, powerAtLogout = 20.0))
        saved(MfPlayer(factionlessId, power = 25.0, powerAtLogout = 25.0))

        service.updatePlayerPower(listOf(leaderId, factionlessId))

        assertEquals(20.0, repository.getPlayer(leaderId)!!.power)
        assertEquals(25.0, repository.getPlayer(factionlessId)!!.power)
    }

    @Test
    fun `set, an online leader gains past maxPower while a member stops at it`() {
        setLeaderMaxPower(30)
        givenFactions(faction())
        saved(MfPlayer(leaderId, power = 20.0, powerAtLogout = 20.0))
        saved(MfPlayer(memberId, power = 20.0, powerAtLogout = 20.0))

        service.updatePlayerPower(listOf(leaderId, memberId))

        val leaderPower = repository.getPlayer(leaderId)!!.power
        assertTrue(leaderPower > 20.0 && leaderPower <= 30.0, "leader power was $leaderPower")
        assertEquals(20.0, repository.getPlayer(memberId)!!.power)
    }

    @Test
    fun `an online leader never goes above leaderMaxPower`() {
        setLeaderMaxPower(30)
        givenFactions(faction())
        saved(MfPlayer(leaderId, power = 29.999, powerAtLogout = 29.999))

        repeat(5) { service.updatePlayerPower(listOf(leaderId)) }

        assertTrue(repository.getPlayer(leaderId)!!.power <= 30.0)
    }

    @Test
    fun `a player who stops being a leader is clamped down to maxPower on the next update, not wiped`() {
        setLeaderMaxPower(30)
        // The former leader now holds the Member role
        givenFactions(faction(leader = memberId, member = leaderId))
        saved(MfPlayer(leaderId, power = 28.0, powerAtLogout = 28.0))

        service.updatePlayerPower(listOf(leaderId))

        assertEquals(20.0, repository.getPlayer(leaderId)!!.power)
    }

    @Test
    fun `the clamp runs even with gain and decay over time disabled`() {
        setLeaderMaxPower(30)
        `when`(config.getBoolean("players.disablePowerGainOverTime")).thenReturn(true)
        `when`(config.getBoolean("players.disablePowerDecayOverTime")).thenReturn(true)
        givenFactions(faction())
        saved(MfPlayer(factionlessId, power = 28.0, powerAtLogout = 28.0))
        saved(MfPlayer(leaderId, power = 28.0, powerAtLogout = 28.0))

        service.updatePlayerPower(emptyList())

        assertEquals(20.0, repository.getPlayer(factionlessId)!!.power)
        assertEquals(28.0, repository.getPlayer(leaderId)!!.power)
    }

    @Test
    fun `a leader above a lowered leaderMaxPower is clamped to it`() {
        setLeaderMaxPower(25)
        givenFactions(faction())
        saved(MfPlayer(leaderId, power = 28.0, powerAtLogout = 28.0))

        service.updatePlayerPower(listOf(leaderId))

        assertEquals(25.0, repository.getPlayer(leaderId)!!.power)
    }

    @Test
    fun `an offline leader decays from above maxPower instead of being capped at it`() {
        setLeaderMaxPower(30)
        givenFactions(faction())
        saved(MfPlayer(leaderId, power = 30.0, powerAtLogout = 30.0))
        saved(MfPlayer(memberId, power = 20.0, powerAtLogout = 20.0))

        service.updatePlayerPower(emptyList())

        val leaderPower = repository.getPlayer(leaderId)!!.power
        assertTrue(leaderPower > 20.0 && leaderPower < 30.0, "leader power was $leaderPower")
        assertTrue(repository.getPlayer(memberId)!!.power < 20.0)
    }

    // --- Power update, JSON storage ----------------------------------------------------------------------------------

    @Test
    fun `JSON storage gives leaders the same maximum`() {
        setLeaderMaxPower(30)
        `when`(plugin.dataFolder).thenReturn(tempDir.toFile())
        `when`(plugin.getResource("schemas/players.json"))
            .thenAnswer { java.io.File("src/main/resources/schemas/players.json").inputStream() }
        val json = JsonMfPlayerRepository(plugin, JsonStorageManager(plugin, tempDir.toString()))
        json.upsert(MfPlayer(leaderId, power = 20.0, powerAtLogout = 20.0))
        json.upsert(MfPlayer(memberId, power = 20.0, powerAtLogout = 20.0))
        json.upsert(MfPlayer(factionlessId, power = 26.0, powerAtLogout = 26.0))

        json.clampPowerToMax(setOf(leaderId))
        json.increaseOnlinePlayerPower(listOf(leaderId, memberId), setOf(leaderId))

        val leaderPower = json.getPlayer(leaderId)!!.power
        assertTrue(leaderPower > 20.0 && leaderPower <= 30.0, "leader power was $leaderPower")
        assertEquals(20.0, json.getPlayer(memberId)!!.power)
        assertEquals(20.0, json.getPlayer(factionlessId)!!.power)
    }

    // --- Helpers -----------------------------------------------------------------------------------------------------

    private fun setLeaderMaxPower(value: Number) {
        `when`(config.get(MfMaxPower.LEADER_MAX_POWER_PATH)).thenReturn(value)
    }

    private fun faction(leader: MfPlayerId = leaderId, member: MfPlayerId = memberId, ownerRoleName: String = "Owner"): MfFaction {
        val owner = MfFactionRole(plugin, id = ownerRoleId, name = ownerRoleName)
        val memberRole = MfFactionRole(plugin, id = memberRoleId, name = "Member")
        return MfFaction(
            plugin,
            name = "Wessex",
            flags = MfFlagValues(plugin, emptyMap()),
            roles = MfFactionRoles(memberRoleId, listOf(owner, memberRole)),
            members = listOf(MfFactionMember(leader, owner), MfFactionMember(member, memberRole)),
            defaultPermissionsByName = emptyMap()
        )
    }

    private fun givenFactions(vararg factions: MfFaction) {
        `when`(factionService.factions).thenReturn(factions.toList())
        factions.forEach { faction ->
            faction.members.forEach { `when`(factionService.getFaction(it.playerId)).thenReturn(faction) }
        }
    }

    private fun saved(player: MfPlayer) = (service.save(player) as Success).value

    private fun newId() = MfPlayerId(UUID.randomUUID().toString())
}
