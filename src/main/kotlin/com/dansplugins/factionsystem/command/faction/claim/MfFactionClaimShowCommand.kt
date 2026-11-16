package com.dansplugins.factionsystem.command.faction.claim

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFactionId
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/**
 * `/f claim show` (#1515): marks the borders of claims near the sender with wool that only the sender can see, and
 * puts the real blocks back after a while. Nothing in the world is changed; the markers are client-side block changes.
 * The sender's own faction's borders are lime, every other faction's are red.
 */
class MfFactionClaimShowCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.claim.show") && !sender.hasPermission("mf.showclaim")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionClaimShowNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionClaimShowNotAPlayer"]}")
            return true
        }
        val world = sender.world
        val centre = sender.location.chunk
        val claimService = plugin.services.claimService
        val ownFactionId = plugin.services.playerService.getPlayer(sender)
            ?.let { plugin.services.factionService.getFaction(it.id) }?.id
        val ownerAt = { x: Int, z: Int -> claimService.getClaim(world.uid, x, z)?.factionId }
        val markers = mutableListOf<Location>()
        for (chunkX in centre.x - RADIUS..centre.x + RADIUS) {
            for (chunkZ in centre.z - RADIUS..centre.z + RADIUS) {
                val owner = ownerAt(chunkX, chunkZ) ?: continue
                val marker = if (owner == ownFactionId) Material.LIME_WOOL else Material.RED_WOOL
                val blockData = marker.createBlockData()
                borderColumns(chunkX, chunkZ, ownerAt).forEach { (x, z) ->
                    val location = Location(world, x.toDouble(), world.getHighestBlockYAt(x, z).toDouble(), z.toDouble())
                    sender.sendBlockChange(location, blockData)
                    markers += location
                }
            }
        }
        if (markers.isEmpty()) {
            sender.sendMessage("$GREEN${plugin.language["CommandFactionClaimShowNoClaims", RADIUS.toString()]}")
            return true
        }
        sender.sendMessage("$GREEN${plugin.language["CommandFactionClaimShowSuccess", RADIUS.toString(), DURATION_SECONDS.toString()]}")
        plugin.server.scheduler.runTaskLater(
            plugin,
            Runnable {
                if (!sender.isOnline) return@Runnable
                markers.filter { it.world == sender.world }.forEach { sender.sendBlockChange(it, it.block.blockData) }
            },
            DURATION_SECONDS * 20L
        )
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ) = emptyList<String>()

    companion object {
        private const val RADIUS = 4
        private const val DURATION_SECONDS = 15

        /**
         * The block columns (block x, block z) along the edges of a claimed chunk where the neighbouring chunk has a
         * different owner or none, i.e. where the claim's border runs. Edges shared with the same owner's claims are
         * left out, so a faction's territory is outlined rather than every chunk boxed.
         */
        fun borderColumns(chunkX: Int, chunkZ: Int, ownerAt: (Int, Int) -> MfFactionId?): Set<Pair<Int, Int>> {
            val owner = ownerAt(chunkX, chunkZ) ?: return emptySet()
            val minX = chunkX * 16
            val minZ = chunkZ * 16
            val columns = linkedSetOf<Pair<Int, Int>>()
            if (ownerAt(chunkX, chunkZ - 1) != owner) (0..15).forEach { columns += (minX + it) to minZ }
            if (ownerAt(chunkX, chunkZ + 1) != owner) (0..15).forEach { columns += (minX + it) to (minZ + 15) }
            if (ownerAt(chunkX - 1, chunkZ) != owner) (0..15).forEach { columns += minX to (minZ + it) }
            if (ownerAt(chunkX + 1, chunkZ) != owner) (0..15).forEach { columns += (minX + 15) to (minZ + it) }
            return columns
        }
    }
}
