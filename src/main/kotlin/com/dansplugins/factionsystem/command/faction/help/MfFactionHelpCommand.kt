package com.dansplugins.factionsystem.command.faction.help

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFactionMember
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.logging.Level

class MfFactionHelpCommand(private val plugin: MedievalFactions) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.help")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionHelpNoPermission"]}");
            return true
        }
        sender.sendMessage("$GREEN${plugin.language["CommandFactionHelpListTitle"]}");

        // more relevant to members
        sender.sendMessage("$GREEN${plugin.language["CommandFactionHelpJoin"]}");
        sender.sendMessage("$GREEN${plugin.language["CommandFactionHelpLawList"]}");

        // more relevant to leaders
        sender.sendMessage("$GREEN${plugin.language["CommandFactionHelpCreate"]}");
        sender.sendMessage("$GREEN${plugin.language["CommandFactionHelpInvite"]}");
        sender.sendMessage("$GREEN${plugin.language["CommandFactionHelpLawAddRemove"]}");
        sender.sendMessage("$GREEN${plugin.language["CommandFactionHelpAlly"]}");
        sender.sendMessage("$GREEN${plugin.language["CommandFactionHelpBreakAlliance"]}");
        sender.sendMessage("$GREEN${plugin.language["CommandFactionHelpDeclareWar"]}");
        return true
    }
}