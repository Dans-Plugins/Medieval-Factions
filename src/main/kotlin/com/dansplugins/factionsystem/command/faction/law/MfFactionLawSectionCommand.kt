package com.dansplugins.factionsystem.command.faction.law

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.law.MfLaw
import com.dansplugins.factionsystem.law.MfLawId
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.logging.Level

/**
 * `/faction law section [number] [section]` puts a law under a free-text section, and
 * `/faction law section [number]` takes it out of its section again (#1650).
 *
 * Sections only group laws in `/faction law list`; law numbers stay global across sections.
 */
class MfFactionLawSectionCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.law.section")) {
            sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionLawSectionNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionLawSectionNotAPlayer"]}")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionLawSectionUsage"]}")
            return true
        }
        val section = parseSection(args.drop(1))
        if (section != null && section.length > MfLaw.MAX_SECTION_LENGTH) {
            sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionLawSectionTooLong", MfLaw.MAX_SECTION_LENGTH.toString()]}")
            return true
        }
        setSection(sender, args[0], section)
        return true
    }

    private fun setSection(player: Player, lawReference: String, section: String?) {
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(player)
                    ?: playerService.save(MfPlayer(plugin, player)).onFailure {
                        player.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionLawSectionFailedToSavePlayer"]}")
                        plugin.logger.log(Level.SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                val faction = factionService.getFaction(mfPlayer.id)
                if (faction == null) {
                    player.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionLawSectionMustBeInAFaction"]}")
                    return@Runnable
                }
                val role = faction.getRole(mfPlayer.id)
                if (role == null || !role.hasPermission(faction, plugin.factionPermissions.editLaw)) {
                    player.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionLawSectionNoFactionPermission"]}")
                    return@Runnable
                }
                val lawService = plugin.services.lawService
                val law = lawService.getLaw(MfLawId(lawReference)) ?: lawService.getLaw(faction.id, lawReference.toIntOrNull())
                if (law == null) {
                    player.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionLawSectionNotNumberOrId"]}")
                    return@Runnable
                }
                if (law.factionId != faction.id) {
                    player.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionLawSectionNotYourFaction"]}")
                    return@Runnable
                }
                val saved = lawService.save(law.copy(section = section))
                    .onFailure {
                        player.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionLawSectionFailedToSave"]}")
                        plugin.logger.log(Level.SEVERE, "Failed to save law section: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val number = (saved.number ?: law.number)?.toString() ?: lawReference
                if (section == null) {
                    player.sendMessage("${ChatColor.GREEN}${plugin.language["CommandFactionLawSectionCleared", number]}")
                } else {
                    player.sendMessage("${ChatColor.GREEN}${plugin.language["CommandFactionLawSectionSet", number, section]}")
                }
            }
        )
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ) = emptyList<String>()

    internal companion object {
        /** Joins the words after the law into a section name; no words, or only spaces, clears the section. */
        fun parseSection(words: List<String>): String? = words.joinToString(" ").trim().ifEmpty { null }
    }
}
