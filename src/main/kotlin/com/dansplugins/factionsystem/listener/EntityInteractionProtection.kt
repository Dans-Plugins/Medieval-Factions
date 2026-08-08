package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.player.MfPlayer
import org.bukkit.ChatColor.RED
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEntityEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Territory protection for right-clicking entities, shared by [PlayerInteractAtEntityListener] and
 * [PlayerInteractEntityListener].
 *
 * `PlayerInteractAtEntityEvent` extends `PlayerInteractEntityEvent`, but it declares its own handler
 * list, so a single event is only ever delivered to one of the two listeners. A single right-click can
 * still raise both events: the client sends an "interact at" packet, and follows it with a plain
 * "interact" packet whenever the entity does not consume the first one. Cancelling both events is
 * required, since each is a separate opportunity for the interaction to go through, but the player
 * should only be told once, so notifications are de-duplicated per player and clicked entity over a
 * short window.
 */
class EntityInteractionProtection(private val plugin: MedievalFactions) {

    private val lastNotifications = ConcurrentHashMap<UUID, Notification>()

    private data class Notification(val entityId: UUID, val timeMillis: Long)

    /**
     * Applies claim and wilderness interaction protection to [event] on behalf of [mfPlayer].
     *
     * The event is cancelled when the interaction is not permitted, and the player is notified unless
     * they were already notified about the same entity a moment ago. Returns true when the event was
     * cancelled.
     */
    fun protect(event: PlayerInteractEntityEvent, mfPlayer: MfPlayer): Boolean {
        val player = event.player
        val clickedEntity = event.rightClicked
        val claimService = plugin.services.claimService
        val claim = claimService.getClaim(clickedEntity.location.chunk)
        if (claim == null) {
            if (plugin.config.getBoolean("wilderness.interaction.prevent", false)) {
                event.isCancelled = true
                if (plugin.config.getBoolean("wilderness.interaction.alert", true)) {
                    notify(player, clickedEntity, "$RED${plugin.language["CannotInteractWithEntityInWilderness"]}")
                }
                return true
            }
            return false
        }

        if (plugin.config.getBoolean("factions.nonMembersCanInteractWithEntities")) return false

        val factionService = plugin.services.factionService
        val claimFaction = factionService.getFaction(claim.factionId) ?: return false

        if (claimService.isInteractionAllowed(mfPlayer.id, claim)) return false

        if (mfPlayer.isBypassEnabled && player.hasPermission("mf.bypass")) {
            notify(player, clickedEntity, "$RED${plugin.language["FactionTerritoryProtectionBypassed"]}")
            return false
        }

        event.isCancelled = true
        notify(player, clickedEntity, "$RED${plugin.language["CannotInteractWithEntityInFactionTerritory", claimFaction.name]}")
        return true
    }

    /**
     * Discards the retained notification state for [playerId].
     *
     * The cache holds one entry per player and is only consulted for the few hundred milliseconds
     * after a notification, so it is dropped when the player leaves rather than being kept for the
     * lifetime of the server.
     */
    fun forgetPlayer(playerId: UUID) {
        lastNotifications.remove(playerId)
    }

    private fun notify(player: Player, clickedEntity: Entity, message: String) {
        if (!shouldNotify(player.uniqueId, clickedEntity.uniqueId)) return
        player.sendMessage(message)
    }

    private fun shouldNotify(playerId: UUID, entityId: UUID): Boolean {
        val now = System.currentTimeMillis()
        val previous = lastNotifications.put(playerId, Notification(entityId, now))
        return previous == null ||
            previous.entityId != entityId ||
            now - previous.timeMillis > DUPLICATE_NOTIFICATION_WINDOW_MILLISECONDS
    }

    companion object {
        /**
         * How long a notification about a given entity suppresses a repeat notification about that same
         * entity. Long enough to cover the two events a single right-click can raise, short enough that a
         * player deliberately clicking again is still told why nothing happened.
         */
        private const val DUPLICATE_NOTIFICATION_WINDOW_MILLISECONDS = 250L
    }
}
