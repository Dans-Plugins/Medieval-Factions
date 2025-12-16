package com.dansplugins.factionsystem.relationship

import java.util.UUID

@JvmInline
value class MfFactionRelationshipId(
    val value: String
) {
    companion object {
        fun generate() = MfFactionRelationshipId(UUID.randomUUID().toString())
    }
}
