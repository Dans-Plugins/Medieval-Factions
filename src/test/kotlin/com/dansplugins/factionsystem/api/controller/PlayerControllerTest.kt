package com.dansplugins.factionsystem.api.controller

import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.MfFactionMember
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.player.MfPlayerService
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID

class PlayerControllerTest {

    private lateinit var playerService: MfPlayerService
    private lateinit var factionService: MfFactionService
    private lateinit var controller: PlayerController
    private lateinit var context: Context

    @BeforeEach
    fun setUp() {
        playerService = mock(MfPlayerService::class.java)
        factionService = mock(MfFactionService::class.java)
        controller = PlayerController(playerService, factionService)
        context = mock(Context::class.java)
    }

    @Test
    fun getAll_ShouldReturnAllPlayers() {
        // Arrange
        val player1 = createMockPlayer("Player1")
        val player2 = createMockPlayer("Player2")
        `when`(playerService.players).thenReturn(listOf(player1, player2))
        `when`(factionService.factions).thenReturn(emptyList())
        `when`(context.json(org.mockito.ArgumentMatchers.any())).thenReturn(context)

        // Act
        controller.getAll(context)

        // Assert
        verify(playerService).players
        verify(context).json(org.mockito.ArgumentMatchers.anyList<Any>())
        verify(context).status(HttpStatus.OK)
    }

    @Test
    fun getById_WithValidId_ShouldReturnPlayer() {
        // Arrange
        val playerId = MfPlayerId(UUID.randomUUID())
        val player = createMockPlayer("TestPlayer", playerId)
        `when`(context.pathParam("id")).thenReturn(playerId.value.toString())
        `when`(playerService.getPlayer(playerId)).thenReturn(player)
        `when`(context.json(org.mockito.ArgumentMatchers.any())).thenReturn(context)

        // Act
        controller.getById(context)

        // Assert
        verify(playerService).getPlayer(playerId)
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
        val playerId = MfPlayerId(UUID.randomUUID())
        `when`(context.pathParam("id")).thenReturn(playerId.value.toString())
        `when`(playerService.getPlayer(playerId)).thenReturn(null)
        `when`(context.json(org.mockito.ArgumentMatchers.any())).thenReturn(context)

        // Act
        controller.getById(context)

        // Assert
        verify(playerService).getPlayer(playerId)
        verify(context).json(org.mockito.ArgumentMatchers.any())
        verify(context).status(HttpStatus.NOT_FOUND)
    }

    @Test
    fun getById_WithPlayerInFaction_ShouldReturnPlayerWithFactionId() {
        // Arrange
        val playerId = MfPlayerId(UUID.randomUUID())
        val player = createMockPlayer("TestPlayer", playerId)
        val faction = mock(MfFaction::class.java)
        val factionId = MfFactionId(UUID.randomUUID())
        `when`(faction.id).thenReturn(factionId)
        `when`(context.pathParam("id")).thenReturn(playerId.value.toString())
        `when`(playerService.getPlayer(playerId)).thenReturn(player)
        `when`(factionService.getFaction(playerId)).thenReturn(faction)
        `when`(context.json(org.mockito.ArgumentMatchers.any())).thenReturn(context)

        // Act
        controller.getById(context)

        // Assert
        verify(playerService).getPlayer(playerId)
        verify(factionService).getFaction(playerId)
        verify(context).json(org.mockito.ArgumentMatchers.any())
        verify(context).status(HttpStatus.OK)
    }
    
    @Test
    fun getAll_WithPlayersInFactions_ShouldReturnPlayersWithFactionIds() {
        // Arrange
        val playerId1 = MfPlayerId(UUID.randomUUID())
        val playerId2 = MfPlayerId(UUID.randomUUID())
        val player1 = createMockPlayer("Player1", playerId1)
        val player2 = createMockPlayer("Player2", playerId2)
        
        val factionId = MfFactionId(UUID.randomUUID())
        val faction = mock(MfFaction::class.java)
        val member1 = mock(MfFactionMember::class.java)
        val member2 = mock(MfFactionMember::class.java)
        
        `when`(member1.playerId).thenReturn(playerId1)
        `when`(member2.playerId).thenReturn(playerId2)
        `when`(faction.id).thenReturn(factionId)
        `when`(faction.members).thenReturn(listOf(member1, member2))
        
        `when`(playerService.players).thenReturn(listOf(player1, player2))
        `when`(factionService.factions).thenReturn(listOf(faction))
        `when`(context.json(org.mockito.ArgumentMatchers.any())).thenReturn(context)

        // Act
        controller.getAll(context)

        // Assert
        verify(playerService).players
        verify(factionService).factions
        verify(context).json(org.mockito.ArgumentMatchers.any())
        verify(context).status(HttpStatus.OK)
    }

    private fun createMockPlayer(name: String, id: MfPlayerId = MfPlayerId(UUID.randomUUID())): MfPlayer {
        val player = mock(MfPlayer::class.java)
        `when`(player.id).thenReturn(id)
        `when`(player.name).thenReturn(name)
        `when`(player.power).thenReturn(10.0)
        return player
    }
}
