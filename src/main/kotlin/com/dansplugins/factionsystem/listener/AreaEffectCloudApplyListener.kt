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

    internal val harmfulPotionTypes: List<PotionType> = listOf(
        PotionType.POISON,
        PotionType.LONG_POISON,
        PotionType.STRONG_POISON,
        PotionType.HARMING,
        PotionType.STRONG_HARMING,
        PotionType.SLOWNESS,
        PotionType.LONG_SLOWNESS,
        PotionType.STRONG_SLOWNESS,
        PotionType.WEAKNESS,
        PotionType.LONG_WEAKNESS
    )

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
}
