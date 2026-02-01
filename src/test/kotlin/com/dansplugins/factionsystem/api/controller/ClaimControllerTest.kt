package com.dansplugins.factionsystem.api.controller

import com.dansplugins.factionsystem.claim.MfClaimService
import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.faction.MfFactionId
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID

class ClaimControllerTest {

    private lateinit var claimService: MfClaimService
    private lateinit var controller: ClaimController
    private lateinit var context: Context

    @BeforeEach
    fun setUp() {
        claimService = mock(MfClaimService::class.java)
        controller = ClaimController(claimService)
        context = mock(Context::class.java)
    }

    @Test
    fun getAll_ShouldReturnAllClaims() {
        // Arrange
        val claim1 = createMockClaim()
        val claim2 = createMockClaim()
        `when`(claimService.getClaims()).thenReturn(listOf(claim1, claim2))
        `when`(context.json(org.mockito.ArgumentMatchers.any())).thenReturn(context)

        // Act
        controller.getAll(context)

        // Assert
        verify(claimService).getClaims()
        verify(context).json(org.mockito.ArgumentMatchers.anyList<Any>())
        verify(context).status(HttpStatus.OK)
    }

    @Test
    fun getByFactionId_WithValidId_ShouldReturnClaims() {
        // Arrange
        val factionId = MfFactionId(UUID.randomUUID())
        val claim1 = createMockClaim(factionId)
        val claim2 = createMockClaim(factionId)
        `when`(context.pathParam("id")).thenReturn(factionId.value.toString())
        `when`(claimService.getClaims(factionId)).thenReturn(listOf(claim1, claim2))
        `when`(context.json(org.mockito.ArgumentMatchers.any())).thenReturn(context)

        // Act
        controller.getByFactionId(context)

        // Assert
        verify(claimService).getClaims(factionId)
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
    fun getByFactionId_WithNoClaimsForFaction_ShouldReturnEmptyList() {
        // Arrange
        val factionId = MfFactionId(UUID.randomUUID())
        `when`(context.pathParam("id")).thenReturn(factionId.value.toString())
        `when`(claimService.getClaims(factionId)).thenReturn(emptyList())
        `when`(context.json(org.mockito.ArgumentMatchers.any())).thenReturn(context)

        // Act
        controller.getByFactionId(context)

        // Assert
        verify(claimService).getClaims(factionId)
        verify(context).json(org.mockito.ArgumentMatchers.anyList<Any>())
        verify(context).status(HttpStatus.OK)
    }

    private fun createMockClaim(factionId: MfFactionId = MfFactionId(UUID.randomUUID())): MfClaimedChunk {
        val claim = mock(MfClaimedChunk::class.java)
        `when`(claim.worldId).thenReturn(UUID.randomUUID())
        `when`(claim.x).thenReturn(10)
        `when`(claim.z).thenReturn(20)
        `when`(claim.factionId).thenReturn(factionId)
        return claim
    }
}
