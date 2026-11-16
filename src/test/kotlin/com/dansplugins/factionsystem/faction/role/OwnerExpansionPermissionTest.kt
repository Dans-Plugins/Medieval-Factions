package com.dansplugins.factionsystem.faction.role

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.flag.MfFlagValues
import com.dansplugins.factionsystem.faction.flag.MfFlags
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission
import com.dansplugins.factionsystem.faction.permission.MfFactionPermissions
import com.dansplugins.factionsystem.lang.Language
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.bukkit.Bukkit
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.PluginManager
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.`when`
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The Owner role is granted every permission an expansion registers through `addPermissionType`, and the permission
 * to set it for other roles, until it is given an entry of its own (#1683, #1796). Other roles keep the faction default
 * and then the code default.
 */
class OwnerExpansionPermissionTest {

    private lateinit var plugin: MedievalFactions
    private lateinit var permissions: MfFactionPermissions
    private lateinit var expansionPermission: MfFactionPermission
    private lateinit var expansionPermissionDefaultTrue: MfFactionPermission

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
        permissions.addPermissionType(permissions.wrapSimplePermission("EXPANSION_ACTION", "Expansion action", false))
        permissions.addPermissionType(permissions.wrapSimplePermission("EXPANSION_VIEW", "Expansion view", true))
        expansionPermission = permissions.parse("EXPANSION_ACTION")!!
        expansionPermissionDefaultTrue = permissions.parse("EXPANSION_VIEW")!!
    }

    private fun role(name: String, vararg grants: Pair<String, Boolean?>) =
        MfFactionRole(plugin, id = MfFactionRoleId(name.lowercase()), name = name, permissionsByName = mapOf(*grants))

    private fun factionWith(vararg roles: MfFactionRole, defaults: Map<String, Boolean> = emptyMap()) = MfFaction(
        plugin,
        id = MfFactionId("f"),
        name = "F",
        flags = MfFlagValues(plugin, emptyMap()),
        roles = MfFactionRoles(roles.last().id, roles.toList()),
        defaultPermissionsByName = defaults
    )

    @Test
    fun aNewlyRegisteredPermissionResolvesTrueForTheOwnerAndToTheCodeDefaultForOthers() {
        val owner = role("Owner")
        val member = role("Member")
        val faction = factionWith(owner, member)
        assertTrue(owner.hasPermission(faction, expansionPermission))
        assertFalse(member.hasPermission(faction, expansionPermission))
        assertTrue(member.hasPermission(faction, expansionPermissionDefaultTrue))
    }

    @Test
    fun theOwnerMayGrantANewlyRegisteredPermissionAndOthersMayNot() {
        val owner = role("Owner")
        val officer = role("Officer")
        val faction = factionWith(owner, officer)
        assertTrue(owner.hasPermission(faction, permissions.setRolePermission(expansionPermission)))
        assertFalse(officer.hasPermission(faction, permissions.setRolePermission(expansionPermission)))
    }

    @Test
    fun anExplicitFalseOnTheOwnerIsRespected() {
        val owner = role(
            "Owner",
            "EXPANSION_ACTION" to false,
            "SET_ROLE_PERMISSION(EXPANSION_ACTION)" to false
        )
        val faction = factionWith(owner)
        assertFalse(owner.hasPermission(faction, expansionPermission))
        assertFalse(owner.hasPermission(faction, permissions.setRolePermission(expansionPermission)))
    }

    @Test
    fun theOwnerGrantOutranksTheFactionDefaultButOtherRolesFollowIt() {
        val owner = role("Owner")
        val member = role("Member")
        val denied = factionWith(owner, member, defaults = mapOf("EXPANSION_ACTION" to false))
        assertTrue(owner.hasPermission(denied, expansionPermission))
        val granted = factionWith(owner, member, defaults = mapOf("EXPANSION_ACTION" to true))
        assertTrue(member.hasPermission(granted, expansionPermission))
    }

    @Test
    fun theOwnerRoleIsMatchedByNameIgnoringCase() {
        val owner = role("owner")
        assertTrue(owner.hasPermission(factionWith(owner), expansionPermission))
        val renamed = role("King")
        assertFalse(renamed.hasPermission(factionWith(renamed), expansionPermission))
    }

    @Test
    fun builtInPermissionsAreNotGrantedToTheOwner() {
        val owner = role("Owner")
        val faction = factionWith(owner)
        assertFalse(permissions.isExpansionPermission(permissions.bypassLocks))
        assertFalse(permissions.isExpansionPermission(permissions.setRolePermission(permissions.bypassLocks)))
        assertFalse(owner.hasPermission(faction, permissions.bypassLocks))
        assertFalse(owner.hasPermission(faction, permissions.setRolePermission(permissions.bypassLocks)))
    }

    @Test
    fun aNewFactionsOwnerHoldsANewlyRegisteredPermission() {
        val roles = MfFactionRoles.defaults(plugin, MfFactionId("new"))
        val faction = factionWith(*roles.toTypedArray())
        val owner = roles.single { it.name == "Owner" }
        val member = roles.single { it.name == "Member" }
        assertTrue(owner.hasPermission(faction, expansionPermission))
        assertFalse(member.hasPermission(faction, expansionPermission))
    }

    @Test
    fun onlyARoleThatMayModifyEveryRoleMayNameARoleOwner() {
        val king = role("King", "MODIFY_ROLE(king)" to true, "MODIFY_ROLE(member)" to true)
        val officer = role("Officer", "MODIFY_ROLE(member)" to true)
        val member = role("Member")
        val faction = factionWith(king, officer, member)
        assertFalse(king.mayNameRoleOwner(faction))
        val kingOfAll = role(
            "King",
            "MODIFY_ROLE(king)" to true,
            "MODIFY_ROLE(officer)" to true,
            "MODIFY_ROLE(member)" to true
        )
        val faction2 = factionWith(kingOfAll, officer, member)
        assertTrue(kingOfAll.mayNameRoleOwner(faction2))
        assertFalse(officer.mayNameRoleOwner(faction2))
    }

    @Test
    fun rolesJsonWrittenBeforeThisChangeLoadsUnchanged() {
        val json = """{"id":"owner-id","name":"Owner","permissions":{"CLAIM":true,"EXPANSION_ACTION":false}}"""
        val serialized = Gson().fromJson<Map<String, Any?>>(json, object : TypeToken<Map<String, Any?>>() {}.type)
        val pluginManager = mock(PluginManager::class.java)
        `when`(pluginManager.getPlugin("MedievalFactions")).thenReturn(plugin)
        val read = mockStatic(Bukkit::class.java).use { bukkit ->
            bukkit.`when`<PluginManager> { Bukkit.getPluginManager() }.thenReturn(pluginManager)
            MfFactionRole.deserialize(serialized)
        }
        assertEquals(mapOf("CLAIM" to true, "EXPANSION_ACTION" to false), read.permissionsByName)
        assertEquals(setOf("id", "name", "permissions"), read.serialize().keys)
        assertEquals(read.permissionsByName, read.serialize()["permissions"])
        val faction = factionWith(read)
        assertFalse(read.hasPermission(faction, expansionPermission))
        assertTrue(read.hasPermission(faction, expansionPermissionDefaultTrue))
        assertTrue(read.hasPermission(faction, permissions.claim))
    }
}
