package com.dansplugins.factionsystem.command.faction.area

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.area.MfCuboidArea
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.lockedarea.MfLockedArea
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.AQUA
import org.bukkit.ChatColor.GRAY
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.UUID
import java.util.logging.Level.SEVERE

/**
 * `/faction area` (#1393): locked areas, named cuboids in the faction's territory where only listed
 * players and roles may build, break and use blocks.
 *
 * - `pos1` / `pos2` select the block the player stands on as a corner
 * - `create <name>`, `delete <name>`, `list`
 * - `add <name> <player>`, `add <name> role <role>`, and the same with `remove`
 *
 * Everything except `list` needs the MANAGE_LOCKED_AREAS faction permission.
 */
class MfFactionAreaCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {

    private val subcommands = listOf("pos1", "pos2", "create", "delete", "list", "add", "remove")

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.area")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionAreaNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionAreaNotAPlayer"]}")
            return true
        }
        val subcommand = args.firstOrNull()?.lowercase()
        if (subcommand !in subcommands) {
            sender.sendMessage("$RED${plugin.language["CommandFactionAreaUsage"]}")
            return true
        }
        // The corner is read here, on the main thread, before going async.
        val standingOn = MfBlockPosition.fromBukkitBlock(sender.location.block)
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionAreaFailedToSavePlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val faction = plugin.services.factionService.getFaction(mfPlayer.id)
                if (faction == null) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionAreaMustBeInAFaction"]}")
                    return@Runnable
                }
                if (subcommand == "list") {
                    list(sender, faction)
                    return@Runnable
                }
                val role = faction.getRole(mfPlayer.id)
                if (role == null || !role.hasPermission(faction, plugin.factionPermissions.manageLockedAreas)) {
                    sender.sendMessage("$RED${plugin.language["CommandFactionAreaNoFactionPermission"]}")
                    return@Runnable
                }
                val rest = args.drop(1)
                when (subcommand) {
                    "pos1", "pos2" -> {
                        plugin.services.lockedAreaService.select(mfPlayer.id, if (subcommand == "pos1") 1 else 2, standingOn)
                        sender.sendMessage(
                            "$GREEN${plugin.language["CommandFactionAreaPositionSet", subcommand.takeLast(1), "${standingOn.x}, ${standingOn.y}, ${standingOn.z}"]}"
                        )
                    }
                    "create" -> create(sender, mfPlayer, faction, rest)
                    "delete" -> delete(sender, faction, rest)
                    "add", "remove" -> editAccess(sender, faction, rest, add = subcommand == "add")
                }
            }
        )
        return true
    }

    private fun create(sender: Player, mfPlayer: MfPlayer, faction: MfFaction, args: List<String>) {
        val name = args.singleOrNull()
        if (name == null || !NAME_PATTERN.matches(name)) {
            sender.sendMessage("$RED${plugin.language["CommandFactionAreaInvalidName"]}")
            return
        }
        val lockedAreaService = plugin.services.lockedAreaService
        if (lockedAreaService.getLockedArea(faction.id, name) != null) {
            sender.sendMessage("$RED${plugin.language["CommandFactionAreaNameTaken", name]}")
            return
        }
        val (first, second) = lockedAreaService.getSelection(mfPlayer.id)
        if (first == null || second == null) {
            sender.sendMessage("$RED${plugin.language["CommandFactionAreaNoSelection"]}")
            return
        }
        if (first.worldId != second.worldId) {
            sender.sendMessage("$RED${plugin.language["CommandFactionAreaDifferentWorlds"]}")
            return
        }
        val area = MfCuboidArea(first, second)
        val claimService = plugin.services.claimService
        val chunksClaimed = (Math.floorDiv(area.minPosition.x, 16)..Math.floorDiv(area.maxPosition.x, 16)).all { chunkX ->
            (Math.floorDiv(area.minPosition.z, 16)..Math.floorDiv(area.maxPosition.z, 16)).all { chunkZ ->
                claimService.getClaim(first.worldId, chunkX, chunkZ)?.factionId?.value == faction.id.value
            }
        }
        if (!chunksClaimed) {
            sender.sendMessage("$RED${plugin.language["CommandFactionAreaNotAllClaimed"]}")
            return
        }
        lockedAreaService.save(MfLockedArea(factionId = faction.id, name = name, area = area)).onFailure {
            sender.sendMessage("$RED${plugin.language["CommandFactionAreaFailedToSave"]}")
            plugin.logger.log(SEVERE, "Failed to save locked area: ${it.reason.message}", it.reason.cause)
            return
        }
        lockedAreaService.clearSelection(mfPlayer.id)
        sender.sendMessage("$GREEN${plugin.language["CommandFactionAreaCreated", name]}")
    }

    private fun delete(sender: Player, faction: MfFaction, args: List<String>) {
        val lockedArea = findArea(sender, faction, args.firstOrNull()) ?: return
        plugin.services.lockedAreaService.delete(lockedArea.id).onFailure {
            sender.sendMessage("$RED${plugin.language["CommandFactionAreaFailedToSave"]}")
            plugin.logger.log(SEVERE, "Failed to delete locked area: ${it.reason.message}", it.reason.cause)
            return
        }
        sender.sendMessage("$GREEN${plugin.language["CommandFactionAreaDeleted", lockedArea.name]}")
    }

    private fun editAccess(sender: Player, faction: MfFaction, args: List<String>, add: Boolean) {
        if (args.size < 2) {
            sender.sendMessage("$RED${plugin.language["CommandFactionAreaUsage"]}")
            return
        }
        val lockedArea = findArea(sender, faction, args[0]) ?: return
        val updated: MfLockedArea
        val shownName: String
        if (args[1].equals("role", ignoreCase = true) && args.size >= 3) {
            val roleName = args.drop(2).joinToString(" ")
            val role = faction.getRole(roleName)
            // A role id still listed after its role was deleted can be removed, but not added.
            val roleId = if (add) role?.id?.value else role?.id?.value ?: roleName.takeIf { name -> lockedArea.accessorRoleIds.any { it.value == name } }
            if (roleId == null) {
                sender.sendMessage("$RED${plugin.language["CommandFactionAreaInvalidRole"]}")
                return
            }
            shownName = role?.name ?: roleName
            val others = lockedArea.accessorRoleIds.filter { it.value != roleId }
            updated = lockedArea.copy(accessorRoleIds = if (add && role != null) others + role.id else others)
        } else {
            val player = runCatching { plugin.server.getOfflinePlayer(UUID.fromString(args[1])) }.getOrNull()
                ?: plugin.server.getOfflinePlayer(args[1])
            if (!player.isOnline && !player.hasPlayedBefore()) {
                sender.sendMessage("$RED${plugin.language["CommandFactionAreaInvalidPlayer"]}")
                return
            }
            val playerId = MfPlayerId(player.uniqueId.toString())
            if (add && plugin.services.playerService.getPlayer(playerId) == null) {
                plugin.services.playerService.save(MfPlayer(plugin, player)).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandFactionAreaFailedToSavePlayer"]}")
                    return
                }
            }
            shownName = player.name ?: args[1]
            val others = lockedArea.accessors.filter { it.value != playerId.value }
            updated = lockedArea.copy(accessors = if (add) others + playerId else others)
        }
        plugin.services.lockedAreaService.save(updated).onFailure {
            sender.sendMessage("$RED${plugin.language["CommandFactionAreaFailedToSave"]}")
            plugin.logger.log(SEVERE, "Failed to save locked area: ${it.reason.message}", it.reason.cause)
            return
        }
        val key = if (add) "CommandFactionAreaAccessAdded" else "CommandFactionAreaAccessRemoved"
        sender.sendMessage("$GREEN${plugin.language[key, shownName, lockedArea.name]}")
    }

    private fun list(sender: Player, faction: MfFaction) {
        val areas = plugin.services.lockedAreaService.getLockedAreas(faction.id)
        if (areas.isEmpty()) {
            sender.sendMessage("$GRAY${plugin.language["CommandFactionAreaListEmpty"]}")
            return
        }
        sender.sendMessage("$AQUA${plugin.language["CommandFactionAreaListTitle"]}")
        val playerService = plugin.services.playerService
        areas.forEach { lockedArea ->
            val min = lockedArea.area.minPosition
            val max = lockedArea.area.maxPosition
            val access = lockedArea.accessors.map { playerService.getPlayer(it)?.toBukkit()?.name ?: plugin.language["UnknownPlayer"] } +
                lockedArea.accessorRoleIds.mapNotNull { faction.getRole(it)?.name }
            sender.sendMessage(
                "$GRAY${plugin.language[
                    "CommandFactionAreaListItem",
                    lockedArea.name,
                    "${min.x}, ${min.y}, ${min.z}",
                    "${max.x}, ${max.y}, ${max.z}",
                    access.joinToString(", ").ifEmpty { "-" }
                ]}"
            )
        }
    }

    private fun findArea(sender: Player, faction: MfFaction, name: String?): MfLockedArea? {
        val lockedArea = name?.let { plugin.services.lockedAreaService.getLockedArea(faction.id, it) }
        if (lockedArea == null) sender.sendMessage("$RED${plugin.language["CommandFactionAreaNotFound"]}")
        return lockedArea
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String> {
        if (args.size <= 1) return subcommands.filter { it.startsWith(args.firstOrNull()?.lowercase() ?: "") }
        if (sender !is Player || args.size != 2 || args[0].lowercase() !in listOf("delete", "add", "remove")) return emptyList()
        val faction = plugin.services.factionService.getFaction(MfPlayerId(sender.uniqueId.toString())) ?: return emptyList()
        return plugin.services.lockedAreaService.getLockedAreas(faction.id).map { it.name }.filter { it.lowercase().startsWith(args[1].lowercase()) }
    }

    companion object {
        private val NAME_PATTERN = Regex("[A-Za-z0-9_-]{1,32}")
    }
}
