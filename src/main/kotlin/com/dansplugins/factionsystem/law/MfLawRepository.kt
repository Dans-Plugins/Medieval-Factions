package com.dansplugins.factionsystem.law

import com.dansplugins.factionsystem.faction.MfFactionId

interface MfLawRepository {

    fun getLaw(id: MfLawId): MfLaw?
    fun getLaw(factionId: MfFactionId, index: Int?): MfLaw?
    fun getLaws(factionId: MfFactionId): List<MfLaw>
    fun upsert(law: MfLaw): MfLaw
    fun delete(id: MfLawId)
    fun delete(law: MfLaw)
    fun move(law: MfLaw, number: Int)
}
