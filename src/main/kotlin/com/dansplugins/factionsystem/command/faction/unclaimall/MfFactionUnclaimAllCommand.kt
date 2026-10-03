package com.dansplugins.factionsystem.command.faction.unclaimall

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.command.unquote
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.logging.Level.SEVERE

class MfFactionUnclaimAllCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.unclaimall")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionUnclaimAllNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionUnclaimAllNotAPlayer"]}")
            return true
        }
        // With mf.force.unclaim, a faction name or ID may be given to unclaim all of that faction's land
        // (/f unclaimall <faction>). Without the permission, any arguments are ignored, as before.
        val hasForcePermission = sender.hasPermission("mf.force.unclaim")
        val targetFactionName = if (hasForcePermission && args.isNotEmpty()) args.unquote().joinToString(" ") else null
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionUnclaimAllFailedToSavePlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                val faction = if (targetFactionName != null) {
                    // An explicitly named faction is only reachable with mf.force.unclaim, which replaces the
                    // faction's own role check, matching how mf.force.flag is honoured. A name that does not resolve
                    // is refused rather than falling back to the sender's own faction.
                    factionService.getFaction(MfFactionId(targetFactionName)) ?: factionService.getFaction(targetFactionName) ?: run {
                        sender.sendMessage("$RED${plugin.language["CommandFactionUnclaimAllInvalidFaction", targetFactionName]}")
                        return@Runnable
                    }
                } else {
                    val ownFaction = factionService.getFaction(mfPlayer.id)
                    if (ownFaction == null) {
                        sender.sendMessage("$RED${plugin.language["CommandFactionUnclaimAllMustBeInAFaction"]}")
                        return@Runnable
                    }
                    val role = ownFaction.getRole(mfPlayer.id)
                    if (role == null || !role.hasPermission(ownFaction, plugin.factionPermissions.unclaim)) {
                        sender.sendMessage("$RED${plugin.language["CommandFactionUnclaimAllNoFactionPermission"]}")
                        return@Runnable
                    }
                    ownFaction
                }
                val claimService = plugin.services.claimService
                claimService.deleteAllClaims(faction.id).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandFactionUnclaimAllFailedToDeleteClaims"]}")
                    plugin.logger.log(SEVERE, "Failed to delete all claims: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                sender.sendMessage("$GREEN${plugin.language["CommandFactionUnclaimAllSuccess"]}")

                val mapService = plugin.services.mapService
                if (mapService != null && !plugin.config.getBoolean("dynmap.onlyRenderTerritoriesUponStartup")) {
                    plugin.server.scheduler.runTask(
                        plugin,
                        Runnable {
                            mapService.scheduleUpdateClaims(faction)
                        }
                    )
                }
            }
        )
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ) = emptyList<String>()
}
