package com.dansplugins.factionsystem.api.dto

import com.dansplugins.factionsystem.relationship.MfFactionRelationship

data class RelationshipDto(
    val factionId: String,
    val targetFactionId: String,
    val type: String
) {
    companion object {
        fun fromRelationship(relationship: MfFactionRelationship): RelationshipDto {
            return RelationshipDto(
                factionId = relationship.factionId.value.toString(),
                targetFactionId = relationship.targetId.value.toString(),
                type = relationship.relationshipType.name
            )
        }
    }
}
