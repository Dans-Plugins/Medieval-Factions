package dansplugins.factionsystem.command.faction.create

import dansplugins.factionsystem.MedievalFactions
import dansplugins.factionsystem.faction.MfFaction
import dansplugins.factionsystem.faction.role.MfFactionRoles
import dansplugins.factionsystem.faction.withRole
import dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.logging.Level.SEVERE

class MfFactionCreateCommand(private val plugin: MedievalFactions) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionCreateNotAPlayer"]}")
            return true
        }
        if (!sender.hasPermission("mf.create")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionCreateNoPermission"]}")
            return true
        }
        val maxFactionNameLength = plugin.config.getInt("factions.maxNameLength")
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val playerService = plugin.services.playerService
            val mfPlayer = playerService.getPlayer(sender)
                ?: playerService.save(MfPlayer.fromBukkit(sender)).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandFactionCreateFailedToSavePlayer"]}")
                    return@Runnable
                }
            val factionService = plugin.services.factionService
            val existingFaction = factionService.getFaction(mfPlayer.id)
            if (existingFaction != null) {
                sender.sendMessage("$RED${plugin.language["CommandFactionCreateAlreadyInFaction"]}")
                return@Runnable
            }
            if (args.isEmpty()) {
                sender.sendMessage("$RED${plugin.language["CommandFactionCreateUsage"]}")
                return@Runnable
            }
            val factionName = args.joinToString(" ")
            if (factionName.length > maxFactionNameLength) {
                sender.sendMessage("$RED${plugin.language["CommandFactionCreateNameTooLong", maxFactionNameLength.toString()]}")
                return@Runnable
            }
            if (factionService.getFaction(factionName) != null) {
                sender.sendMessage("$RED${plugin.language["CommandFactionCreateFactionAlreadyExists"]}")
                return@Runnable
            }
            val roles = MfFactionRoles.defaults()
            val owner = roles.single { it.name == "Owner" }
            val faction = MfFaction(plugin, name = factionName, roles = roles, members = listOf(mfPlayer.withRole(owner)))
            val createdFaction = factionService.save(faction).onFailure { failure ->
                sender.sendMessage("$RED${plugin.language["CommandFactionCreateFactionFailedToSave"]}")
                plugin.logger.log(SEVERE, "Failed to save faction: ${failure.reason.message}", failure.reason.cause)
                return@Runnable
            }
            sender.sendMessage("$GREEN${plugin.language["CommandFactionCreateSuccess", createdFaction.name]}")
        })
        return true
    }
}