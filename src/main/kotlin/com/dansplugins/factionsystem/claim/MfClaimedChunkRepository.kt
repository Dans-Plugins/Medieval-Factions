package com.dansplugins.factionsystem.claim

import com.dansplugins.factionsystem.faction.MfFactionId
import java.util.*

interface MfClaimedChunkRepository {

    fun getClaim(worldId: UUID, x: Int, z: Int): MfClaimedChunk?
    fun getClaims(factionId: MfFactionId): List<MfClaimedChunk>
    fun getClaims(): List<MfClaimedChunk>
    fun upsert(claim: MfClaimedChunk): MfClaimedChunk
    fun delete(worldId: UUID, x: Int, z: Int)
    fun deleteAll(factionId: MfFactionId)
}
