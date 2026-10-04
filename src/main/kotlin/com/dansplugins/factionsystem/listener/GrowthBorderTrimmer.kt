package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import org.bukkit.block.BlockState
import java.util.UUID

/**
 * Keeps growth inside the territory it starts in (#2084). A tree, a huge mushroom or bone-meal spread
 * that starts in one chunk can place blocks in a neighbouring one; without this, a sapling planted next
 * to a claim border grows logs and leaves into a claim where its owner could not place a block.
 *
 * Every block of the growth that lands in a chunk claimed by a faction other than the one owning the
 * origin chunk is dropped from the growth, and so is every block that lands in any claim when the origin
 * is wilderness. The rest of the growth goes ahead, so a tree on a border grows lopsided rather than not
 * at all. This applies to natural growth as well as to bone meal (owner decision 2026-10-04).
 */
class GrowthBorderTrimmer(private val plugin: MedievalFactions) {

    /** Removes the blocks that would cross into foreign territory from [blocks]; returns how many were removed. */
    fun trim(worldId: UUID, originX: Int, originZ: Int, blocks: MutableList<BlockState>): Int {
        val claimService = plugin.services.claimService
        val originFactionId = claimService.getClaim(worldId, originX shr 4, originZ shr 4)?.factionId
        val before = blocks.size
        blocks.removeIf { state ->
            val claim = claimService.getClaim(worldId, state.x shr 4, state.z shr 4)
            claim != null && claim.factionId != originFactionId
        }
        return before - blocks.size
    }
}
