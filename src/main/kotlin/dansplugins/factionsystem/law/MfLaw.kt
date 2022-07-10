package dansplugins.factionsystem.law

import dansplugins.factionsystem.faction.MfFaction
import dansplugins.factionsystem.faction.MfFactionId

data class MfLaw(
    val id: MfLawId,
    val version: Int,
    val factionId: MfFactionId,
    val text: String
) {
    constructor(faction: MfFaction, text: String) : this(
        MfLawId.generate(),
        1,
        faction.id,
        text
    )
}