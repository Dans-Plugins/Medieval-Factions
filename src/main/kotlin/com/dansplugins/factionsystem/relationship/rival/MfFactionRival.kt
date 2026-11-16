package com.dansplugins.factionsystem.relationship.rival

import com.dansplugins.factionsystem.faction.MfFactionId
import java.time.Instant

/**
 * [factionId] has named [targetId] as a rival (#1896). A rivalry is one-sided: the target does not have to agree, and
 * it is not a relationship type. Rivals are kept apart from `MfFactionRelationshipType` so that a server going back
 * to a version without rivals never reads a relationship type it does not know.
 */
data class MfFactionRival(
    @get:JvmName("getFactionId")
    val factionId: MfFactionId,
    @get:JvmName("getTargetId")
    val targetId: MfFactionId,
    val createdAt: Instant = Instant.now()
)
