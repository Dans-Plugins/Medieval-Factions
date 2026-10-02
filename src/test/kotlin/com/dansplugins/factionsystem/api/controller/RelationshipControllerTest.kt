package com.dansplugins.factionsystem.api.controller

import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.relationship.MfFactionRelationship
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipService
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID

class RelationshipControllerTest {

    private lateinit var relationshipService: MfFactionRelationshipService
    private lateinit var factionService: MfFactionService
    private lateinit var controller: RelationshipController
    private lateinit var context: Context

    @BeforeEach
    fun setUp() {
        relationshipService = mock(MfFactionRelationshipService::class.java)
        factionService = mock(MfFactionService::class.java)
        controller = RelationshipController(relationshipService, factionService)
        context = mock(Context::class.java)
    }

    @Test
    fun getAll_ShouldReturnAllRelationships() {
        // Arrange
        // getAll has no all-relationships accessor to call: it walks the factions and
        // asks for each faction's relationships in turn.
        val factionId = MfFactionId(UUID.randomUUID().toString())
        val faction = mock(MfFaction::class.java)
        `when`(faction.id).thenReturn(factionId)
        val relationship1 = createMockRelationship(factionId)
        val relationship2 = createMockRelationship(factionId)
        `when`(factionService.factions).thenReturn(listOf(faction))
        `when`(relationshipService.getRelationships(factionId)).thenReturn(listOf(relationship1, relationship2))
        `when`(context.json(anyObject())).thenReturn(context)

        // Act
        controller.getAll(context)

        // Assert
        verify(factionService).factions
        verify(relationshipService).getRelationships(factionId)
        verify(context).json(org.mockito.ArgumentMatchers.anyList<Any>())
        verify(context).status(HttpStatus.OK)
    }

    @Test
    fun getByFactionId_WithValidId_ShouldReturnRelationships() {
        // Arrange
        val factionId = MfFactionId(UUID.randomUUID().toString())
        val relationship1 = createMockRelationship(factionId)
        val relationship2 = createMockRelationship(factionId)
        `when`(context.pathParam("id")).thenReturn(factionId.value.toString())
        `when`(relationshipService.getRelationships(factionId)).thenReturn(listOf(relationship1, relationship2))
        `when`(context.json(anyObject())).thenReturn(context)

        // Act
        controller.getByFactionId(context)

        // Assert
        verify(relationshipService).getRelationships(factionId)
        verify(context).json(org.mockito.ArgumentMatchers.anyList<Any>())
        verify(context).status(HttpStatus.OK)
    }

    @Test
    fun getByFactionId_WithInvalidId_ShouldReturnBadRequest() {
        // Arrange
        `when`(context.pathParam("id")).thenReturn("invalid-uuid")
        `when`(context.json(anyObject())).thenReturn(context)

        // Act
        controller.getByFactionId(context)

        // Assert
        verify(context).json(anyObject())
        verify(context).status(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun getByFactionId_WithNoRelationships_ShouldReturnEmptyList() {
        // Arrange
        val factionId = MfFactionId(UUID.randomUUID().toString())
        `when`(context.pathParam("id")).thenReturn(factionId.value.toString())
        `when`(relationshipService.getRelationships(factionId)).thenReturn(emptyList())
        `when`(context.json(anyObject())).thenReturn(context)

        // Act
        controller.getByFactionId(context)

        // Assert
        verify(relationshipService).getRelationships(factionId)
        verify(context).json(org.mockito.ArgumentMatchers.anyList<Any>())
        verify(context).status(HttpStatus.OK)
    }

    private fun createMockRelationship(
        factionId: MfFactionId = MfFactionId(UUID.randomUUID().toString()),
        targetId: MfFactionId = MfFactionId(UUID.randomUUID().toString())
    ): MfFactionRelationship {
        val relationship = mock(MfFactionRelationship::class.java)
        `when`(relationship.factionId).thenReturn(factionId)
        `when`(relationship.targetId).thenReturn(targetId)
        `when`(relationship.type).thenReturn(MfFactionRelationshipType.ALLY)
        return relationship
    }
}
