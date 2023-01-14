package com.dansplugins.factionsystem.faction.role

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission
import org.bukkit.Bukkit
import org.bukkit.configuration.serialization.ConfigurationSerializable

data class MfFactionRole(
    val plugin: MedievalFactions,
    @get:JvmName("getId")
    val id: MfFactionRoleId = MfFactionRoleId.generate(),
    val name: String,
    val permissionsByName: Map<String, Boolean?> = emptyMap()
) : ConfigurationSerializable {

    val permissions: Map<MfFactionPermission, Boolean?>
        get() = permissionsByName.toList().map { (key, value) -> plugin.factionPermissions.parse(key) to value }
            .filter { (key, _) -> key != null }
            .associate { (key, value) -> key!! to value }

    fun hasPermission(faction: MfFaction, permission: MfFactionPermission) =
        permissions[permission]
            ?: faction.defaultPermissions[permission]
            ?: permission.default

    fun getPermissionValue(permission: MfFactionPermission) = permissions[permission]

    override fun serialize() = mapOf(
        "id" to id.value,
        "name" to name,
        "permissions" to permissionsByName
    )

    companion object {
        @JvmStatic
        fun deserialize(serialized: Map<String, Any?>): MfFactionRole {
            val id = (serialized["id"] as String).let(::MfFactionRoleId)
            val name = serialized["name"] as String
            val permissionsByName = serialized["permissions"] as Map<String, Boolean?>
            return MfFactionRole(
                Bukkit.getPluginManager().getPlugin("MedievalFactions") as MedievalFactions,
                id,
                name,
                permissionsByName
            )
        }
    }
}
