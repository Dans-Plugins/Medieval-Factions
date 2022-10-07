package com.dansplugins.factionsystem.chat

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.chat.MfFactionChatChannel.*
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.ALLY
import java.util.concurrent.ConcurrentHashMap
import net.md_5.bungee.api.ChatColor as SpigotChatColor
import org.bukkit.ChatColor as BukkitChatColor

class MfChatService(private val plugin: MedievalFactions) {

    private val formats = ConcurrentHashMap(MfFactionChatChannel.values().associateWith { plugin.config.getString("chat.${it.toString().lowercase()}.format") })

    fun sendMessage(mfPlayer: MfPlayer, faction: MfFaction, channel: MfFactionChatChannel, message: String) {
        val bukkitPlayer = mfPlayer.toBukkit()
        val name = bukkitPlayer.name ?: plugin.language["UnknownPlayer"]
        val displayName = bukkitPlayer.player?.displayName ?: bukkitPlayer.name ?: plugin.language["UnknownPlayer"]
        val factionService = plugin.services.factionService
        val relationshipService = plugin.services.factionRelationshipService
        val formattedMessage = (formats[channel] ?: when (mfPlayer.chatChannel) {
            FACTION -> "&7[faction] [\${factionColor}\${faction}&7] [\${role}] &f\${displayName}: \${message}"
            VASSALS -> "&7[vassals] [\${factionColor}\${faction}&7] [\${role}] &f\${displayName}: \${message}"
            ALLIES -> "&7[allies] [\${factionColor}\${faction}&7] [\${role}] &f\${displayName}: \${message}"
            null -> ""
        })
            .replace("\${factionColor}", SpigotChatColor.of(faction.flags[plugin.flags.color]).toString())
            .replace("\${faction}", faction.name)
            .replace("\${role}", faction.getRole(mfPlayer.id)?.name ?: plugin.language["NoRole"])
            .replace("\${name}", name)
            .replace("\${displayName}", displayName)
            .replace("\${message}", message)
            .let { BukkitChatColor.translateAlternateColorCodes('&', it) }
        val recipients = when (channel) {
            FACTION -> faction.members.mapNotNull { it.playerId.toBukkitPlayer().player }
            VASSALS -> (faction.members + relationshipService.getVassalTree(faction.id)
                .mapNotNull { factionService.getFaction(it) }
                .flatMap { vassal -> vassal.members }).mapNotNull { member -> member.playerId.toBukkitPlayer().player }
            ALLIES -> (faction.members + relationshipService.getRelationships(faction.id, ALLY)
                .mapNotNull { relationship ->
                    val reverseRelationships =
                        relationshipService.getRelationships(relationship.targetId, relationship.factionId)
                    return@mapNotNull if (reverseRelationships.none { it.type == ALLY }) {
                        null
                    } else {
                        factionService.getFaction(relationship.targetId)
                    }
                }.flatMap { it.members }).mapNotNull { it.playerId.toBukkitPlayer().player }
        }
        recipients.forEach { it.sendMessage(bukkitPlayer.uniqueId, formattedMessage) }
    }

}