package com.dansplugins.factionsystem.relationship

import com.dansplugins.factionsystem.faction.MfFactionId

data class MfVassalNode(val factionId: MfFactionId, val vassals: List<MfVassalNode>) {
    fun contains(factionId: MfFactionId): Boolean {
        return vassals.any { it.factionId == factionId || it.contains(factionId) }
    }

    fun <R> flatMap(transform: (MfFactionId) -> Iterable<R>): List<R> {
        return vassals.flatMap { vassal -> vassal.flatMap(transform) + transform(vassal.factionId) }
    }

    fun <R> map(transform: (MfFactionId) -> R): List<R> {
        return vassals.flatMap { vassal -> vassal.map(transform) + transform(vassal.factionId) }
    }

    fun <R> mapNotNull(transform: (MfFactionId) -> R?): List<R> {
        return vassals.flatMap { vassal ->
            val results = vassal.mapNotNull(transform).toMutableList()
            val result = transform(vassal.factionId)
            if (result != null) {
                results.add(result)
            }
            return@flatMap results
        }
    }

    fun forEach(action: (MfFactionId) -> Unit) {
        vassals.forEach { vassal ->
            vassal.forEach(action)
            action(vassal.factionId)
        }
    }
}