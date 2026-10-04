package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import org.bukkit.ChatColor.RED
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable

/**
 * Territory protection for player actions that change the world through a block without placing or
 * breaking one: setting fire ([BlockIgniteListener]), applying bone meal ([BlockFertilizeListener]),
 * placing a vehicle, armour stand or end crystal ([EntityPlaceListener]) and hanging an item frame or
 * painting ([HangingPlaceListener]) - see #2002.
 *
 * [PlayerInteractListener] already cancels the right-click behind each of these, so for most clicks
 * none of these events fires at all. This is the second line of defence: it does not depend on that
 * listener's world-neutral item list, and it keeps each action protected if that listener changes.
 * It therefore applies the same rules - wilderness, claim relationship, `mf.bypass` and the wartime
 * placeable list for the item used - so it never refuses an action the interact listener allows by
 * design. Bypass is allowed silently: the interact listener has already told the player.
 */
class WorldActionProtection(private val plugin: MedievalFactions) {

    /**
     * Cancels [event] when [player] may not change [block] with [item], and tells the player why.
     * Returns true when the event was cancelled.
     */
    fun protect(event: Cancellable, player: Player, block: Block, item: Material?): Boolean {
        val mfPlayer = plugin.services.playerService.getPlayer(player)
        if (mfPlayer == null) {
            // PlayerInteractListener saves unknown players and tells them; a second notice would be noise.
            event.isCancelled = true
            return true
        }

        val claimService = plugin.services.claimService
        val claim = claimService.getClaim(block.chunk)
        if (claim == null) {
            if (!plugin.config.getBoolean("wilderness.interaction.prevent", false)) return false
            event.isCancelled = true
            if (plugin.config.getBoolean("wilderness.interaction.alert", true)) {
                player.sendMessage("$RED${plugin.language["CannotInteractBlockInWilderness"]}")
            }
            return true
        }

        val claimFaction = plugin.services.factionService.getFaction(claim.factionId) ?: return false
        if (claimService.isInteractionAllowed(mfPlayer.id, claim)) return false
        if (mfPlayer.isBypassEnabled && player.hasPermission("mf.bypass")) return false
        if (item != null && claimService.isWartimePlaceableBlock(mfPlayer.id, claim, item)) return false

        event.isCancelled = true
        player.sendMessage("$RED${plugin.language["CannotInteractWithBlockInFactionTerritory", claimFaction.name]}")
        return true
    }
}
