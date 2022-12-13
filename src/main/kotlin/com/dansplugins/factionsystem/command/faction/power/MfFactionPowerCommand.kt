package com.dansplugins.factionsystem.command.faction.power

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.AQUA
import org.bukkit.ChatColor.BOLD
import org.bukkit.ChatColor.GRAY
import org.bukkit.ChatColor.RED
import org.bukkit.OfflinePlayer
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.logging.Level.SEVERE
import kotlin.math.floor

class MfFactionPowerCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    private val decimalFormat = DecimalFormat("0", DecimalFormatSymbols.getInstance(plugin.language.locale))

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.power")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionPowerNoPermission"]}")
            return true
        }
        val hasViewOtherPermission = sender.hasPermission("mf.power.view.other")
        if (sender !is Player && !hasViewOtherPermission) {
            sender.sendMessage("$RED${plugin.language["CommandFactionPowerNotAPlayer"]}")
            return true
        }
        var targetPlayer: OfflinePlayer?
        if (hasViewOtherPermission && args.isNotEmpty()) {
            targetPlayer = plugin.server.getOfflinePlayer(args.first())
            if (!targetPlayer.isOnline && !targetPlayer.hasPlayedBefore()) {
                targetPlayer = null
            }
        } else if (sender is Player) {
            targetPlayer = sender
        } else {
            targetPlayer = null
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val factionService = plugin.services.factionService
                var mfPlayer: MfPlayer? = null
                var faction: MfFaction?
                when {
                    hasViewOtherPermission && args.isNotEmpty() -> {
                        faction = factionService.getFaction(args.joinToString(" "))
                        if (faction == null) {
                            if (targetPlayer != null) {
                                mfPlayer = playerService.getPlayer(targetPlayer)
                                    ?: playerService.save(MfPlayer(plugin, targetPlayer)).onFailure {
                                        sender.sendMessage("$RED${plugin.language["CommandFactionPowerFailedToSavePlayer"]}")
                                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                                        return@Runnable
                                    }
                                faction = factionService.getFaction(mfPlayer.id)
                            }
                        }
                    }
                    sender is Player -> {
                        mfPlayer = playerService.getPlayer(sender)
                            ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                                sender.sendMessage("$RED${plugin.language["CommandFactionPowerFailedToSavePlayer"]}")
                                plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                                return@Runnable
                            }
                        faction = factionService.getFaction(mfPlayer.id)
                    }
                    else -> {
                        sender.sendMessage("$RED${plugin.language["CommandFactionPowerNotAPlayer"]}")
                        return@Runnable
                    }
                }

                if (mfPlayer != null || faction != null) {
                    sender.sendMessage("$AQUA$BOLD${plugin.language["CommandFactionPowerTitle"]}")
                    if (mfPlayer != null) {
                        if (targetPlayer == sender) {
                            sender.sendMessage(
                                "$GRAY${
                                plugin.language[
                                    "CommandFactionPowerPlayerPower",
                                    decimalFormat.format(floor(mfPlayer.power)),
                                    decimalFormat.format(floor(plugin.config.getDouble("players.maxPower")))
                                ]
                                }"
                            )
                        } else {
                            sender.sendMessage(
                                "$GRAY${
                                plugin.language[
                                    "CommandFactionPowerOtherPlayerPower",
                                    targetPlayer?.name ?: plugin.language["UnknownPlayer"],
                                    decimalFormat.format(floor(mfPlayer.power)),
                                    decimalFormat.format(floor(plugin.config.getDouble("players.maxPower")))
                                ]
                                }"
                            )
                        }
                    }
                    if (faction != null) {
                        sender.sendMessage(
                            "$GRAY${
                            plugin.language[
                                "CommandFactionPowerFactionPower",
                                decimalFormat.format(floor(faction.power)),
                                decimalFormat.format(floor(faction.maxPower)),
                                decimalFormat.format(floor(faction.memberPower)),
                                decimalFormat.format(floor(faction.maxMemberPower)),
                                decimalFormat.format(floor(faction.vassalPower)),
                                decimalFormat.format(floor(faction.maxVassalPower)),
                                decimalFormat.format(if (faction.flags[plugin.flags.acceptBonusPower]) floor(faction.bonusPower) else 0)
                            ]
                            }"
                        )
                    }
                } else {
                    sender.sendMessage("$RED${plugin.language["CommandFactionPowerNotAPlayer"]}")
                    return@Runnable
                }
            }
        )
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ) = when {
        args.isEmpty() -> plugin.services.factionService.factions.map(MfFaction::name) +
            plugin.server.offlinePlayers.mapNotNull(OfflinePlayer::getName)
        args.size == 1 -> (
            plugin.services.factionService.factions.map(MfFaction::name) +
                plugin.server.offlinePlayers.mapNotNull(OfflinePlayer::getName)
            )
            .filter { it.lowercase().startsWith(args[0].lowercase()) }
        else -> emptyList()
    }
}
