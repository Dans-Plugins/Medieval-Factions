package com.dansplugins.factionsystem.faction

import java.util.UUID

@JvmInline
value class MfFactionId(
    val value: String
) {
    companion object {
        fun generate() = MfFactionId(UUID.randomUUID().toString())
    }
}
