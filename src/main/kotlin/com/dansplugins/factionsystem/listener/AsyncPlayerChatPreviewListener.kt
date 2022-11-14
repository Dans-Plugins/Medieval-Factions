package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatPreviewEvent
import java.util.logging.Level
import net.md_5.bungee.api.ChatColor as SpigotChatColor
import org.bukkit.ChatColor as BukkitChatColor

class AsyncPlayerChatPreviewListener(private val plugin: MedievalFactions) : Listener {

    private val isDefaultChatFormattingEnabled = plugin.config.getBoolean("chat.enableDefaultChatFormatting")

    @EventHandler
    fun onAsyncPlayerChatPreview(event: AsyncPlayerChatPreviewEvent) {
        val playerService = plugin.services.playerService
        val factionService = plugin.services.factionService
        val mfPlayer = playerService.getPlayer(event.player)
            ?: playerService.save(MfPlayer(plugin, event.player)).onFailure {
                event.player.sendMessage("${BukkitChatColor.RED}${plugin.language["ChatFailedToSavePlayer"]}")
                plugin.logger.log(Level.SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                return
            }
        val faction = factionService.getFaction(mfPlayer.id) ?: return
        if (mfPlayer.chatChannel != null) {
            event.isCancelled = true
        } else if (isDefaultChatFormattingEnabled) {
            event.format = "${SpigotChatColor.WHITE}[${SpigotChatColor.of(faction.flags[plugin.flags.color])}${faction.prefix ?: faction.name}${SpigotChatColor.WHITE}] %s: %s"
        }
    }

}