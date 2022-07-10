package dansplugins.factionsystem.law

import dansplugins.factionsystem.faction.MfFactionId

interface MfLawRepository {

    fun getLaw(id: MfLawId): MfLaw?
    fun getLaws(factionId: MfFactionId): List<MfLaw>
    fun upsert(law: MfLaw): MfLaw
    fun delete(id: MfLawId)

}