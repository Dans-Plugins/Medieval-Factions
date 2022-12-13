package com.dansplugins.factionsystem.teleport

import com.dansplugins.factionsystem.MedievalFactions
import org.bukkit.ChatColor
import org.bukkit.ChatColor.GRAY
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.util.*

class MfTeleportService(private val plugin: MedievalFactions) {

    private val tasks = mutableMapOf<UUID, BukkitTask>()

    fun teleport(player: Player, location: Location, message: String? = null) {
        val teleportDelay = plugin.config.getInt("factions.factionHomeTeleportDelay")
        if (teleportDelay <= 0) {
            player.teleport(location)
            player.sendMessage(message)
            return
        }
        val uuid = player.uniqueId
        tasks[uuid]?.cancel()
        player.sendMessage("$GRAY${plugin.language["Teleporting", teleportDelay.toString()]}")
        val task = plugin.server.scheduler.runTaskLater(
            plugin,
            Runnable {
                tasks.remove(uuid)
                val playerToTeleport = plugin.server.getPlayer(uuid)
                if (playerToTeleport != null) {
                    playerToTeleport.teleport(location)
                    if (message != null) playerToTeleport.sendMessage(message)
                }
            },
            teleportDelay * 20L
        )
        tasks[uuid] = task
    }

    fun cancelTeleportation(player: Player) {
        val task = tasks[player.uniqueId]
        if (task != null) {
            task.cancel()
            tasks.remove(player.uniqueId)
            player.sendMessage("${ChatColor.RED}${plugin.language["TeleportationCancelled"]}")
        }
    }
}
