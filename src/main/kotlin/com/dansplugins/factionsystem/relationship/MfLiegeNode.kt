package com.dansplugins.factionsystem.relationship

import com.dansplugins.factionsystem.faction.MfFactionId

data class MfLiegeNode(val factionId: MfFactionId, val liege: MfLiegeNode?) {
    fun contains(factionId: MfFactionId): Boolean {
        return liege != null && (liege.factionId == factionId || liege.contains(factionId))
    }
}