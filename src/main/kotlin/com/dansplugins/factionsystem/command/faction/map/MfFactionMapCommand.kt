package com.dansplugins.factionsystem.command.faction.map

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.command.faction.map.MfFactionMapCommand.MapType.DIPLOMATIC
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.ALLY
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.AT_WAR
import dev.forkhandles.result4k.onFailure
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.hover.content.Text
import org.bukkit.World
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.logging.Level
import net.md_5.bungee.api.ChatColor as SpigotChatColor
import org.bukkit.ChatColor as BukkitChatColor

class MfFactionMapCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {

    enum class MapType(val supportsFactionless: Boolean) {
        NORMAL(true),
        DIPLOMATIC(false)
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.map")) {
            sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionMapNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionMapNotAPlayer"]}")
            return true
        }
        val mapType = if (args.isEmpty()) {
            MapType.NORMAL
        } else {
            MapType.valueOf(args.joinToString(" ").uppercase())
        }
        val senderChunk = sender.location.chunk
        val senderChunkX = senderChunk.x
        val senderChunkZ = senderChunk.z
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionMapFailedToSavePlayer"]}")
                        plugin.logger.log(Level.SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                val faction = factionService.getFaction(mfPlayer.id)
                if (faction == null && !mapType.supportsFactionless) {
                    sender.sendMessage("${BukkitChatColor.RED}${plugin.language["CommandFactionMapMapTypeRequiresFaction"]}")
                    return@Runnable
                }
                val map = renderMap(faction, mapType, sender.world, senderChunkX - 10, senderChunkZ - 4, senderChunkX + 10, senderChunkZ + 4)
                map.forEach { row ->
                    sender.spigot().sendMessage(*row)
                }
                if (mapType == DIPLOMATIC) {
                    sender.sendMessage(
                        "${BukkitChatColor.GRAY}${plugin.language["FactionMapKey"]} " +
                            "${BukkitChatColor.GREEN}■ ${plugin.language["FactionMapYourFaction"]} " +
                            "${BukkitChatColor.RED}■ ${plugin.language["FactionMapEnemy"]} " +
                            "${BukkitChatColor.BLUE}■ ${plugin.language["FactionMapAlly"]} " +
                            "${BukkitChatColor.DARK_GREEN}■ ${plugin.language["FactionMapVassal"]} " +
                            "${BukkitChatColor.YELLOW}■ ${plugin.language["FactionMapLiege"]} " +
                            "${BukkitChatColor.WHITE}■ ${plugin.language["FactionMapNeutral"]}"
                    )
                }
            }
        )
        return true
    }

    private fun renderMap(viewerFaction: MfFaction?, mapType: MapType, world: World, minX: Int, minZ: Int, maxX: Int, maxZ: Int): List<Array<out BaseComponent>> {
        val claimService = plugin.services.claimService
        val factionService = plugin.services.factionService
        return (minZ..maxZ).map { z ->
            (minX..maxX).map { x ->
                val claim = claimService.getClaim(world, x, z)
                val faction = claim?.factionId?.let(factionService::getFaction)
                val color = getColor(viewerFaction, faction, mapType)
                TextComponent(if (x == (minX + maxX) / 2 && z == (minZ + maxZ) / 2) "\u2b1c" else "\u2b1b").apply {
                    this.color = color
                    hoverEvent = HoverEvent(
                        SHOW_TEXT,
                        Text(
                            arrayOf(
                                if (faction != null) {
                                    TextComponent(faction.name).apply {
                                        this.color = color
                                    }
                                } else {
                                    TextComponent(plugin.language["Wilderness"]).apply {
                                        this.color = SpigotChatColor.of(plugin.config.getString("wilderness.color"))
                                    }
                                }
                            )
                        )
                    )
                }
            }.toTypedArray()
        }
    }

    private fun getColor(viewer: MfFaction?, faction: MfFaction?, mapType: MapType): SpigotChatColor {
        val relationshipService = plugin.services.factionRelationshipService
        when (mapType) {
            MapType.NORMAL -> {
                return SpigotChatColor.of(faction?.flags?.get(plugin.flags.color) ?: plugin.config.getString("wilderness.color"))
            }
            DIPLOMATIC -> {
                if (viewer == null) {
                    return if (faction == null) {
                        SpigotChatColor.of(plugin.config.getString("wilderness.color"))
                    } else {
                        SpigotChatColor.RED
                    }
                } else {
                    return if (faction == null) {
                        SpigotChatColor.of(plugin.config.getString("wilderness.color"))
                    } else {
                        val relationships = relationshipService.getRelationships(viewer.id, faction.id)
                        val reverseRelationships = relationshipService.getRelationships(faction.id, viewer.id)
                        when {
                            viewer.id.value == faction.id.value -> SpigotChatColor.GREEN
                            (relationships + reverseRelationships).any { it.type == AT_WAR } -> SpigotChatColor.RED
                            relationships.any { it.type == ALLY } && reverseRelationships.any { it.type == ALLY } -> SpigotChatColor.BLUE
                            relationshipService.getVassalTree(viewer.id).contains(faction.id) -> SpigotChatColor.DARK_GREEN
                            relationshipService.getLiegeChain(viewer.id).contains(faction.id) -> SpigotChatColor.YELLOW
                            else -> SpigotChatColor.WHITE
                        }
                    }
                }
            }
        }
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ) = when {
        args.isEmpty() -> MapType.values().map { it.name.lowercase() }
        args.size == 1 -> MapType.values().map { it.name.lowercase() }.filter { it.startsWith(args[0].lowercase()) }
        else -> emptyList()
    }
}
