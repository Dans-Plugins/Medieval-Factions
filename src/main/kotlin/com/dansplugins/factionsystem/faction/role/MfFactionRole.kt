package com.dansplugins.factionsystem.faction.role

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.configuration.serialization.ConfigurationSerializable

data class MfFactionRole(
    val plugin: MedievalFactions,
    @get:JvmName("getId")
    val id: MfFactionRoleId = MfFactionRoleId.generate(),
    val name: String,
    val permissionsByName: Map<String, Boolean?> = emptyMap(),
    /**
     * A prefix shown for members of this role, such as `[Knight]`, or null for none (#1525). It may hold `&` colour
     * codes, which are translated where it is shown.
     */
    val prefix: String? = null
) : ConfigurationSerializable {

    val permissions: Map<MfFactionPermission, Boolean?>
        get() = permissionsByName.toList().map { (key, value) -> plugin.factionPermissions.parse(key) to value }
            .filter { (key, _) -> key != null }
            .associate { (key, value) -> key!! to value }

    /**
     * Whether this is the faction's Owner role: the role named [MfFactionRoles.OWNER_ROLE_NAME], ignoring case, which
     * is how `/faction admin setleader`, faction creation and the leader-death rule find it. Factions store no owner
     * role id, so a renamed Owner role is not treated as the Owner role.
     */
    val isOwner: Boolean
        get() = name.equals(MfFactionRoles.OWNER_ROLE_NAME, ignoreCase = true)

    /**
     * Resolves [permission] for this role: its own entry wins, then the entry it inherits from, then (for the Owner
     * role only) a grant of every permission an expansion registered, then the faction's default, then the
     * permission's code default.
     */
    fun hasPermission(faction: MfFaction, permission: MfFactionPermission) =
        permissions[permission]
            ?: INHERITED_FROM[permission.name]?.let { permissionsByName[it] }
            ?: ownerGrant(permission)
            ?: faction.defaultPermissions[permission]
            ?: permission.default

    // The Owner role holds every expansion permission it has no entry for (#1683, #1796), so an expansion added after
    // factions already exist need not rewrite their saved roles, and each faction's owner can then grant it onwards.
    private fun ownerGrant(permission: MfFactionPermission): Boolean? =
        if (isOwner && plugin.factionPermissions.isExpansionPermission(permission)) true else null

    fun getPermissionValue(permission: MfFactionPermission) = permissions[permission]

    /**
     * Whether a member of this role may give a role the Owner role's name. Because the Owner role is granted every
     * expansion permission, only a role that may already modify every role in [faction] may do so; otherwise a
     * lesser role could rename or create a role called Owner, after a faction renamed its own, to gain those grants.
     */
    fun mayNameRoleOwner(faction: MfFaction) =
        faction.roles.all { role -> hasPermission(faction, plugin.factionPermissions.modifyRole(role.id)) }

    /** [prefix] with its `&` colour codes translated, or an empty string when the role has no prefix. */
    val displayPrefix: String
        get() = prefix?.let { ChatColor.translateAlternateColorCodes('&', it) } ?: ""

    // The prefix is written only when one is set, so a role without one is stored exactly as before #1525.
    override fun serialize(): Map<String, Any> = buildMap {
        put("id", id.value)
        put("name", name)
        put("permissions", permissionsByName)
        if (prefix != null) put("prefix", prefix)
    }

    companion object {
        /** The longest prefix, colour codes included, that `/faction role setprefix` accepts. */
        const val MAX_PREFIX_LENGTH = 32

        // A permission added after factions already existed has no entry in their roles. Until a role is given one,
        // it takes the value of the permission it pairs with, so existing Owners, who may toggle autoclaim, may
        // also toggle autounclaim (and grant it) without their saved roles being rewritten.
        private val INHERITED_FROM = mapOf(
            "TOGGLE_AUTOUNCLAIM" to "TOGGLE_AUTOCLAIM",
            "SET_ROLE_PERMISSION(TOGGLE_AUTOUNCLAIM)" to "SET_ROLE_PERMISSION(TOGGLE_AUTOCLAIM)",
            // Locked areas (#1393) narrow who may build inside claimed land, so the roles that may claim it may set them up.
            "MANAGE_LOCKED_AREAS" to "CLAIM",
            "SET_ROLE_PERMISSION(MANAGE_LOCKED_AREAS)" to "SET_ROLE_PERMISSION(CLAIM)"
        )

        @JvmStatic
        fun deserialize(serialized: Map<String, Any?>): MfFactionRole {
            val id = (serialized["id"] as String).let(::MfFactionRoleId)
            val name = serialized["name"] as String
            val permissionsByName = serialized["permissions"] as Map<String, Boolean?>
            // Roles saved before #1525 have no prefix entry, and read as having no prefix.
            val prefix = serialized["prefix"] as? String
            return MfFactionRole(
                Bukkit.getPluginManager().getPlugin("MedievalFactions") as MedievalFactions,
                id,
                name,
                permissionsByName,
                prefix
            )
        }
    }
}
