package com.dansplugins.factionsystem.relationship

import com.dansplugins.factionsystem.faction.MfFactionId

data class MfFactionRelationship(
    @get:JvmName("getId")
    val id: MfFactionRelationshipId = MfFactionRelationshipId.generate(),
    @get:JvmName("getFactionId")
    val factionId: MfFactionId,
    @get:JvmName("getTargetId")
    val targetId: MfFactionId,
    val type: MfFactionRelationshipType
)
