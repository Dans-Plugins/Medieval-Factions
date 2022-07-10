package com.dansplugins.factionsystem.relationship

import com.dansplugins.factionsystem.faction.MfFactionId

data class MfFactionRelationship(
    val factionId: MfFactionId,
    val targetId: MfFactionId,
    val relationshipType: MfFactionRelationshipType
)