package com.dansplugins.factionsystem.law

import java.util.*

@JvmInline
value class MfLawId(val value: String) {
    companion object {
        fun generate() = MfLawId(UUID.randomUUID().toString())
    }
}
