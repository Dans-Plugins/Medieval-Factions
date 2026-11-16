package com.dansplugins.factionsystem.claim

import com.dansplugins.factionsystem.area.MfChunkPosition

/**
 * Reports whether a chunk lies inside a region that another protection plugin manages, so that
 * claiming it can be refused.
 *
 * Implementations that talk to an optional plugin (such as WorldGuard) live in their own class and
 * are only created when that plugin is enabled, so Medieval Factions still loads without it.
 */
interface MfRegionClaimGuard {
    fun overlapsProtectedRegion(chunk: MfChunkPosition): Boolean
}
