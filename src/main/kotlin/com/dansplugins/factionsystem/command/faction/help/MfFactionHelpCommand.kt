package com.dansplugins.factionsystem.command.faction.help

import com.dansplugins.factionsystem.MedievalFactions
import org.bukkit.ChatColor.AQUA
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class MfFactionHelpCommand(private val plugin: MedievalFactions) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.help")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionHelpNoPermission"]}")
            return true
        }
        sender.sendMessage("$AQUA${plugin.language["CommandFactionHelpListTitle"]}")

        // more relevant to members
        sender.sendMessage("$AQUA${plugin.language["CommandFactionHelpJoin"]}")
        sender.sendMessage("$AQUA${plugin.language["CommandFactionHelpMembers"]}")
        sender.sendMessage("$AQUA${plugin.language["CommandFactionHelpLawList"]}")

        // more relevant to leaders
        sender.sendMessage("$AQUA${plugin.language["CommandFactionHelpCreate"]}")
        sender.sendMessage("$AQUA${plugin.language["CommandFactionHelpInvite"]}")
        sender.sendMessage("$AQUA${plugin.language["CommandFactionHelpLawAddRemove"]}")
        sender.sendMessage("$AQUA${plugin.language["CommandFactionHelpAlly"]}")
        sender.sendMessage("$AQUA${plugin.language["CommandFactionHelpBreakAlliance"]}")
        sender.sendMessage("$AQUA${plugin.language["CommandFactionHelpDeclareWar"]}")
        sender.sendMessage("$AQUA${plugin.language["CommandFacitonHelpMakePeace"]}")
        return true
    }
}