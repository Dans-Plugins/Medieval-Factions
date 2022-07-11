package com.dansplugins.factionsystem.relationship

import com.dansplugins.factionsystem.faction.MfFactionId

data class MfFactionRelationship(
    val id: MfFactionRelationshipId = MfFactionRelationshipId.generate(),
    val factionId: MfFactionId,
    val targetId: MfFactionId,
    val type: MfFactionRelationshipType
)