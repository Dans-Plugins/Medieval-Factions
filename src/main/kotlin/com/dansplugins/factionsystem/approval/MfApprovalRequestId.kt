package com.dansplugins.factionsystem.approval

import java.util.UUID

@JvmInline
value class MfApprovalRequestId(val value: String) {
    companion object {
        fun generate() = MfApprovalRequestId(UUID.randomUUID().toString())
    }
}
