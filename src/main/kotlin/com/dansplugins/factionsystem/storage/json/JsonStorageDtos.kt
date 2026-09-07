package com.dansplugins.factionsystem.storage.json

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.area.MfCuboidArea
import com.dansplugins.factionsystem.area.MfPosition
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionApplication
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.MfFactionInvite
import com.dansplugins.factionsystem.faction.MfFactionMember
import com.dansplugins.factionsystem.faction.flag.MfFlagValues
import com.dansplugins.factionsystem.faction.role.MfFactionRole
import com.dansplugins.factionsystem.faction.role.MfFactionRoleId
import com.dansplugins.factionsystem.faction.role.MfFactionRoles
import com.dansplugins.factionsystem.gate.MfGate
import com.dansplugins.factionsystem.gate.MfGateId
import com.dansplugins.factionsystem.gate.MfGateStatus
import com.dansplugins.factionsystem.player.MfPlayerId
import org.bukkit.Material
import java.util.UUID

/**
 * Serialization DTOs for the JSON storage backend.
 *
 * Domain types such as [MfFaction], [MfFactionRole], [MfFlagValues] and [MfGate] hold a reference to
 * the [MedievalFactions] plugin instance. Handing those objects straight to Gson makes its reflective
 * serializer walk into the plugin (and from there into the JDK internals behind [JavaPlugin]), which
 * fails outright. These DTOs describe exactly the state that belongs on disk, mirroring the columns the
 * Jooq repositories persist, and the mapping functions re-attach the plugin reference on the way back in.
 */

internal data class JsonPositionDto(
    val worldId: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float
)

internal data class JsonBlockPositionDto(
    val worldId: String,
    val x: Int,
    val y: Int,
    val z: Int
)

internal data class JsonCuboidAreaDto(
    val position1: JsonBlockPositionDto,
    val position2: JsonBlockPositionDto
)

internal data class JsonFactionRoleDto(
    val id: String,
    val name: String,
    val permissionsByName: Map<String, Boolean?> = emptyMap()
)

internal data class JsonFactionRolesDto(
    val defaultRoleId: String,
    val roles: List<JsonFactionRoleDto> = emptyList()
)

internal data class JsonFactionMemberDto(
    val playerId: String,
    val roleId: String
)

internal data class JsonFactionInviteDto(
    val playerId: String
)

internal data class JsonFactionApplicationDto(
    val playerId: String
)

internal data class JsonFactionDto(
    val id: String,
    val version: Int,
    val name: String,
    val description: String = "",
    val members: List<JsonFactionMemberDto> = emptyList(),
    val invites: List<JsonFactionInviteDto> = emptyList(),
    val flags: Map<String, Any?> = emptyMap(),
    val prefix: String? = null,
    val home: JsonPositionDto? = null,
    val bonusPower: Double = 0.0,
    val autoclaim: Boolean = false,
    val roles: JsonFactionRolesDto,
    val defaultPermissionsByName: Map<String, Boolean> = emptyMap(),
    val applications: List<JsonFactionApplicationDto> = emptyList()
)

internal data class JsonGateDto(
    val id: String,
    val version: Int,
    val factionId: String,
    val area: JsonCuboidAreaDto,
    val trigger: JsonBlockPositionDto,
    val material: String,
    val status: String
)

// --- domain -> DTO ---

internal fun MfPosition.toDto() = JsonPositionDto(worldId.toString(), x, y, z, yaw, pitch)

internal fun MfBlockPosition.toDto() = JsonBlockPositionDto(worldId.toString(), x, y, z)

internal fun MfCuboidArea.toDto() = JsonCuboidAreaDto(position1.toDto(), position2.toDto())

internal fun MfFactionRole.toDto() = JsonFactionRoleDto(id.value, name, permissionsByName)

internal fun MfFactionRoles.toDto() = JsonFactionRolesDto(defaultRoleId.value, roles.map(MfFactionRole::toDto))

