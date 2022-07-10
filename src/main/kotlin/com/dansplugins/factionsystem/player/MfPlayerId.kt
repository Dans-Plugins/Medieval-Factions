package com.dansplugins.factionsystem.player

import java.util.*

@JvmInline
value class MfPlayerId(val value: String) {
    companion object {
        fun generate() = MfPlayerId(UUID.randomUUID().toString())
    }
}