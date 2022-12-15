package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.Material.PLAYER_HEAD
import org.bukkit.NamespacedKey
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.util.logging.Level

class EntityDamageListener(private val plugin: MedievalFactions) : Listener {

    @EventHandler
    fun onEntityDamage(event: EntityDamageEvent) {
        val entity = event.entity
        if (entity is Player) {
            val playerService = plugin.services.playerService
            val mfPlayer = playerService.getPlayer(entity) ?: MfPlayer(plugin, entity)

            val teleportService = plugin.services.teleportService
            teleportService.cancelTeleportation(entity)

            val duelService = plugin.services.duelService
            val duel = duelService.getDuel(mfPlayer.id)
            if (duel != null && entity.health - event.finalDamage <= 0) {
                event.isCancelled = true
                plugin.server.getBossBar(NamespacedKey(plugin, "duel_${duel.id.value}"))?.removeAll()
                plugin.server.removeBossBar(NamespacedKey(plugin, "duel_${duel.id.value}"))
                val notificationDistance = plugin.config.getInt("duels.notificationDistance")
                val notificationDistanceSquared = notificationDistance * notificationDistance
                val challengerBukkitPlayer = duel.challengerId.toBukkitPlayer().player
                val nearbyPlayers = mutableSetOf<Player>()
                if (challengerBukkitPlayer != null) {
                    challengerBukkitPlayer.activePotionEffects.clear()
                    challengerBukkitPlayer.fireTicks = 0
                    challengerBukkitPlayer.health = duel.challengerHealth
                    duel.challengerLocation?.toBukkitLocation()?.let(challengerBukkitPlayer::teleport)
                    nearbyPlayers += challengerBukkitPlayer.world.players
                        .filter { it.location.distanceSquared(challengerBukkitPlayer.location) <= notificationDistanceSquared }
                }
                val challengedBukkitPlayer = duel.challengedId.toBukkitPlayer().player
                if (challengedBukkitPlayer != null) {
                    challengedBukkitPlayer.activePotionEffects.clear()
                    challengedBukkitPlayer.fireTicks = 0
                    challengedBukkitPlayer.health = duel.challengedHealth
                    duel.challengedLocation?.toBukkitLocation()?.let(challengedBukkitPlayer::teleport)
                    nearbyPlayers += challengedBukkitPlayer.world.players
                        .filter { it.location.distanceSquared(challengedBukkitPlayer.location) <= notificationDistanceSquared }
                }
                if (mfPlayer.id == duel.challengerId) {
                    if (challengedBukkitPlayer != null) {
                        challengedBukkitPlayer.inventory
                            .addItem(getHead(duel.challengerId.toBukkitPlayer(), duel.challengedId.toBukkitPlayer()))
                            .values
                            .forEach { item ->
                                challengedBukkitPlayer.world.dropItem(challengedBukkitPlayer.location, item)
                            }
                    }
                    nearbyPlayers.forEach { notifiedPlayer ->
                        notifiedPlayer.sendMessage(
                            plugin.language[
                                "DuelWin",
                                duel.challengedId.toBukkitPlayer().name ?: plugin.language["UnknownPlayer"],
                                duel.challengerId.toBukkitPlayer().name ?: plugin.language["UnknownPlayer"]
                            ]
                        )
                    }
                } else {
                    if (challengerBukkitPlayer != null) {
                        challengerBukkitPlayer.inventory
                            .addItem(getHead(duel.challengedId.toBukkitPlayer(), duel.challengerId.toBukkitPlayer()))
                            .values
                            .forEach { item ->
                                challengerBukkitPlayer.world.dropItem(challengerBukkitPlayer.location, item)
                            }
                    }
                    nearbyPlayers.forEach { notifiedPlayer ->
                        notifiedPlayer.sendMessage(
                            plugin.language[
                                "DuelWin",
                                duel.challengerId.toBukkitPlayer().name ?: plugin.language["UnknownPlayer"],
                                duel.challengedId.toBukkitPlayer().name ?: plugin.language["UnknownPlayer"]
                            ]
                        )
                    }
                }
                plugin.server.scheduler.runTaskAsynchronously(
                    plugin,
                    Runnable {
                        duelService.delete(duel.id).onFailure {
                            plugin.logger.log(Level.SEVERE, "Failed to delete duel: ${it.reason.message}", it.reason.cause)
                            return@Runnable
                        }
                    }
                )
            }
        }
    }

    private fun getHead(bukkitPlayer: OfflinePlayer, claimant: OfflinePlayer): ItemStack {
        val item = ItemStack(PLAYER_HEAD)
        val meta = (item.itemMeta ?: plugin.server.itemFactory.getItemMeta(PLAYER_HEAD)) as? SkullMeta
        if (meta != null) {
            meta.setDisplayName("${bukkitPlayer.name ?: plugin.language["UnknownPlayer"]}'s head")
            meta.owningPlayer = bukkitPlayer
            meta.lore = listOf(
                "Lost in a duel against ${claimant.name ?: plugin.language["UnknownPlayer"]}"
            )
        }
        item.itemMeta = meta
        return item
    }
}
