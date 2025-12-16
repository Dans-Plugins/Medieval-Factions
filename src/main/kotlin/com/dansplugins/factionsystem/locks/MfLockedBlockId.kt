package com.dansplugins.factionsystem.locks

import java.util.UUID

@JvmInline
value class MfLockedBlockId(
    val value: String
) {
    companion object {
        fun generate() = MfLockedBlockId(UUID.randomUUID().toString())
    }
}
