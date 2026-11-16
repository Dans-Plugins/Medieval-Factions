package com.dansplugins.factionsystem.faction.role

import com.dansplugins.factionsystem.MedievalFactions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.plugin.PluginManager
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.`when`
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Role prefixes (#1525) are stored in the roles JSON that the database backend keeps in `mf_faction.roles`, so roles
 * written before prefixes existed must read as having none, and a role without one must be written as before.
 */
class MfFactionRolePrefixTest {

    private val plugin = mock(MedievalFactions::class.java)
    private val gson = Gson()

    private fun deserialize(json: String): MfFactionRole {
        val serialized = gson.fromJson<Map<String, Any?>>(json, object : TypeToken<Map<String, Any?>>() {}.type)
        val pluginManager = mock(PluginManager::class.java)
        `when`(pluginManager.getPlugin("MedievalFactions")).thenReturn(plugin)
        mockStatic(Bukkit::class.java).use { bukkit ->
            bukkit.`when`<PluginManager> { Bukkit.getPluginManager() }.thenReturn(pluginManager)
            return MfFactionRole.deserialize(serialized)
        }
    }

    @Test
    fun aRoleWithoutAPrefixIsSerializedExactlyAsBefore() {
        val role = MfFactionRole(plugin, MfFactionRoleId("r1"), "Member", mapOf("CLAIM" to true))
        assertEquals(setOf("id", "name", "permissions"), role.serialize().keys)
    }

    @Test
    fun aRolePrefixSurvivesTheRolesJson() {
        val role = MfFactionRole(plugin, MfFactionRoleId("r1"), "Knight", mapOf("CLAIM" to true), "&6[Knight]")
        val read = deserialize(gson.toJson(role.serialize()))
        assertEquals("&6[Knight]", read.prefix)
        assertEquals("Knight", read.name)
        assertEquals(mapOf("CLAIM" to true), read.permissionsByName)
    }

    @Test
    fun rolesJsonWrittenBeforePrefixesReadsAsNoPrefix() {
        val read = deserialize("""{"id":"r1","name":"Member","permissions":{"CLAIM":true}}""")
        assertEquals("Member", read.name)
        assertNull(read.prefix)
    }

    @Test
    fun theDisplayPrefixTranslatesColourCodesAndIsEmptyWithoutAPrefix() {
        assertEquals("${ChatColor.GOLD}[Knight]", MfFactionRole(plugin, name = "Knight", prefix = "&6[Knight]").displayPrefix)
        assertEquals("", MfFactionRole(plugin, name = "Member").displayPrefix)
    }
}
