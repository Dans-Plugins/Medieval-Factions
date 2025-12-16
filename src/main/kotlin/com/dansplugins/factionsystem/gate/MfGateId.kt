package com.dansplugins.factionsystem.gate

import java.util.UUID

@JvmInline
value class MfGateId(
    val value: String
) {
    companion object {
        fun generate() = MfGateId(UUID.randomUUID().toString())
    }
}
