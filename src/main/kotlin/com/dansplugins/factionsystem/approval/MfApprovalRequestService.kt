package com.dansplugins.factionsystem.approval

import com.dansplugins.factionsystem.faction.MfFactionId
import java.util.concurrent.ConcurrentHashMap

class MfApprovalRequestService {

    private val pendingRequests = ConcurrentHashMap<MfApprovalRequestId, MfApprovalRequest>()

    fun addRequest(request: MfApprovalRequest): MfApprovalRequest {
        synchronized(this) {
            val existing = pendingRequests.values.firstOrNull {
                it.factionId == request.factionId &&
                    it.targetId == request.targetId &&
                    it.type == request.type
            }

            if (existing != null) {
                return existing
            }

            pendingRequests[request.id] = request
            return request
        }
    }

    fun getRequest(id: MfApprovalRequestId): MfApprovalRequest? = pendingRequests[id]

    fun getAllRequests(): List<MfApprovalRequest> = pendingRequests.values.toList()

    fun removeRequest(id: MfApprovalRequestId): MfApprovalRequest? = pendingRequests.remove(id)

    fun hasPendingRequest(factionId: MfFactionId, targetId: MfFactionId, type: MfApprovalRequestType): Boolean =
        pendingRequests.values.any { it.factionId == factionId && it.targetId == targetId && it.type == type }
}
