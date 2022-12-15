package com.dansplugins.factionsystem.command.duel.accept

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfPosition
import com.dansplugins.factionsystem.duel.MfDuel
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.GRAY
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.time.Duration
import java.time.Instant
import java.util.logging.Level
import java.util.logging.Level.SEVERE

class MfDuelAcceptCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.duel")) {
            sender.sendMessage("$RED${plugin.language["CommandDuelAcceptNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandDuelAcceptNotAPlayer"]}")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("$RED${plugin.language["CommandDuelAcceptUsage"]}")
            return true
        }
        val target = plugin.server.getPlayer(args[0])
        if (target == null) {
            sender.sendMessage("$RED${plugin.language["CommandDuelAcceptInvalidTarget"]}")
            return true
        }
        if (sender == target) {
            sender.sendMessage("$RED${plugin.language["CommandDuelAcceptCannotDuelSelf"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandDuelAcceptFailedToSavePlayer"]}")
                        plugin.logger.log(Level.SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val targetMfPlayer = playerService.getPlayer(target)
                    ?: playerService.save(MfPlayer(plugin, target)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandDuelAcceptFailedToSaveTargetPlayer"]}")
                        plugin.logger.log(Level.SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val duelService = plugin.services.duelService
                val existingDuel = duelService.getDuel(mfPlayer.id)
                if (existingDuel != null) {
                    sender.sendMessage("$RED${plugin.language["CommandDuelAcceptAlreadyInADuel"]}")
                    return@Runnable
                }
                val targetExistingDuel = duelService.getDuel(targetMfPlayer.id)
                if (targetExistingDuel != null) {
                    sender.sendMessage("$RED${plugin.language["CommandDuelAcceptTargetAlreadyInDuel"]}")
                    return@Runnable
                }
                val invite = duelService.getInvite(targetMfPlayer.id, mfPlayer.id)
                if (invite == null) {
                    sender.sendMessage("$RED${plugin.language["CommandDuelAcceptNoInvite", target.name]}")
                    return@Runnable
                }
                duelService.deleteInvite(invite.inviterId, invite.inviteeId).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandDuelAcceptFailedToDeleteInvite"]}")
                    plugin.logger.log(SEVERE, "Failed to delete invite", it.reason.cause)
                    return@Runnable
                }
                val duel = duelService.save(
                    MfDuel(
                        challengerId = invite.inviterId,
                        challengedId = invite.inviteeId,
                        challengerHealth = target.health,
                        challengedHealth = sender.health,
                        endTime = Instant.now().plus(Duration.parse(plugin.config.getString("duels.duration"))),
                        challengerLocation = MfPosition.fromBukkitLocation(target.location),
                        challengedLocation = MfPosition.fromBukkitLocation(sender.location)
                    )
                ).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandDuelAcceptFailedToSaveDuel"]}")
                    plugin.logger.log(SEVERE, "Failed to save duel: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                sender.sendMessage("$GREEN${plugin.language["CommandDuelAcceptSuccess", target.name]}")
                target.sendMessage("$GREEN${plugin.language["CommandDuelAcceptChallengeAccepted", sender.name]}")
                plugin.server.scheduler.runTask(
                    plugin,
                    Runnable {
                        sender.health = sender.getAttribute(GENERIC_MAX_HEALTH)?.value ?: sender.health
                        target.health = target.getAttribute(GENERIC_MAX_HEALTH)?.value ?: target.health

                        val bar = plugin.server.createBossBar(
                            NamespacedKey(plugin, "duel_${duel.id.value}"),
                            "${target.name} vs ${sender.name}",
                            BarColor.WHITE,
                            BarStyle.SEGMENTED_20
                        )
                        bar.progress = 1.0
                        bar.addPlayer(sender)
                        bar.addPlayer(target)

                        val notificationDistance = plugin.config.getInt("duels.notificationDistance")
                        val notificationDistanceSquared = notificationDistance * notificationDistance
                        val nearbyPlayers = mutableSetOf<Player>()
                        nearbyPlayers += sender.world.players.filter {
                            it.location.distanceSquared(sender.location) <= notificationDistanceSquared
                        }
                        nearbyPlayers += target.world.players.filter {
                            it.location.distanceSquared(target.location) <= notificationDistanceSquared
                        }
                        nearbyPlayers.forEach { notifiedPlayer ->
                            notifiedPlayer.sendMessage("$GRAY${plugin.language["CommandDuelAcceptNotification", target.name, sender.name]}")
                        }
                    }
                )
            }
        )
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        if (sender !is Player) return emptyList()
        val senderMfPlayerId = MfPlayerId.fromBukkitPlayer(sender)
        val duelService = plugin.services.duelService
        return when {
            args.isEmpty() -> plugin.server.onlinePlayers.filter { bukkitPlayer ->
                duelService.getInvitesByInvitee(senderMfPlayerId).any {
                    it.inviterId == MfPlayerId.fromBukkitPlayer(bukkitPlayer)
                }
            }.map(Player::getName)
            args.size == 1 -> plugin.server.onlinePlayers.filter { bukkitPlayer ->
                if (!bukkitPlayer.name.lowercase().startsWith(args[0].lowercase())) return@filter false
                duelService.getInvitesByInvitee(senderMfPlayerId).any {
                    it.inviterId == MfPlayerId.fromBukkitPlayer(bukkitPlayer)
                }
            }.map(Player::getName)
            else -> emptyList()
        }
    }
}