internal fun MfFaction.toDto() = JsonFactionDto(
    id = id.value,
    version = version,
    name = name,
    description = description,
    members = members.map { JsonFactionMemberDto(it.playerId.value, it.role.id.value) },
    invites = invites.map { JsonFactionInviteDto(it.playerId.value) },
    flags = flags.valuesByName,
    prefix = prefix,
    home = home?.toDto(),
    bonusPower = bonusPower,
    autoclaim = autoclaim,
    roles = roles.toDto(),
    defaultPermissionsByName = defaultPermissionsByName,
    applications = applications.map { JsonFactionApplicationDto(it.applicantId.value) }
)

internal fun MfGate.toDto() = JsonGateDto(
    id = id.value,
    version = version,
    factionId = factionId.value,
    area = area.toDto(),
    trigger = trigger.toDto(),
    material = material.name,
    status = status.name
)

// --- DTO -> domain ---

internal fun JsonPositionDto.toDomain() = MfPosition(UUID.fromString(worldId), x, y, z, yaw, pitch)

internal fun JsonBlockPositionDto.toDomain() = MfBlockPosition(UUID.fromString(worldId), x, y, z)

internal fun JsonCuboidAreaDto.toDomain() = MfCuboidArea(position1.toDomain(), position2.toDomain())

internal fun JsonFactionRoleDto.toDomain(plugin: MedievalFactions) =
    MfFactionRole(plugin, MfFactionRoleId(id), name, permissionsByName)

internal fun JsonFactionRolesDto.toDomain(plugin: MedievalFactions) = MfFactionRoles(
    defaultRoleId = MfFactionRoleId(defaultRoleId),
    roles = roles.map { it.toDomain(plugin) }
)

internal fun JsonFactionDto.toDomain(plugin: MedievalFactions): MfFaction {
    val factionRoles = roles.toDomain(plugin)
    val rolesById = factionRoles.roles.associateBy { it.id.value }
    val factionId = MfFactionId(id)
    return MfFaction(
        plugin = plugin,
        id = factionId,
        version = version,
        name = name,
        description = description,
        members = members.mapNotNull { member ->
            val role = rolesById[member.roleId]
                // A hand-edited or partially written file can reference a role that no longer exists.
                // Falling back to the default role keeps the faction loadable instead of failing the
                // whole read; dropping the member entirely would silently remove them from the faction.
                ?: factionRoles.roles.firstOrNull { it.id.value == roles.defaultRoleId }
                ?: run {
                    plugin.logger.severe(
                        "Faction $name ($id) has member ${member.playerId} with unknown role ${member.roleId} " +
                            "and no usable default role; skipping this member."
                    )
                    return@mapNotNull null
                }
            if (rolesById[member.roleId] == null) {
                plugin.logger.warning(
                    "Faction $name ($id) has member ${member.playerId} with unknown role ${member.roleId}; " +
                        "falling back to the default role."
                )
            }
            MfFactionMember(MfPlayerId(member.playerId), role)
        },
        invites = invites.map { MfFactionInvite(MfPlayerId(it.playerId)) },
        flags = MfFlagValues(plugin, flags),
        prefix = prefix,
        home = home?.toDomain(),
        bonusPower = bonusPower,
        autoclaim = autoclaim,
        roles = factionRoles,
        defaultPermissionsByName = defaultPermissionsByName,
        applications = applications.map { MfFactionApplication(factionId, MfPlayerId(it.playerId)) }
    )
}

internal fun JsonGateDto.toDomain(plugin: MedievalFactions): MfGate? {
    val gateMaterial = Material.getMaterial(material)
    if (gateMaterial == null) {
        plugin.logger.severe("Gate $id references unknown material \"$material\"; skipping this gate.")
        return null
    }
    val gateStatus = MfGateStatus.values().firstOrNull { it.name == status }
    if (gateStatus == null) {
        plugin.logger.severe("Gate $id references unknown status \"$status\"; skipping this gate.")
        return null
    }
    return MfGate(
        plugin = plugin,
        id = MfGateId(id),
        version = version,
        factionId = MfFactionId(factionId),
        area = area.toDomain(),
        trigger = trigger.toDomain(),
        material = gateMaterial,
        status = gateStatus
    )
}
