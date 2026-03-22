package com.dansplugins.factionsystem.approval

import com.dansplugins.factionsystem.TestUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MfApprovalRequestServiceTest {

    private val testUtils = TestUtils()
    private lateinit var service: MfApprovalRequestService

    @BeforeEach
    fun setUp() {
        service = MfApprovalRequestService()
    }

    @Test
    fun testAddRequest() {
        val factionId = testUtils.createFactionId()
        val targetId = testUtils.createFactionId()
        val requesterId = testUtils.createPlayerId()
        val request = MfApprovalRequest(
            factionId = factionId,
            targetId = targetId,
            type = MfApprovalRequestType.WAR,
            requesterId = requesterId,
            reason = "Test reason"
        )

        val result = service.addRequest(request)

        assertEquals(request.id, result.id)
        assertEquals(factionId, result.factionId)
        assertEquals(targetId, result.targetId)
        assertEquals(MfApprovalRequestType.WAR, result.type)
        assertEquals(requesterId, result.requesterId)
        assertEquals("Test reason", result.reason)
    }

    @Test
    fun testGetRequest() {
        val request = createTestRequest(MfApprovalRequestType.WAR)
        service.addRequest(request)

        val retrieved = service.getRequest(request.id)

        assertNotNull(retrieved)
        assertEquals(request.id, retrieved!!.id)
    }

    @Test
    fun testGetRequestNotFound() {
        val result = service.getRequest(MfApprovalRequestId.generate())

        assertNull(result)
    }

    @Test
    fun testGetAllRequests() {
        val request1 = createTestRequest(MfApprovalRequestType.WAR)
        val request2 = createTestRequest(MfApprovalRequestType.ALLY)
        service.addRequest(request1)
        service.addRequest(request2)

        val allRequests = service.getAllRequests()

        assertEquals(2, allRequests.size)
    }

    @Test
    fun testRemoveRequest() {
        val request = createTestRequest(MfApprovalRequestType.WAR)
        service.addRequest(request)

        val removed = service.removeRequest(request.id)

        assertNotNull(removed)
        assertEquals(request.id, removed!!.id)
        assertNull(service.getRequest(request.id))
    }

    @Test
    fun testRemoveRequestNotFound() {
        val result = service.removeRequest(MfApprovalRequestId.generate())

        assertNull(result)
    }

    @Test
    fun testHasPendingRequest() {
        val factionId = testUtils.createFactionId()
        val targetId = testUtils.createFactionId()
        val request = MfApprovalRequest(
            factionId = factionId,
            targetId = targetId,
            type = MfApprovalRequestType.ALLY,
            requesterId = testUtils.createPlayerId()
        )
        service.addRequest(request)

        assertTrue(service.hasPendingRequest(factionId, targetId, MfApprovalRequestType.ALLY))
        assertFalse(service.hasPendingRequest(factionId, targetId, MfApprovalRequestType.WAR))
        assertFalse(service.hasPendingRequest(targetId, factionId, MfApprovalRequestType.ALLY))
    }

    @Test
    fun testHasPendingRequestAfterRemoval() {
        val factionId = testUtils.createFactionId()
        val targetId = testUtils.createFactionId()
        val request = MfApprovalRequest(
            factionId = factionId,
            targetId = targetId,
            type = MfApprovalRequestType.VASSALIZE,
            requesterId = testUtils.createPlayerId()
        )
        service.addRequest(request)

        assertTrue(service.hasPendingRequest(factionId, targetId, MfApprovalRequestType.VASSALIZE))
        service.removeRequest(request.id)
        assertFalse(service.hasPendingRequest(factionId, targetId, MfApprovalRequestType.VASSALIZE))
    }

    @Test
    fun testRequestWithNullReason() {
        val request = MfApprovalRequest(
            factionId = testUtils.createFactionId(),
            targetId = testUtils.createFactionId(),
            type = MfApprovalRequestType.WAR,
            requesterId = testUtils.createPlayerId(),
            reason = null
        )

        val result = service.addRequest(request)

        assertNull(result.reason)
    }

    private fun createTestRequest(type: MfApprovalRequestType): MfApprovalRequest {
        return MfApprovalRequest(
            factionId = testUtils.createFactionId(),
            targetId = testUtils.createFactionId(),
            type = type,
            requesterId = testUtils.createPlayerId()
        )
    }
}
