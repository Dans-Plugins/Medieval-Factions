package com.dansplugins.factionsystem.claim

import com.dansplugins.factionsystem.faction.MfFactionId
import org.bukkit.World

interface MfClaimedChunkRepository {

    fun getClaim(world: World, x: Int, z: Int): MfClaimedChunk?
    fun getClaims(factionId: MfFactionId): List<MfClaimedChunk>
    fun getClaims(): List<MfClaimedChunk>
    fun upsert(claim: MfClaimedChunk): MfClaimedChunk
    fun delete(world: World, x: Int, z: Int)
    fun deleteAll(factionId: MfFactionId)

}