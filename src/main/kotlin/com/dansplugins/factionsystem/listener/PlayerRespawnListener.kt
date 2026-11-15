package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.player.MfPlayerId
import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerRespawnEvent

/**
 * Sends a respawning player to their faction's home when `factions.respawnAtFactionHome` is true.
 * Applies to deaths only. A bed or respawn anchor still takes priority, and nothing changes for a player whose faction
 * has no home or whose home is in a world that is not loaded.
 */
class PlayerRespawnListener internal constructor(
    private val plugin: MedievalFactions,
    private val homeLocationOf: (MfFaction) -> Location?
) : Listener {

    constructor(plugin: MedievalFactions) : this(plugin, { faction -> faction.home?.toBukkitLocation() })

    @EventHandler
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        if (!plugin.config.getBoolean("factions.respawnAtFactionHome")) return
        if (event.isBedSpawn || event.isAnchorSpawn) return
        // Only deaths: leaving the End also fires this event. getRespawnReason() is missing on
        // older server versions, where every respawn is treated as a death.
        val reason = runCatching { event.respawnReason }.getOrNull()
        if (reason != null && reason != PlayerRespawnEvent.RespawnReason.DEATH) return
        val faction = plugin.services.factionService.getFaction(MfPlayerId.fromBukkitPlayer(event.player)) ?: return
        val home = homeLocationOf(faction) ?: return
        event.respawnLocation = home
    }
}
