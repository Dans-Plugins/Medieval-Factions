package com.dansplugins.factionsystem.command.faction.apply.tasks

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFactionApplication
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import net.md_5.bungee.api.ChatColor
import org.bukkit.entity.Player
import java.util.logging.Level.SEVERE

class SendApplicationTask(
    private val plugin: MedievalFactions,
    private val sender: Player,
    private val targetFactionName: String
) : Runnable {

    override fun run() {
        val factionService = plugin.services.factionService
        val playerService = plugin.services.playerService
        val mfPlayer = playerService.getPlayer(sender)
            ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionApplyFailedToSavePlayer"]}")
                plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                return
            }
        val faction = factionService.getFaction(mfPlayer.id)
        val target = factionService.getFaction(targetFactionName)
        if (target == null) {
            sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionApplyInvalidTarget"]}")
            return
        }
        if (faction != null) {
            if (target.id.value == faction.id.value) {
                sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionApplyCannotApplyToCurrentFaction"]}")
                return
            }
        }

        factionService.save(target.copy(applications = target.applications + MfFactionApplication(target.id, mfPlayer.id))).onFailure {
            sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionApplyFailedToSaveFaction"]}")
            plugin.logger.log(SEVERE, "Failed to save faction: ${it.reason.message}", it.reason.cause)
            return
        }
    }
}
