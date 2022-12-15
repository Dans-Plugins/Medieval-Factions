package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PotionSplashEvent
import org.bukkit.potion.PotionEffectType

class PotionSplashListener(private val plugin: MedievalFactions) : Listener {

    private val harmfulPotionEffectTypes = listOf(
        "BAD_OMEN",
        "BLINDNESS",
        "CONFUSION",
        "DARKNESS",
        "HARM",
        "HUNGER",
        "POISON",
        "SLOW",
        "SLOW_DIGGING",
        "UNLUCK",
        "WEAKNESS",
        "WITHER"
    ).mapNotNull {
        PotionEffectType.getByName(it)
    }

    @EventHandler
    fun onPotionSplash(event: PotionSplashEvent) {
        if (event.potion.effects.none { potionEffect -> harmfulPotionEffectTypes.any { it == potionEffect.type } }) return
        val damager = event.potion.shooter as? Player ?: return
        for (damaged in event.affectedEntities.filterIsInstance<Player>()) {
            val playerService = plugin.services.playerService
            val factionService = plugin.services.factionService
            val duelService = plugin.services.duelService
            val damagerMfPlayer = playerService.getPlayer(damager) ?: MfPlayer(plugin, damager)
            val damagerFaction = factionService.getFaction(damagerMfPlayer.id)
            val damagedMfPlayer = playerService.getPlayer(damaged) ?: MfPlayer(plugin, damaged)
            val damagerDuel = duelService.getDuel(damagerMfPlayer.id)
            val damagedDuel = duelService.getDuel(damagedMfPlayer.id)
            if (damagerDuel != null && damagedDuel != null && damagerDuel.id == damagedDuel.id) {
                return
            }
            val damagedFaction = factionService.getFaction(damagedMfPlayer.id)
            if (damagerFaction == null || damagedFaction == null) {
                if (!plugin.config.getBoolean("pvp.enabledForFactionlessPlayers")) {
                    event.setIntensity(damaged, 0.0)
                }
                return
            }
            if (damagerFaction.id == damagedFaction.id) {
                if (!plugin.config.getBoolean("pvp.friendlyFire") && !damagerFaction.flags[plugin.flags.allowFriendlyFire]) {
                    event.setIntensity(damaged, 0.0)
                }
                return
            }
            val relationshipService = plugin.services.factionRelationshipService
            val relationships = relationshipService.getRelationships(damagerFaction.id, damagedFaction.id)
            val reverseRelationships = relationshipService.getRelationships(damagedFaction.id, damagerFaction.id)
            if ((relationships + reverseRelationships).none { it.type == MfFactionRelationshipType.AT_WAR }) {
                if (plugin.config.getBoolean("pvp.warRequiredForPlayersOfDifferentFactions")) {
                    event.setIntensity(damaged, 0.0)
                }
                return
            }
        }
    }
}
