package com.dansplugins.factionsystem.slimefun

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.slimefun.MfSlimefunAction.ATTACK_ENTITY
import com.dansplugins.factionsystem.slimefun.MfSlimefunAction.ATTACK_PLAYER
import com.dansplugins.factionsystem.slimefun.MfSlimefunAction.BREAK_BLOCK
import com.dansplugins.factionsystem.slimefun.MfSlimefunAction.INTERACT_BLOCK
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.type.Door
import org.bukkit.block.data.type.Gate
import org.bukkit.block.data.type.TrapDoor

/**
 * Decides whether a player may use, break or place a Slimefun block or item at a location, by
 * applying the claim rules Medieval Factions already uses for the matching Bukkit events (#1750).
 *
 * It references no Slimefun class, so it can be tested and loaded without Slimefun installed.
 *
 * - Wilderness is never restricted here, and neither is an attack on a player, which Medieval
 *   Factions' own PvP rules decide when the damage happens.
 * - In a claim, the rules of [com.dansplugins.factionsystem.claim.MfClaimService.isInteractionAllowed]
 *   apply: members, the vassal and liege flags, the `alliesCanInteractWithLand` flag and
 *   `pvp.enableWartimeBlockDestruction` for factions at war.
 * - A player with bypass enabled and `mf.bypass` is let through.
 * - Otherwise the wartime lists apply as they do to ordinary blocks: `factions.wartimeBreakableBlocks`
 *   for breaking and `factions.wartimeInteractableBlocks` for using a block, and
 *   `factions.nonMembersCanInteractWithDoors` for doors, trapdoors and fence gates.
 * - Attacking a non-player entity in a claim is only restricted when the claim's faction has the
 *   `enableMobProtection` flag on.
 */
class MfSlimefunProtectionPolicy(private val plugin: MedievalFactions) {

    fun isAllowed(player: OfflinePlayer, location: Location, action: MfSlimefunAction): Boolean {
        if (!plugin.config.getBoolean("slimefun.protection.enabled")) return true
        if (action == ATTACK_PLAYER) return true
        val world = location.world ?: return true
        val claimService = plugin.services.claimService
        val claim = claimService.getClaim(world, location.blockX shr 4, location.blockZ shr 4) ?: return true
        if (action == ATTACK_ENTITY) {
            val claimFaction = plugin.services.factionService.getFaction(claim.factionId) ?: return true
            if (!claimFaction.flags[plugin.flags.enableMobProtection]) return true
        }
        // A player Medieval Factions has not seen yet belongs to no faction, so is an outsider everywhere
        val mfPlayer = plugin.services.playerService.getPlayer(player) ?: return false
        if (claimService.isInteractionAllowed(mfPlayer.id, claim)) return true
        if (mfPlayer.isBypassEnabled && player.player?.hasPermission("mf.bypass") == true) return true
        return when (action) {
            BREAK_BLOCK -> claimService.isWartimeBreakableBlock(mfPlayer.id, claim, location.block.type)
            INTERACT_BLOCK -> {
                val block = location.block
                (plugin.config.getBoolean("factions.nonMembersCanInteractWithDoors") && isDoor(block.blockData)) ||
                    claimService.isWartimeInteractableBlock(mfPlayer.id, claim, block.type)
            }
            else -> false
        }
    }

    private fun isDoor(blockData: BlockData) = blockData is Door || blockData is TrapDoor || blockData is Gate
}
