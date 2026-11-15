package com.dansplugins.factionsystem.teleport

import com.dansplugins.factionsystem.MedievalFactions
import org.bukkit.ChatColor
import org.bukkit.ChatColor.GRAY
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.util.*

class MfTeleportService internal constructor(
    private val plugin: MedievalFactions,
    private val currentTimeMillis: () -> Long
) {

    constructor(plugin: MedievalFactions) : this(plugin, System::currentTimeMillis)

    private val tasks = mutableMapOf<UUID, BukkitTask>()
    private val lastTeleportTimes = mutableMapOf<UUID, Long>()

    /**
     * Seconds the player still has to wait before another faction home teleport, or 0 when they
     * may teleport now. Governed by `factions.factionHomeTeleportCooldown`; 0 (the default) means
     * no cooldown. The cooldown starts when a teleport completes and is kept in memory only.
     */
    fun getRemainingCooldownSeconds(player: Player): Long {
        val cooldownSeconds = plugin.config.getLong("factions.factionHomeTeleportCooldown")
        if (cooldownSeconds <= 0) return 0
        val lastTeleport = lastTeleportTimes[player.uniqueId] ?: return 0
        val remainingMillis = lastTeleport + cooldownSeconds * 1000 - currentTimeMillis()
        if (remainingMillis <= 0) return 0
        return (remainingMillis + 999) / 1000
    }

    fun teleport(player: Player, location: Location, message: String? = null) {
        val teleportDelay = plugin.config.getInt("factions.factionHomeTeleportDelay")
        if (teleportDelay <= 0) {
            player.teleport(location)
            lastTeleportTimes[player.uniqueId] = currentTimeMillis()
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
                    lastTeleportTimes[uuid] = currentTimeMillis()
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
