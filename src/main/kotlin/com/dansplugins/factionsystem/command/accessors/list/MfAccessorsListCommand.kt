package com.dansplugins.factionsystem.command.accessors.list

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.interaction.MfInteractionStatus.CHECKING_ACCESS
import com.dansplugins.factionsystem.pagination.PaginatedView
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.hover.content.Text
import org.bukkit.block.BlockFace
import org.bukkit.block.Chest
import org.bukkit.block.DoubleChest
import org.bukkit.block.data.Bisected
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.logging.Level.SEVERE
import net.md_5.bungee.api.ChatColor as SpigotChatColor
import org.bukkit.ChatColor as BukkitChatColor

class MfAccessorsListCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.checkaccess") && !sender.hasPermission("mf.accessors.list")) {
            sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandAccessorsListNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandAccessorsListNotAPlayer"]}")
            return true
        }
        val lockService = plugin.services.lockService
        if (args.size >= 3) {
            val (x, y, z) = args.take(3).map(String::toIntOrNull)
            if (x != null && y != null && z != null) {
                val block = sender.world.getBlockAt(x, y, z)
                val blockData = block.blockData
                val holder = (block.state as? Chest)?.inventory?.holder
                val blocks = if (blockData is Bisected) {
                    if (blockData.half == Bisected.Half.BOTTOM) {
                        listOf(block, block.getRelative(BlockFace.UP))
                    } else {
                        listOf(block, block.getRelative(BlockFace.DOWN))
                    }
                } else if (holder is DoubleChest) {
                    val left = holder.leftSide as? Chest
                    val right = holder.rightSide as? Chest
                    listOfNotNull(left?.block, right?.block)
                } else {
                    listOf(block)
                }
                val lockedBlocks = blocks.mapNotNull { lockService.getLockedBlock(MfBlockPosition.fromBukkitBlock(it)) }
                val lockedBlock = lockedBlocks.firstOrNull()
                if (lockedBlock == null) {
                    sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandAccessorsListBlockNotLocked"]}")
                    return true
                }
                val pageNumber = if (args.size >= 4) {
                    args.lastOrNull()?.toIntOrNull()?.minus(1) ?: 0
                } else {
                    0
                }
                val playerService = plugin.services.playerService
                val view = PaginatedView(
                    plugin.language,
                    lazy {
                        arrayOf(
                            TextComponent(
                                plugin.language["CommandAccessorsListTitle"]
                            ).apply {
                                color = SpigotChatColor.AQUA
                                isBold = true
                            }
                        )
                    },
                    lockedBlock.accessors.map { accessor ->
                        lazy {
                            val player = playerService.getPlayer(accessor)
                            return@lazy arrayOf(
                                TextComponent("✖ ").apply {
                                    color = SpigotChatColor.RED
                                    clickEvent = ClickEvent(RUN_COMMAND, "/accessors remove ${lockedBlock.block.x} ${lockedBlock.block.y} ${lockedBlock.block.z} ${accessor.value}")
                                    hoverEvent = HoverEvent(SHOW_TEXT, Text(plugin.language["CommandAccessorsListDeleteAccessorButtonHover", player?.toBukkit()?.name ?: plugin.language["UnknownPlayer"]]))
                                },
                                if (player != null) {
                                    TextComponent(
                                        player.toBukkit().name
                                    ).apply {
                                        color = SpigotChatColor.GRAY
                                    }
                                } else {
                                    TextComponent(
                                        "Player not found"
                                    ).apply {
                                        color = SpigotChatColor.RED
                                    }
                                }
                            )
                        }
                    } + lazy {
                        val player = playerService.getPlayer(lockedBlock.playerId)
                        return@lazy arrayOf(
                            if (player != null) {
                                TextComponent(
                                    player.toBukkit().name
                                ).apply {
                                    color = SpigotChatColor.GRAY
                                }
                            } else {
                                TextComponent(
                                    "Player not found"
                                ).apply {
                                    color = SpigotChatColor.RED
                                }
                            }
                        )
                    }
                ) { page -> "/accessors list ${page + 1}" }
                if (pageNumber !in view.pages.indices) {
                    sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandAccessorsListInvalidPageNumber"]}")
                    return true
                }
                view.sendPage(sender, pageNumber)
                if (sender.hasPermission("mf.accessors.add")) {
                    sender.spigot().sendMessage(
                        TextComponent(plugin.language["CommandAccessorsListAddAccessor"]).apply {
                            color = SpigotChatColor.GREEN
                            clickEvent = ClickEvent(RUN_COMMAND, "/accessors add ${lockedBlock.block.x} ${lockedBlock.block.y} ${lockedBlock.block.z}")
                            hoverEvent =
                                HoverEvent(SHOW_TEXT, Text(plugin.language["CommandAccessorsListAddAccessorHover"]))
                        }
                    )
                }
            }
        } else {
            plugin.server.scheduler.runTask(
                plugin,
                Runnable {
                    val playerService = plugin.services.playerService
                    val mfPlayer = playerService.getPlayer(sender)
                        ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                            sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandAccessorsListFailedToSavePlayer"]}")
                            plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                            return@Runnable
                        }
                    val interactionService = plugin.services.interactionService
                    interactionService.setInteractionStatus(mfPlayer.id, CHECKING_ACCESS).onFailure {
                        sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandAccessorsListFailedToSetInteractionStatus"]}")
                        plugin.logger.log(SEVERE, "Failed to set interaction status: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                    sender.sendMessage("${BukkitChatColor.GREEN}${plugin.language["CommandAccessorsListSelectBlock"]}")
                }
            )
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ) = emptyList<String>()
}
