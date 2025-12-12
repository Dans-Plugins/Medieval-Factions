package com.dansplugins.factionsystem.api.controller

import com.dansplugins.factionsystem.api.dto.ErrorResponse
import com.dansplugins.factionsystem.api.dto.FactionDto
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.MfFactionService
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import java.util.UUID

class FactionController(private val factionService: MfFactionService) {

    fun getAll(ctx: Context) {
        val factions = factionService.factions.map { FactionDto.fromFaction(it) }
        ctx.json(factions).status(HttpStatus.OK)
    }

    fun getById(ctx: Context) {
        val id = ctx.pathParam("id")
        try {
            val factionId = MfFactionId(UUID.fromString(id))
            val faction = factionService.getFaction(factionId)
            
            if (faction != null) {
                ctx.json(FactionDto.fromFaction(faction)).status(HttpStatus.OK)
            } else {
                ctx.json(ErrorResponse("NOT_FOUND", "Faction not found")).status(HttpStatus.NOT_FOUND)
            }
        } catch (e: IllegalArgumentException) {
            ctx.json(ErrorResponse("INVALID_ID", "Invalid faction ID format")).status(HttpStatus.BAD_REQUEST)
        }
    }

    fun getByName(ctx: Context) {
        val name = ctx.pathParam("name")
        val faction = factionService.getFaction(name)
        
        if (faction != null) {
            ctx.json(FactionDto.fromFaction(faction)).status(HttpStatus.OK)
        } else {
            ctx.json(ErrorResponse("NOT_FOUND", "Faction not found")).status(HttpStatus.NOT_FOUND)
        }
    }
}
