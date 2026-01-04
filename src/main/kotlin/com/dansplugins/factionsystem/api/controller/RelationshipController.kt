package com.dansplugins.factionsystem.api.controller

import com.dansplugins.factionsystem.api.dto.ErrorResponse
import com.dansplugins.factionsystem.api.dto.RelationshipDto
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipService
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import java.util.UUID

class RelationshipController(private val relationshipService: MfFactionRelationshipService) {

    fun getAll(ctx: Context) {
        val relationships = relationshipService.relationships.map { RelationshipDto.fromRelationship(it) }
        ctx.json(relationships).status(HttpStatus.OK)
    }

    fun getByFactionId(ctx: Context) {
        val id = ctx.pathParam("id")
        try {
            val factionId = MfFactionId(UUID.fromString(id))
            val relationships = relationshipService.getRelationships(factionId)
                .map { RelationshipDto.fromRelationship(it) }
            
            ctx.json(relationships).status(HttpStatus.OK)
        } catch (e: IllegalArgumentException) {
            ctx.json(ErrorResponse("INVALID_ID", "Invalid faction ID format")).status(HttpStatus.BAD_REQUEST)
        }
    }
}
