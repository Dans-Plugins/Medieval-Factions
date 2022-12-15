package com.dansplugins.factionsystem.duel

import java.util.*

@JvmInline
value class MfDuelId(val value: String) {
    companion object {
        fun generate() = MfDuelId(UUID.randomUUID().toString())
    }
}
