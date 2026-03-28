package com.dansplugins.factionsystem.api.controller

import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.MfFactionService
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID

class FactionControllerTest {

    private lateinit var factionService: MfFactionService
    private lateinit var controller: FactionController
    private lateinit var context: Context

    @BeforeEach
    fun setUp() {
        factionService = mock(MfFactionService::class.java)
        controller = FactionController(factionService)
        context = mock(Context::class.java)
    }

    @Test
    fun getAll_ShouldReturnAllFactions() {
        // Arrange
        val faction1 = createMockFaction("Faction1")
        val faction2 = createMockFaction("Faction2")
        `when`(factionService.factions).thenReturn(listOf(faction1, faction2))
        `when`(context.json(org.mockito.ArgumentMatchers.any())).thenReturn(context)

        // Act
        controller.getAll(context)

        // Assert
        verify(factionService).factions
        verify(context).json(org.mockito.ArgumentMatchers.anyList<Any>())
        verify(context).status(HttpStatus.OK)
    }

    @Test
    fun getById_WithValidId_ShouldReturnFaction() {
        // Arrange
        val factionId = MfFactionId(UUID.randomUUID())
        val faction = createMockFaction("TestFaction", factionId)
        `when`(context.pathParam("id")).thenReturn(factionId.value.toString())
        `when`(factionService.getFaction(factionId)).thenReturn(faction)
        `when`(context.json(org.mockito.ArgumentMatchers.any())).thenReturn(context)

        // Act
        controller.getById(context)

        // Assert
        verify(factionService).getFaction(factionId)
        verify(context).json(org.mockito.ArgumentMatchers.any())
        verify(context).status(HttpStatus.OK)
    }

    @Test
    fun getById_WithInvalidId_ShouldReturnBadRequest() {
        // Arrange
        `when`(context.pathParam("id")).thenReturn("invalid-uuid")
        `when`(context.json(org.mockito.ArgumentMatchers.any())).thenReturn(context)

        // Act
        controller.getById(context)

        // Assert
        verify(context).json(org.mockito.ArgumentMatchers.any())
        verify(context).status(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun getById_WithNonExistentId_ShouldReturnNotFound() {
        // Arrange
        val factionId = MfFactionId(UUID.randomUUID())
        `when`(context.pathParam("id")).thenReturn(factionId.value.toString())
        `when`(factionService.getFaction(factionId)).thenReturn(null)
        `when`(context.json(org.mockito.ArgumentMatchers.any())).thenReturn(context)

        // Act
        controller.getById(context)

        // Assert
        verify(factionService).getFaction(factionId)
        verify(context).json(org.mockito.ArgumentMatchers.any())
        verify(context).status(HttpStatus.NOT_FOUND)
    }

    @Test
    fun getByName_WithValidName_ShouldReturnFaction() {
        // Arrange
        val factionName = "TestFaction"
        val faction = createMockFaction(factionName)
        `when`(context.pathParam("name")).thenReturn(factionName)
        `when`(factionService.getFaction(factionName)).thenReturn(faction)
        `when`(context.json(org.mockito.ArgumentMatchers.any())).thenReturn(context)

        // Act
        controller.getByName(context)

        // Assert
        verify(factionService).getFaction(factionName)
        verify(context).json(org.mockito.ArgumentMatchers.any())
        verify(context).status(HttpStatus.OK)
    }

    @Test
    fun getByName_WithNonExistentName_ShouldReturnNotFound() {
        // Arrange
        val factionName = "NonExistent"
        `when`(context.pathParam("name")).thenReturn(factionName)
        `when`(factionService.getFaction(factionName)).thenReturn(null)
        `when`(context.json(org.mockito.ArgumentMatchers.any())).thenReturn(context)

        // Act
        controller.getByName(context)

        // Assert
        verify(factionService).getFaction(factionName)
        verify(context).json(org.mockito.ArgumentMatchers.any())
        verify(context).status(HttpStatus.NOT_FOUND)
    }

    private fun createMockFaction(name: String, id: MfFactionId = MfFactionId(UUID.randomUUID())): MfFaction {
        val faction = mock(MfFaction::class.java)
        `when`(faction.id).thenReturn(id)
        `when`(faction.name).thenReturn(name)
        `when`(faction.description).thenReturn("Test description")
        `when`(faction.prefix).thenReturn(null)
        `when`(faction.power).thenReturn(100.0)
        `when`(faction.maxPower).thenReturn(200.0)
        `when`(faction.members).thenReturn(emptyList())
        `when`(faction.home).thenReturn(null)
        return faction
    }
}
