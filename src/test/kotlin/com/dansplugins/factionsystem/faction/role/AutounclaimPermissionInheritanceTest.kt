package com.dansplugins.factionsystem.faction.role

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.flag.MfFlagValues
import com.dansplugins.factionsystem.faction.flag.MfFlags
import com.dansplugins.factionsystem.faction.permission.MfFactionPermissions
import com.dansplugins.factionsystem.lang.Language
import org.bukkit.configuration.file.FileConfiguration
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TOGGLE_AUTOUNCLAIM was added after factions already existed, so their Owner roles have no entry for it (or for
 * granting it). Until a role is given one, it follows the role's TOGGLE_AUTOCLAIM entry.
 */
class AutounclaimPermissionInheritanceTest {

    private lateinit var plugin: MedievalFactions
    private lateinit var permissions: MfFactionPermissions

    @BeforeTest
    fun setUp() {
        plugin = mock(MedievalFactions::class.java)
        val language = mock(Language::class.java) { invocation -> invocation.arguments.firstOrNull() as? String ?: "" }
        `when`(plugin.language).thenReturn(language)
        permissions = MfFactionPermissions(plugin)
        `when`(plugin.factionPermissions).thenReturn(permissions)
        `when`(plugin.config).thenReturn(mock(FileConfiguration::class.java))
        val flags = MfFlags(plugin)
        `when`(plugin.flags).thenReturn(flags)
    }

    private fun factionWith(role: MfFactionRole) = MfFaction(
        plugin,
        id = MfFactionId("f"),
        name = "F",
        flags = MfFlagValues(plugin, emptyMap()),
        roles = MfFactionRoles(role.id, listOf(role)),
        defaultPermissionsByName = emptyMap()
    )

    private fun role(vararg grants: Pair<String, Boolean?>) =
        MfFactionRole(plugin, id = MfFactionRoleId("r"), name = "Owner", permissionsByName = mapOf(*grants))

    @Test
    fun anExistingOwnerWhoMayToggleAutoclaimMayToggleAutounclaim() {
        val owner = role("TOGGLE_AUTOCLAIM" to true)
        assertTrue(owner.hasPermission(factionWith(owner), permissions.toggleAutounclaim))
    }

    @Test
    fun anExistingOwnerWhoMayGrantAutoclaimMayGrantAutounclaim() {
        val owner = role("SET_ROLE_PERMISSION(TOGGLE_AUTOCLAIM)" to true)
        val faction = factionWith(owner)
        assertTrue(owner.hasPermission(faction, permissions.setRolePermission(permissions.toggleAutounclaim)))
    }

    @Test
    fun anExplicitAutounclaimEntryWins() {
        val owner = role("TOGGLE_AUTOCLAIM" to true, "TOGGLE_AUTOUNCLAIM" to false)
        assertFalse(owner.hasPermission(factionWith(owner), permissions.toggleAutounclaim))
    }

    @Test
    fun aRoleWithoutAutoclaimStillCannotToggleAutounclaim() {
        val member = role()
        assertFalse(member.hasPermission(factionWith(member), permissions.toggleAutounclaim))
        val denied = role("TOGGLE_AUTOCLAIM" to false)
        assertFalse(denied.hasPermission(factionWith(denied), permissions.toggleAutounclaim))
    }

    @Test
    fun aNewFactionsOwnerIsGrantedAutounclaim() {
        val owner = MfFactionRoles.defaults(plugin, MfFactionId("new")).single { it.name == "Owner" }
        assertTrue(owner.permissionsByName["TOGGLE_AUTOUNCLAIM"] == true)
    }
}
