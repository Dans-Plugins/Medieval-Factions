package com.dansplugins.factionsystem.command.faction.grantindependence

import com.dansplugins.factionsystem.MedievalFactions
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class MfFactionGrantIndependenceCommand(private val plugin: MedievalFactions) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {

        return true
    }
}