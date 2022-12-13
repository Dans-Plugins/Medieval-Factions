package com.dansplugins.factionsystem.relationship

import com.dansplugins.factionsystem.faction.MfFactionId

data class MfLiegeNode(
    @get:JvmName("getFactionId")
    val factionId: MfFactionId,
    val liege: MfLiegeNode?
) {
    @JvmName("contains")
    fun contains(factionId: MfFactionId): Boolean {
        return liege != null && (liege.factionId == factionId || liege.contains(factionId))
    }

    fun last(): MfLiegeNode = liege?.last() ?: this
}
