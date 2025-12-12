package com.dansplugins.factionsystem.api.dto

import com.dansplugins.factionsystem.claim.MfClaimedChunk

data class ClaimDto(
    val worldId: String,
    val x: Int,
    val z: Int,
    val factionId: String
) {
    companion object {
        fun fromClaim(claim: MfClaimedChunk): ClaimDto {
            return ClaimDto(
                worldId = claim.worldId.toString(),
                x = claim.x,
                z = claim.z,
                factionId = claim.factionId.value.toString()
            )
        }
    }
}
