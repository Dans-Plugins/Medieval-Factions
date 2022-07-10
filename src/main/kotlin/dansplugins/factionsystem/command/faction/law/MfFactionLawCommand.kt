package dansplugins.factionsystem.command.faction.law

import dansplugins.factionsystem.MedievalFactions
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class MfFactionLawCommand(private val plugin: MedievalFactions) : CommandExecutor {

    private val factionLawAddCommand = MfFactionLawAddCommand(plugin)
    private val factionLawRemoveCommand = MfFactionLawRemoveCommand(plugin)
    private val factionLawListCommand = MfFactionLawListCommand(plugin)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionLawUsage"]}")
            return true
        }
        return when (args[0].lowercase()) {
            "add", "create", "new", "a", "c", "n" -> factionLawAddCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "remove", "delete", "rm", "r", "d" -> factionLawRemoveCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            "list", "ls", "l" -> factionLawListCommand.onCommand(sender, command, label, args.drop(1).toTypedArray())
            else -> {
                sender.sendMessage("${ChatColor.RED}${plugin.language["CommandFactionLawUsage"]}")
                true
            }
        }
    }
}