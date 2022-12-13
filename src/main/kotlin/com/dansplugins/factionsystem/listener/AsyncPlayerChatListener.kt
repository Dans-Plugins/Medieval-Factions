package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import net.md_5.bungee.api.ChatColor
import org.bukkit.ChatColor.RED
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import java.util.logging.Level.SEVERE

class AsyncPlayerChatListener(private val plugin: MedievalFactions) : Listener {

    private val isDefaultChatFormattingEnabled = plugin.config.getBoolean("chat.enableDefaultChatFormatting")

    @EventHandler
    fun onAsyncPlayerChat(event: AsyncPlayerChatEvent) {
        val playerService = plugin.services.playerService
        val factionService = plugin.services.factionService
        val mfPlayer = playerService.getPlayer(event.player)
            ?: playerService.save(MfPlayer(plugin, event.player)).onFailure {
                event.player.sendMessage("$RED${plugin.language["ChatFailedToSavePlayer"]}")
                plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                return
            }
        val faction = factionService.getFaction(mfPlayer.id) ?: return
        if (mfPlayer.chatChannel != null) {
            if (faction.getRole(mfPlayer.id)?.hasPermission(faction, plugin.factionPermissions.chat(mfPlayer.chatChannel)) != true) {
                event.player.sendMessage("$RED${plugin.language["ChatNoFactionPermission", mfPlayer.chatChannel.toString().lowercase()]}")
                event.isCancelled = true
                return
            }
            event.isCancelled = true
            val chatService = plugin.services.chatService
            plugin.server.scheduler.runTask(
                plugin,
                Runnable {
                    chatService.sendMessage(mfPlayer, faction, mfPlayer.chatChannel, event.message)
                }
            )
        } else if (isDefaultChatFormattingEnabled) {
            event.format = "${ChatColor.WHITE}[${ChatColor.of(faction.flags[plugin.flags.color])}${faction.prefix ?: faction.name}${ChatColor.WHITE}] %s: %s"
        }
    }
}
