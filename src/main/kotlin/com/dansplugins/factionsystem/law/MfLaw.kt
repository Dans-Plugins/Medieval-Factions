package com.dansplugins.factionsystem.law

import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId

data class MfLaw(
    val id: MfLawId,
    val version: Int = 0,
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