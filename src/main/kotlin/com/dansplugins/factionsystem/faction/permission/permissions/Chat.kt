package com.dansplugins.factionsystem.faction.permission.permissions

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.chat.MfFactionChatChannel
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission
import com.dansplugins.factionsystem.faction.permission.MfFactionPermissionType
import com.dansplugins.factionsystem.faction.role.MfFactionRoleId

class Chat(private val plugin: MedievalFactions) : MfFactionPermissionType() {
    private val permissions = MfFactionChatChannel.values()
        .associateWith { chatChannel ->
            MfFactionPermission(
                "CHAT(${chatChannel.name})",
                plugin.language["FactionPermissionChat", chatChannel.toString().lowercase()],
                true
            )
        }

    override fun parse(name: String): MfFactionPermission? =
        if (name.matches(Regex("CHAT\\((.+)\\)"))) {
            Regex("CHAT\\((.+)\\)").find(name)
                ?.groupValues?.get(1)
                ?.let(MfFactionChatChannel::valueOf)
                ?.let(permissions::get)
        } else {
            null
        }

    override fun permissionsFor(factionId: MfFactionId, roleIds: List<MfFactionRoleId>): List<MfFactionPermission> {
        return permissions.values.toList()
    }
}
