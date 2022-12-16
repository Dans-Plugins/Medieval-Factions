package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent

class CreatureSpawnListener(private val plugin: MedievalFactions) : Listener {

    @EventHandler
    fun onCreatureSpawn(event: CreatureSpawnEvent) {
        if (plugin.config.getBoolean("factions.mobsSpawnInFactionTerritory")) {
            return
        }

        val spawnReasonsToAllow = plugin.config.getStringList("factions.allowedMobSpawnReasons")
            .mapNotNull {
                try {
                    return@mapNotNull CreatureSpawnEvent.SpawnReason.valueOf(it)
                } catch (exception: IllegalArgumentException) {
                    return@mapNotNull null
                }
            }
        if (spawnReasonsToAllow.contains(event.spawnReason)) {
            return
        }

        val claimService = plugin.services.claimService
        if (claimService.getClaim(event.location.chunk) == null) {
            return
        }

        event.isCancelled = true
    }
}
