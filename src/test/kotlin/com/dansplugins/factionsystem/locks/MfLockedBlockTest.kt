package com.dansplugins.factionsystem.locks

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.MfFactionMember
import com.dansplugins.factionsystem.faction.flag.MfFlagValues
import com.dansplugins.factionsystem.faction.role.MfFactionRole
import com.dansplugins.factionsystem.faction.role.MfFactionRoleId
import com.dansplugins.factionsystem.faction.role.MfFactionRoles
import com.dansplugins.factionsystem.player.MfPlayerId
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.util.UUID

/** Who [MfLockedBlock.hasAccess] lets through, including role accessors (#1569). */
class MfLockedBlockTest {

    private val plugin = mock(MedievalFactions::class.java)
    private val owner = newPlayerId()
    private val officerRole = MfFactionRole(plugin, MfFactionRoleId.generate(), "Officer")
    private val memberRole = MfFactionRole(plugin, MfFactionRoleId.generate(), "Member")

    @Test
    fun `the owner and listed players have access without a faction`() {
        val accessor = newPlayerId()
        val lock = lockedBlock(accessors = listOf(accessor))

        assertTrue(lock.hasAccess(owner, null))
        assertTrue(lock.hasAccess(accessor, null))
        assertFalse(lock.hasAccess(newPlayerId(), null))
    }

    @Test
    fun `a member holding a listed role of the owner's faction has access`() {
        val officer = newPlayerId()
        val faction = faction(owner to memberRole, officer to officerRole)

        assertTrue(lockedBlock(roles = listOf(officerRole.id)).hasAccess(officer, faction))
    }

    @Test
    fun `a member whose role is not listed has no access`() {
        val member = newPlayerId()
        val faction = faction(owner to memberRole, member to memberRole)

        assertFalse(lockedBlock(roles = listOf(officerRole.id)).hasAccess(member, faction))
    }

    @Test
    fun `a listed role grants nothing when no faction is given`() {
        assertFalse(lockedBlock(roles = listOf(officerRole.id)).hasAccess(newPlayerId(), null))
    }

    @Test
    fun `a listed role grants nothing in a faction the owner is not in`() {
        val officer = newPlayerId()
        // The owner left; the faction still has the role and a member holding it.
        val faction = faction(officer to officerRole)

        assertFalse(lockedBlock(roles = listOf(officerRole.id)).hasAccess(officer, faction))
    }

    @Test
    fun `a player outside the owner's faction has no access through a role`() {
        val faction = faction(owner to memberRole)

        assertFalse(lockedBlock(roles = listOf(memberRole.id)).hasAccess(newPlayerId(), faction))
    }

    private fun newPlayerId() = MfPlayerId(UUID.randomUUID().toString())

    private fun lockedBlock(accessors: List<MfPlayerId> = emptyList(), roles: List<MfFactionRoleId> = emptyList()) =
        MfLockedBlock(
            block = MfBlockPosition(UUID.randomUUID(), 1, 64, 1),
            chunkX = 0,
            chunkZ = 0,
            playerId = owner,
            accessors = accessors,
            accessorRoleIds = roles
        )

    private fun faction(vararg members: Pair<MfPlayerId, MfFactionRole>) = MfFaction(
        plugin = plugin,
        id = MfFactionId.generate(),
        name = "Lockers",
        members = members.map { (playerId, role) -> MfFactionMember(playerId, role) },
        flags = MfFlagValues(plugin, emptyMap()),
        roles = MfFactionRoles(memberRole.id, listOf(memberRole, officerRole)),
        defaultPermissionsByName = emptyMap()
    )
}
