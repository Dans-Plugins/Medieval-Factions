package com.dansplugins.factionsystem.faction.role

import java.util.*

@JvmInline
value class MfFactionRoleId(val value: String) {
    companion object {
        fun generate() = MfFactionRoleId(UUID.randomUUID().toString())
    }
}
