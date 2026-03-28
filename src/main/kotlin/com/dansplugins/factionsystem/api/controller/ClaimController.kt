package com.dansplugins.factionsystem.api.controller

import com.dansplugins.factionsystem.api.dto.ClaimDto
import com.dansplugins.factionsystem.api.dto.ErrorResponse
import com.dansplugins.factionsystem.claim.MfClaimService
import com.dansplugins.factionsystem.faction.MfFactionId
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import java.util.UUID

class ClaimController(private val claimService: MfClaimService) {

    fun getAll(ctx: Context) {
        val claims = claimService.getClaims().map { ClaimDto.fromClaim(it) }
        ctx.json(claims).status(HttpStatus.OK)
    }

    fun getByFactionId(ctx: Context) {
        val id = ctx.pathParam("id")
        try {
            UUID.fromString(id) // Validate UUID format
            val factionId = MfFactionId(id)
            val claims = claimService.getClaims(factionId).map { ClaimDto.fromClaim(it) }
            ctx.json(claims).status(HttpStatus.OK)
        } catch (e: IllegalArgumentException) {
            ctx.json(ErrorResponse("INVALID_ID", "Invalid faction ID format")).status(HttpStatus.BAD_REQUEST)
        }
    }
}
