package com.dansplugins.factionsystem.api.controller

import com.dansplugins.factionsystem.api.dto.ErrorResponse
import com.dansplugins.factionsystem.api.dto.RelationshipDto
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipService
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import java.util.UUID

class RelationshipController(
    private val relationshipService: MfFactionRelationshipService,
    private val factionService: MfFactionService
) {

    fun getAll(ctx: Context) {
        // Get all relationships by iterating through all factions
        val allRelationships = mutableListOf<com.dansplugins.factionsystem.relationship.MfFactionRelationship>()
        factionService.factions.forEach { faction ->
            allRelationships.addAll(relationshipService.getRelationships(faction.id))
        }
        val relationships = allRelationships.map { RelationshipDto.fromRelationship(it) }
        ctx.json(relationships).status(HttpStatus.OK)
    }

    fun getByFactionId(ctx: Context) {
        val id = ctx.pathParam("id")
        try {
            UUID.fromString(id) // Validate UUID format
            val factionId = MfFactionId(id)
            val relationships = relationshipService.getRelationships(factionId)
                .map { RelationshipDto.fromRelationship(it) }
            
            ctx.json(relationships).status(HttpStatus.OK)
        } catch (e: IllegalArgumentException) {
            ctx.json(ErrorResponse("INVALID_ID", "Invalid faction ID format")).status(HttpStatus.BAD_REQUEST)
        }
    }
}
