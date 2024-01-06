package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent

class EntityDamageByEntityListener(private val plugin: MedievalFactions) : Listener {

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val damaged = event.entity
        val damager = event.damager
        val damagerPlayer: Player? = when (damager) {
            is Player -> damager
            is Projectile -> damager.shooter as? Player
            else -> null
        }
        if (damagerPlayer != null) {
            val playerService = plugin.services.playerService
            val factionService = plugin.services.factionService
            val duelService = plugin.services.duelService
            val damagerMfPlayer = playerService.getPlayer(damagerPlayer) ?: MfPlayer(plugin, damagerPlayer)
            val damagerFaction = factionService.getFaction(damagerMfPlayer.id)
            if (damaged !is Player) {
                val claimService = plugin.services.claimService
                val claim = claimService.getClaim(damaged.location.chunk) ?: return
                val damagedFaction = factionService.getFaction(claim.factionId) ?: return
                if (!damagedFaction.flags[plugin.flags.enableMobProtection]) return
                if (damagerFaction?.id == damagedFaction.id) return
                if (damaged is Monster) return
                event.isCancelled = true
                return
            }
            val damagedMfPlayer = playerService.getPlayer(damaged) ?: MfPlayer(plugin, damaged)
            val damagerDuel = duelService.getDuel(damagerMfPlayer.id)
            val damagedDuel = duelService.getDuel(damagedMfPlayer.id)
            if (damagerDuel != null && damagedDuel != null && damagerDuel.id == damagedDuel.id) {
                return
            }
            val damagedFaction = factionService.getFaction(damagedMfPlayer.id)
            if (damagerFaction == null || damagedFaction == null) {
                if (!plugin.config.getBoolean("pvp.enabledForFactionlessPlayers")) {
                    event.isCancelled = true
                }
                return
            }
            if (damagerFaction.id == damagedFaction.id) {
                if (!plugin.config.getBoolean("pvp.friendlyFire") && !damagerFaction.flags[plugin.flags.allowFriendlyFire]) {
                    event.isCancelled = true
                }
                return
            }
            val relationshipService = plugin.services.factionRelationshipService
            val relationships = relationshipService.getRelationships(damagerFaction.id, damagedFaction.id)
            val reverseRelationships = relationshipService.getRelationships(damagedFaction.id, damagerFaction.id)
            if ((relationships + reverseRelationships).none { it.type == MfFactionRelationshipType.AT_WAR }) {
                if (plugin.config.getBoolean("pvp.warRequiredForPlayersOfDifferentFactions")) {
                    event.isCancelled = true
                }
                return
            }
        }
    }
}
