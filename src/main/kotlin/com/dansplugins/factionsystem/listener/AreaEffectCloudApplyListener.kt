package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.AreaEffectCloudApplyEvent
import org.bukkit.potion.PotionType

class AreaEffectCloudApplyListener(private val plugin: MedievalFactions) : Listener {

    // Resolved by name so that constants missing from the running server's API are skipped
    // rather than failing plugin enable. The LONG_/STRONG_ variants and the HARMING name only
    // exist from 1.20.5; older servers call it INSTANT_DAMAGE and encode the variants in
    // PotionData flags, so the base type alone matches there.
    internal val harmfulPotionTypes: Set<PotionType> = PotionType.values()
        .filter { it.name in HARMFUL_POTION_TYPE_NAMES }
        .toSet()

    @EventHandler
    fun onAreaEffectCloudApply(event: AreaEffectCloudApplyEvent) {
        val basePotionData = event.entity.basePotionData ?: return
        if (!harmfulPotionTypes.contains(basePotionData.type)) return
        val potionService = plugin.services.potionService
        val damager = potionService.getLingeringPotionEffectThrower(event.entity) ?: return
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
                    event.affectedEntities.remove(damaged)
                }
                return
            }
            if (damagerFaction.id == damagedFaction.id) {
                if (!plugin.config.getBoolean("pvp.friendlyFire") && !damagerFaction.flags[plugin.flags.allowFriendlyFire]) {
                    event.affectedEntities.remove(damaged)
                }
                return
            }
            val relationshipService = plugin.services.factionRelationshipService
            val relationships = relationshipService.getRelationships(damagerFaction.id, damagedFaction.id)
            val reverseRelationships = relationshipService.getRelationships(damagedFaction.id, damagerFaction.id)
            if ((relationships + reverseRelationships).none { it.type == MfFactionRelationshipType.AT_WAR }) {
                if (plugin.config.getBoolean("pvp.warRequiredForPlayersOfDifferentFactions")) {
                    event.affectedEntities.remove(damaged)
                }
                return
            }
        }
    }

    companion object {
        internal val HARMFUL_POTION_TYPE_NAMES = setOf(
            "POISON",
            "LONG_POISON",
            "STRONG_POISON",
            "HARMING",
            "INSTANT_DAMAGE",
            "STRONG_HARMING",
            "SLOWNESS",
            "LONG_SLOWNESS",
            "STRONG_SLOWNESS",
            "WEAKNESS",
            "LONG_WEAKNESS"
        )
    }
}
