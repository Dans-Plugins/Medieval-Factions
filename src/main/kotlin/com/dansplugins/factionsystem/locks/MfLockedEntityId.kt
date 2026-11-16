package com.dansplugins.factionsystem.locks

import java.util.*

@JvmInline
value class MfLockedEntityId(val value: String) {
    companion object {
        fun generate() = MfLockedEntityId(UUID.randomUUID().toString())
    }
}
