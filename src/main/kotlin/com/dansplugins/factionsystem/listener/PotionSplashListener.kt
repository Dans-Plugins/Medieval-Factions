package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PotionSplashEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

// isHarmful is a parameter so that tests can avoid PotionEffectType, whose constants can only be
// loaded from a running server's registry
class PotionSplashListener internal constructor(
    private val plugin: MedievalFactions,
    private val isHarmful: (PotionEffect) -> Boolean
) : Listener {

    constructor(plugin: MedievalFactions) : this(plugin, harmfulPotionEffectTypes().let { types -> { effect -> effect.type in types } })

    @EventHandler
    fun onPotionSplash(event: PotionSplashEvent) {
        if (event.potion.effects.none(isHarmful)) return
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
                continue
            }
            val damagedFaction = factionService.getFaction(damagedMfPlayer.id)
            if (damagerFaction == null || damagedFaction == null) {
                if (!plugin.config.getBoolean("pvp.enabledForFactionlessPlayers")) {
                    event.setIntensity(damaged, 0.0)
                }
                continue
            }
            if (damagerFaction.id == damagedFaction.id) {
                if (!plugin.config.getBoolean("pvp.friendlyFire") && !damagerFaction.flags[plugin.flags.allowFriendlyFire]) {
                    event.setIntensity(damaged, 0.0)
                }
                continue
            }
            val relationshipService = plugin.services.factionRelationshipService
            val relationships = relationshipService.getRelationships(damagerFaction.id, damagedFaction.id)
            val reverseRelationships = relationshipService.getRelationships(damagedFaction.id, damagerFaction.id)
            if ((relationships + reverseRelationships).none { it.type == MfFactionRelationshipType.AT_WAR }) {
                if (plugin.config.getBoolean("pvp.warRequiredForPlayersOfDifferentFactions")) {
                    event.setIntensity(damaged, 0.0)
                }
                continue
            }
        }
    }

    companion object {
        private fun harmfulPotionEffectTypes() = listOf(
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
    }
}
