package com.dansplugins.factionsystem.lockedarea

import java.util.UUID

@JvmInline
value class MfLockedAreaId(val value: String) {
    companion object {
        fun generate() = MfLockedAreaId(UUID.randomUUID().toString())
    }
}
