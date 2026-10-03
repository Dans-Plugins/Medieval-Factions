package com.dansplugins.factionsystem.command.faction.unclaimall

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.command.unquote
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.hover.content.Text
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.logging.Level.SEVERE
import net.md_5.bungee.api.ChatColor as SpigotChatColor

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
        // Removing every claim is not undoable, so the first invocation only reports what would be removed and offers
        // a [Confirm] button that re-runs the command with a trailing `confirm`, the same click-to-confirm pattern
        // /f addmember and /f join use for their destructive paths.
        val confirmed = args.lastOrNull()?.equals(CONFIRM_ARG, ignoreCase = true) == true
        val factionArgs = if (confirmed) args.dropLast(1).toTypedArray() else args
        val hasForcePermission = sender.hasPermission("mf.force.unclaim")
        val targetFactionName = if (hasForcePermission && factionArgs.isNotEmpty()) factionArgs.unquote().joinToString(" ") else null
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
                if (!confirmed) {
                    val claimCount = claimService.getClaims(faction.id).size
                    if (claimCount == 0) {
                        sender.sendMessage("$RED${plugin.language["CommandFactionUnclaimAllNoClaims", faction.name]}")
                        return@Runnable
                    }
                    // The admin form names the faction by ID so the confirmation cannot resolve to a different
                    // faction, whatever its name contains.
                    val confirmCommand = if (targetFactionName != null) {
                        "/faction unclaimall ${faction.id.value} $CONFIRM_ARG"
                    } else {
                        "/faction unclaimall $CONFIRM_ARG"
                    }
                    sendConfirmation(sender, faction.name, claimCount, confirmCommand)
                    return@Runnable
                }
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

    private fun sendConfirmation(player: Player, factionName: String, claimCount: Int, confirmCommand: String) {
        player.sendMessage("$RED${plugin.language["CommandFactionUnclaimAllConfirm", claimCount.toString(), factionName, confirmCommand]}")
        player.spigot().sendMessage(
            TextComponent(plugin.language["CommandFactionUnclaimAllConfirmButton"]).apply {
                color = SpigotChatColor.GREEN
                isBold = true
                hoverEvent = HoverEvent(SHOW_TEXT, Text(plugin.language["CommandFactionUnclaimAllConfirmButtonHover", claimCount.toString(), factionName]))
                clickEvent = ClickEvent(RUN_COMMAND, confirmCommand)
            }
        )
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ) = emptyList<String>()

    companion object {
        private const val CONFIRM_ARG = "confirm"
    }
}
