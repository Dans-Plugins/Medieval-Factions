package com.dansplugins.factionsystem.lockedarea

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.area.MfCuboidArea
import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.MfFactionMember
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.faction.flag.MfFlagValues
import com.dansplugins.factionsystem.faction.flag.MfFlags
import com.dansplugins.factionsystem.faction.permission.MfFactionPermissions
import com.dansplugins.factionsystem.faction.role.MfFactionRole
import com.dansplugins.factionsystem.faction.role.MfFactionRoles
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.service.Services
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.RETURNS_DEFAULTS
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.UUID
import java.util.logging.Logger

/** Who a locked area (#1393) refuses, and who it lets through. */
class MfLockedAreaServiceTest {

    private val world = UUID.randomUUID()
    private val factionId = MfFactionId.generate()
    private lateinit var plugin: MedievalFactions
    private lateinit var factionService: MfFactionService
    private lateinit var player: Player
    private lateinit var steward: MfFactionRole
    private lateinit var member: MfFactionRole
    private lateinit var manager: MfFactionRole

    private val inside = MfBlockPosition(world, 5, 64, 5)
    private val outside = MfBlockPosition(world, 50, 64, 50)
    private val claim = MfClaimedChunk(world, 0, 0, factionId)

    @BeforeEach
    fun setUp() {
        plugin = mock(MedievalFactions::class.java)
        `when`(plugin.logger).thenReturn(Logger.getLogger("test"))
        val language = mock(Language::class.java) { invocation ->
            if (invocation.method.returnType == String::class.java) invocation.arguments[0] else RETURNS_DEFAULTS.answer(invocation)
        }
        `when`(plugin.language).thenReturn(language)
        `when`(plugin.config).thenReturn(mock(FileConfiguration::class.java))
        val flags = MfFlags(plugin)
        `when`(plugin.flags).thenReturn(flags)
        val permissions = MfFactionPermissions(plugin)
        `when`(plugin.factionPermissions).thenReturn(permissions)
        val services = mock(Services::class.java)
        factionService = mock(MfFactionService::class.java)
        `when`(plugin.services).thenReturn(services)
        `when`(services.factionService).thenReturn(factionService)
        player = mock(Player::class.java)
        steward = MfFactionRole(plugin, name = "Steward")
        member = MfFactionRole(plugin, name = "Member")
        manager = MfFactionRole(plugin, name = "Lord", permissionsByName = mapOf("MANAGE_LOCKED_AREAS" to true))
    }

    @Test
    fun `an unlisted member is refused inside the area and nowhere else`() {
        val memberId = newPlayerId()
        val service = serviceWith(area(), members = mapOf(memberId to member))

        assertEquals("Vault", service.getRefusingArea(mfPlayer(memberId), player, claim, inside)?.name)
        assertNull(service.getRefusingArea(mfPlayer(memberId), player, claim, outside))
    }

    @Test
    fun `a listed player and a holder of a listed role are let in`() {
        val listed = newPlayerId()
        val holder = newPlayerId()
        val service = serviceWith(
            area().copy(accessors = listOf(listed), accessorRoleIds = listOf(steward.id)),
            members = mapOf(listed to member, holder to steward)
        )

        assertNull(service.getRefusingArea(mfPlayer(listed), player, claim, inside))
        assertNull(service.getRefusingArea(mfPlayer(holder), player, claim, inside))
    }

    @Test
    fun `a member whose role may manage locked areas is let in`() {
        val lord = newPlayerId()
        val service = serviceWith(area(), members = mapOf(lord to manager))

        assertNull(service.getRefusingArea(mfPlayer(lord), player, claim, inside))
    }

    @Test
    fun `a player with bypass on is let in`() {
        val memberId = newPlayerId()
        val service = serviceWith(area(), members = mapOf(memberId to member))
        `when`(player.hasPermission("mf.bypass")).thenReturn(true)

        assertNull(service.getRefusingArea(mfPlayer(memberId, bypass = true), player, claim, inside))
    }

    @Test
    fun `an area protects nothing in land another faction now holds`() {
        val memberId = newPlayerId()
        val service = serviceWith(area(), members = mapOf(memberId to member))
        val otherClaim = MfClaimedChunk(world, 0, 0, MfFactionId.generate())

        assertNull(service.getRefusingArea(mfPlayer(memberId), player, otherClaim, inside))
    }

    @Test
    fun `selections keep both corners`() {
        val service = serviceWith(area(), members = emptyMap())
        val playerId = newPlayerId()

        service.select(playerId, 1, inside)
        service.select(playerId, 2, outside)

        assertEquals(inside to outside, service.getSelection(playerId))
    }

    private fun newPlayerId() = MfPlayerId(UUID.randomUUID().toString())

    private fun mfPlayer(id: MfPlayerId, bypass: Boolean = false) = MfPlayer(id, name = "Steve", isBypassEnabled = bypass)

    private fun area() = MfLockedArea(
        factionId = factionId,
        name = "Vault",
        area = MfCuboidArea(MfBlockPosition(world, 0, 60, 0), MfBlockPosition(world, 10, 70, 10))
    )

    private fun serviceWith(lockedArea: MfLockedArea, members: Map<MfPlayerId, MfFactionRole>): MfLockedAreaService {
        val faction = MfFaction(
            plugin = plugin,
            id = factionId,
            name = "Keep",
            members = members.map { (id, role) -> MfFactionMember(id, role) },
            flags = MfFlagValues(plugin, emptyMap()),
            roles = MfFactionRoles(member.id, listOf(member, steward, manager)),
            defaultPermissionsByName = emptyMap()
        )
        `when`(factionService.getFaction(factionId)).thenReturn(faction)
        val repository = mock(MfLockedAreaRepository::class.java)
        `when`(repository.getLockedAreas()).thenReturn(listOf(lockedArea))
        return MfLockedAreaService(plugin, repository)
    }
}
