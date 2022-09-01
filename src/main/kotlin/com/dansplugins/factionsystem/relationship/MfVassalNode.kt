package com.dansplugins.factionsystem.relationship

import com.dansplugins.factionsystem.faction.MfFactionId

data class MfVassalNode(val factionId: MfFactionId, val vassals: List<MfVassalNode>) {
    fun contains(factionId: MfFactionId): Boolean {
        return vassals.any { it.factionId == factionId || it.contains(factionId) }
    }
}