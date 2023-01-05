package com.dansplugins.factionsystem.law

import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId

data class MfLaw(
    @get:JvmName("getId")
    val id: MfLawId,
    val version: Int = 0,
    @get:JvmName("getFactionId")
    val factionId: MfFactionId,
    val text: String,
    val number: Int?
) {
    constructor(faction: MfFaction, text: String) : this(
        MfLawId.generate(),
        1,
        faction.id,
        text,
        null
    )
}
