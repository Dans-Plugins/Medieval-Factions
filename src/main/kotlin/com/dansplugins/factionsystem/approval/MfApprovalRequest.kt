package com.dansplugins.factionsystem.approval

import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.player.MfPlayerId
import java.time.Instant

data class MfApprovalRequest(
    @get:JvmName("getId")
    val id: MfApprovalRequestId = MfApprovalRequestId.generate(),
    @get:JvmName("getFactionId")
    val factionId: MfFactionId,
    @get:JvmName("getTargetId")
    val targetId: MfFactionId,
    val type: MfApprovalRequestType,
    @get:JvmName("getRequesterId")
    val requesterId: MfPlayerId,
    val reason: String? = null,
    val createdAt: Instant = Instant.now()
)
