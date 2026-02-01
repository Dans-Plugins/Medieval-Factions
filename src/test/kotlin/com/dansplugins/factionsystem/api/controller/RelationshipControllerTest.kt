package com.dansplugins.factionsystem.api.controller

import com.dansplugins.factionsystem.faction.MfFactionId
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
    private lateinit var controller: RelationshipController
    private lateinit var context: Context

    @BeforeEach
    fun setUp() {
        relationshipService = mock(MfFactionRelationshipService::class.java)
        controller = RelationshipController(relationshipService)
        context = mock(Context::class.java)
    }

    @Test
    fun getAll_ShouldReturnAllRelationships() {
        // Arrange
        val relationship1 = createMockRelationship()
        val relationship2 = createMockRelationship()
        `when`(relationshipService.relationships).thenReturn(listOf(relationship1, relationship2))
        `when`(context.json(org.mockito.ArgumentMatchers.any())).thenReturn(context)

        // Act
        controller.getAll(context)

        // Assert
        verify(relationshipService).relationships
        verify(context).json(org.mockito.ArgumentMatchers.anyList<Any>())
        verify(context).status(HttpStatus.OK)
    }

    @Test
    fun getByFactionId_WithValidId_ShouldReturnRelationships() {
        // Arrange
        val factionId = MfFactionId(UUID.randomUUID())
        val relationship1 = createMockRelationship(factionId)
        val relationship2 = createMockRelationship(factionId)
        `when`(context.pathParam("id")).thenReturn(factionId.value.toString())
        `when`(relationshipService.getRelationships(factionId)).thenReturn(listOf(relationship1, relationship2))
        `when`(context.json(org.mockito.ArgumentMatchers.any())).thenReturn(context)

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
        `when`(context.json(org.mockito.ArgumentMatchers.any())).thenReturn(context)

        // Act
        controller.getByFactionId(context)

        // Assert
        verify(context).json(org.mockito.ArgumentMatchers.any())
        verify(context).status(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun getByFactionId_WithNoRelationships_ShouldReturnEmptyList() {
        // Arrange
        val factionId = MfFactionId(UUID.randomUUID())
        `when`(context.pathParam("id")).thenReturn(factionId.value.toString())
        `when`(relationshipService.getRelationships(factionId)).thenReturn(emptyList())
        `when`(context.json(org.mockito.ArgumentMatchers.any())).thenReturn(context)

        // Act
        controller.getByFactionId(context)

        // Assert
        verify(relationshipService).getRelationships(factionId)
        verify(context).json(org.mockito.ArgumentMatchers.anyList<Any>())
        verify(context).status(HttpStatus.OK)
    }

    private fun createMockRelationship(
        factionId: MfFactionId = MfFactionId(UUID.randomUUID()),
        targetId: MfFactionId = MfFactionId(UUID.randomUUID())
    ): MfFactionRelationship {
        val relationship = mock(MfFactionRelationship::class.java)
        `when`(relationship.factionId).thenReturn(factionId)
        `when`(relationship.targetId).thenReturn(targetId)
        `when`(relationship.relationshipType).thenReturn(MfFactionRelationshipType.ALLY)
        return relationship
    }
}
