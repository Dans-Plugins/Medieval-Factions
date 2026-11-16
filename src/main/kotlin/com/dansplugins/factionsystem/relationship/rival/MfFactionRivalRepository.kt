package com.dansplugins.factionsystem.relationship.rival

import com.dansplugins.factionsystem.faction.MfFactionId

interface MfFactionRivalRepository {
    fun getRivals(): List<MfFactionRival>
    fun insert(rival: MfFactionRival): MfFactionRival
    fun delete(factionId: MfFactionId, targetId: MfFactionId)
}
