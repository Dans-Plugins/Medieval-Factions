package com.dansplugins.factionsystem.faction

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.flag.MfFlagValues
import com.dansplugins.factionsystem.faction.permission.MfFactionPermissions
import com.dansplugins.factionsystem.faction.role.MfFactionRole
import com.dansplugins.factionsystem.faction.role.MfFactionRoleId
import com.dansplugins.factionsystem.faction.role.MfFactionRoles
import com.dansplugins.factionsystem.lang.Language
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A faction created before role management was made deny-by-default saved SET_MEMBER_ROLE, MODIFY_ROLE and
 * DELETE_ROLE as true in its default permissions. Read back, those saved values must not let a role with no
 * entry of its own (the default Member role) manage roles.
 */
class MfFactionSavedRoleManagementDefaultsTest {

    private lateinit var plugin: MedievalFactions
    private lateinit var permissions: MfFactionPermissions

    private val memberId = MfFactionRoleId("member")
    private val ownerId = MfFactionRoleId("owner")

    @BeforeTest
    fun setUp() {
        plugin = mock(MedievalFactions::class.java)
        // Permission display names are looked up when the permission types are registered; the key will do.
        val language = mock(Language::class.java) { invocation -> invocation.arguments.firstOrNull() as? String ?: "" }
        `when`(plugin.language).thenReturn(language)
        permissions = MfFactionPermissions(plugin)
        `when`(plugin.factionPermissions).thenReturn(permissions)
    }

    /** What a 7.0.0 (or older) faction has in `default_permissions` for its two roles. */
    private fun savedBefore8(): Map<String, Boolean> = listOf(memberId, ownerId).flatMap { id ->
        listOf(
            "SET_MEMBER_ROLE(${id.value})" to true,
            "MODIFY_ROLE(${id.value})" to true,
            "DELETE_ROLE(${id.value})" to true,
            "VIEW_ROLE(${id.value})" to true
        )
    }.toMap()

    private fun faction(saved: Map<String, Boolean>): MfFaction {
        val member = MfFactionRole(plugin, id = memberId, name = "Member")
        val owner = MfFactionRole(
            plugin,
            id = ownerId,
            name = "Owner",
            permissionsByName = mapOf(
                "SET_MEMBER_ROLE(${ownerId.value})" to true,
                "MODIFY_ROLE(${memberId.value})" to true,
                "DELETE_ROLE(${memberId.value})" to true
            )
        )
        return MfFaction(
            plugin,
            name = "Old",
            flags = MfFlagValues(plugin, emptyMap()),
            roles = MfFactionRoles(memberId, listOf(owner, member)),
            defaultPermissionsByName = saved
        )
    }

    @Test
    fun theMemberRoleCannotManageRolesFromSavedTrueDefaults() {
        val faction = faction(savedBefore8())
        val member = faction.getRole(memberId)!!
        listOf(memberId, ownerId).forEach { id ->
            assertFalse(member.hasPermission(faction, permissions.setMemberRole(id)), "SET_MEMBER_ROLE(${id.value})")
            assertFalse(member.hasPermission(faction, permissions.modifyRole(id)), "MODIFY_ROLE(${id.value})")
            assertFalse(member.hasPermission(faction, permissions.deleteRole(id)), "DELETE_ROLE(${id.value})")
        }
    }

    @Test
    fun theOwnersExplicitGrantsStillApply() {
        val faction = faction(savedBefore8())
        val owner = faction.getRole(ownerId)!!
        assertTrue(owner.hasPermission(faction, permissions.setMemberRole(ownerId)))
        assertTrue(owner.hasPermission(faction, permissions.modifyRole(memberId)))
        assertTrue(owner.hasPermission(faction, permissions.deleteRole(memberId)))
    }

    @Test
    fun otherSavedDefaultsAreReadAsSaved() {
        val faction = faction(savedBefore8())
        assertTrue(faction.getRole(memberId)!!.hasPermission(faction, permissions.viewRole(ownerId)))
        assertEquals(true, faction.defaultPermissions[permissions.viewRole(memberId)])
    }

    @Test
    fun theFalseIsWhatASaveWritesBack() {
        // The database saves `defaultPermissions`; the JSON backend saves `savedDefaultPermissionsByName`, which keeps
        // keys no loaded permission type parses. Both store false for role management after the next save.
        val faction = faction(savedBefore8() + ("ADDON_PERMISSION" to true))
        listOf(faction.defaultPermissions.mapKeys { it.key.name }, faction.savedDefaultPermissionsByName).forEach { saved ->
            assertEquals(false, saved["SET_MEMBER_ROLE(member)"])
            assertEquals(false, saved["MODIFY_ROLE(owner)"])
            assertEquals(false, saved["DELETE_ROLE(member)"])
            assertEquals(true, saved["VIEW_ROLE(member)"])
        }
        assertEquals(true, faction.savedDefaultPermissionsByName["ADDON_PERMISSION"])
    }
}
