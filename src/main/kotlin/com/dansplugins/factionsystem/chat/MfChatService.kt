package com.dansplugins.factionsystem.chat

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.chat.MfFactionChatChannel.ALLIES
import com.dansplugins.factionsystem.chat.MfFactionChatChannel.FACTION
import com.dansplugins.factionsystem.chat.MfFactionChatChannel.VASSALS
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.ALLY
import net.md_5.bungee.api.ChatColor
import java.time.Instant

class MfChatService(private val plugin: MedievalFactions, private val repo: MfChatChannelMessageRepository) {

    fun sendMessage(mfPlayer: MfPlayer, faction: MfFaction, channel: MfFactionChatChannel, message: String) {
        val bukkitPlayer = mfPlayer.toBukkit()
        val name = bukkitPlayer.name ?: plugin.language["UnknownPlayer"]
        val displayName = bukkitPlayer.player?.displayName ?: bukkitPlayer.name ?: plugin.language["UnknownPlayer"]
        val factionService = plugin.services.factionService
        val relationshipService = plugin.services.factionRelationshipService
        val formattedMessage = (
            plugin.config.getString("chat.${channel.toString().lowercase()}.format") ?: when (mfPlayer.chatChannel) {
                FACTION -> "&7[faction] [\${factionColor}\${faction}&7] [\${role}] &f\${displayName}: \${message}"
                VASSALS -> "&7[vassals] [\${factionColor}\${faction}&7] [\${role}] &f\${displayName}: \${message}"
                ALLIES -> "&7[allies] [\${factionColor}\${faction}&7] [\${role}] &f\${displayName}: \${message}"
                null -> ""
            }
            )
            .replace("\${factionColor}", ChatColor.of(faction.flags[plugin.flags.color]).toString())
            .replace("\${faction}", faction.name)
            .replace("\${role}", faction.getRole(mfPlayer.id)?.name ?: plugin.language["NoRole"])
            .replace("\${name}", name)
            .replace("\${displayName}", displayName)
            .replace("\${message}", message)
            .let { ChatColor.translateAlternateColorCodes('&', it) }
        val recipients = when (channel) {
            FACTION -> faction.members.mapNotNull { it.playerId.toBukkitPlayer().player }
            VASSALS -> {
                val topLiegeId = relationshipService.getLiegeChain(faction.id).last().factionId
                val topLiege = factionService.getFaction(topLiegeId)
                (
                    (topLiege?.members ?: emptyList()) + relationshipService.getVassalTree(topLiegeId)
                        .mapNotNull { factionService.getFaction(it) }
                        .flatMap { vassal -> vassal.members }
                    ).mapNotNull { member -> member.playerId.toBukkitPlayer().player }
            }
            ALLIES -> (
                faction.members + relationshipService.getRelationships(faction.id, ALLY)
                    .mapNotNull { relationship ->
                        val reverseRelationships =
                            relationshipService.getRelationships(relationship.targetId, relationship.factionId)
                        return@mapNotNull if (reverseRelationships.none { it.type == ALLY }) {
                            null
                        } else {
                            factionService.getFaction(relationship.targetId)
                        }
                    }.flatMap { it.members }
                ).mapNotNull { it.playerId.toBukkitPlayer().player }
        }
        recipients.forEach { it.sendMessage(formattedMessage) }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                repo.insert(MfChatChannelMessage(Instant.now(), mfPlayer.id, faction.id, channel, message))
            }
        )
        // Using console sender means that colour codes will come through in console
        // It doesn't automatically give the [MedievalFactions] prefix like with the plugin's logger though
        // If we want to use the plugin's logger we could do something like ChatColor.stripColor.
        plugin.server.consoleSender.sendMessage(formattedMessage)
    }

    @JvmName("getChatChannelMessagesByFactionId")
    fun getChatChannelMessages(factionId: MfFactionId): List<MfChatChannelMessage> {
        return repo.getChatChannelMessages(factionId)
    }

    @JvmName("getChatChannelMessagesByFactionId")
    fun getChatChannelMessages(factionId: MfFactionId, limit: Int, offset: Int = 0): List<MfChatChannelMessage> {
        return repo.getChatChannelMessages(factionId, limit, offset)
    }

    @JvmName("getChatChannelMessageCountByFactionId")
    fun getChatChannelMessageCount(factionId: MfFactionId): Int {
        return repo.getChatChannelMessageCount(factionId)
    }
}
