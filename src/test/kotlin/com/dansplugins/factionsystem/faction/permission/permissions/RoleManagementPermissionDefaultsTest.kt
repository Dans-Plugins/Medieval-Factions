package com.dansplugins.factionsystem.faction.permission.permissions

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.role.MfFactionRoleId
import org.mockito.Mockito.mock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Changing, renaming or deleting a role must be granted explicitly: a role with no entry for these
 * permissions (the default Member role) is refused. Viewing a role stays allowed by default.
 */
class RoleManagementPermissionDefaultsTest {

    private val plugin = mock(MedievalFactions::class.java)
    private val factionId = MfFactionId("faction")
    private val roleId = MfFactionRoleId("role")

    @Test
    fun setMemberRoleDefaultsToFalse() {
        val permission = SetMemberRole(plugin).permissionsFor(factionId, listOf(roleId)).single()
        assertEquals("SET_MEMBER_ROLE(role)", permission.name)
        assertFalse(permission.default)
    }

    @Test
    fun modifyRoleDefaultsToFalse() {
        val permission = ModifyRole(plugin).permissionsFor(factionId, listOf(roleId)).single()
        assertEquals("MODIFY_ROLE(role)", permission.name)
        assertFalse(permission.default)
    }

    @Test
    fun deleteRoleDefaultsToFalse() {
        val permission = DeleteRole(plugin).permissionsFor(factionId, listOf(roleId)).single()
        assertEquals("DELETE_ROLE(role)", permission.name)
        assertFalse(permission.default)
    }

    @Test
    fun parsedPermissionsKeepTheFalseDefault() {
        val parsed = listOf(
            SetMemberRole(plugin).parse("SET_MEMBER_ROLE(role)"),
            ModifyRole(plugin).parse("MODIFY_ROLE(role)"),
            DeleteRole(plugin).parse("DELETE_ROLE(role)")
        )
        parsed.forEach { permission ->
            assertNotNull(permission)
            assertFalse(permission.default)
        }
    }

    @Test
    fun viewRoleStillDefaultsToTrue() {
        val permission = ViewRole(plugin).permissionsFor(factionId, listOf(roleId)).single()
        assertTrue(permission.default)
    }
}
