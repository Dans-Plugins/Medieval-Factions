package com.dansplugins.factionsystem.gate

import java.util.*

@JvmInline
value class MfGateId(val value: String) {
    companion object {
        fun generate() = MfGateId(UUID.randomUUID().toString())
    }
}
