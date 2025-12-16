package com.dansplugins.factionsystem.duel

import java.util.UUID

@JvmInline
value class MfDuelId(
    val value: String
) {
    companion object {
        fun generate() = MfDuelId(UUID.randomUUID().toString())
    }
}
