package com.dansplugins.factionsystem.claim.worldguard

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfChunkPosition
import com.dansplugins.factionsystem.claim.MfRegionClaimGuard
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.protection.flags.StateFlag
import com.sk89q.worldguard.protection.managers.RegionManager
import com.sk89q.worldguard.protection.regions.GlobalProtectedRegion
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion
import com.sk89q.worldguard.protection.regions.ProtectedRegion
import org.bukkit.World

/**
 * Refuses claims in chunks that overlap a WorldGuard region (#1630).
 *
 * Every WorldGuard class is referenced from this file only. It is created only when the WorldGuard
 * plugin is enabled, so servers without WorldGuard never load it.
 *
 * Whether the check applies at all is decided by `worldGuard.blockClaimsInRegions`, which
 * [com.dansplugins.factionsystem.claim.MfClaimService.isClaimBlockedByWorldGuard] reads.
 */
class MfWorldGuardClaimGuard internal constructor(
    private val plugin: MedievalFactions,
    private val regionManagerFor: (World) -> RegionManager?
) : MfRegionClaimGuard {

    constructor(plugin: MedievalFactions) : this(
        plugin,
        { world -> WorldGuard.getInstance().platform.regionContainer.get(BukkitAdapter.adapt(world)) }
    )

    override fun overlapsProtectedRegion(chunk: MfChunkPosition): Boolean {
        val world = plugin.server.getWorld(chunk.worldId) ?: return false
        val regionManager = regionManagerFor(world) ?: return false
        val minX = chunk.x shl 4
        val minZ = chunk.z shl 4
        // The whole column of the chunk, so a region at any height counts
        val column = ProtectedCuboidRegion(
            CHUNK_QUERY_REGION_ID,
            BlockVector3.at(minX, world.minHeight, minZ),
            BlockVector3.at(minX + 15, world.maxHeight - 1, minZ + 15)
        )
        val exemptRegions = plugin.config.getStringList("worldGuard.exemptRegions").mapTo(HashSet()) { it.lowercase() }
        val exemptFlags = plugin.config.getStringList("worldGuard.exemptFlags").mapTo(HashSet()) { it.lowercase() }
        return regionManager.getApplicableRegions(column).regions.any { isBlockingRegion(it, exemptRegions, exemptFlags) }
    }

    companion object {
        private const val CHUNK_QUERY_REGION_ID = "__mf_claim_check__"

        /**
         * A region blocks claiming unless it is the world's global region, its ID is listed in
         * `worldGuard.exemptRegions`, or it sets one of the flags listed in `worldGuard.exemptFlags`.
         * A listed state flag (such as `build`) only exempts the region when it is set to `allow`;
         * any other listed flag exempts it when it is set to any value.
         */
        internal fun isBlockingRegion(region: ProtectedRegion, exemptRegions: Set<String>, exemptFlags: Set<String>): Boolean {
            if (region is GlobalProtectedRegion || region.id.equals(ProtectedRegion.GLOBAL_REGION, ignoreCase = true)) return false
            if (region.id.lowercase() in exemptRegions) return false
            if (exemptFlags.isEmpty()) return true
            return region.flags.none { (flag, value) ->
                flag.name.lowercase() in exemptFlags && (flag !is StateFlag || value == StateFlag.State.ALLOW)
            }
        }
    }
}
