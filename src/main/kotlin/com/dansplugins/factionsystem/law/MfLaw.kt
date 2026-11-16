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
    val number: Int?,
    /** The free-text section the law is listed under, or null for none (#1650). Law numbers stay global across sections. */
    val section: String? = null
) {
    constructor(faction: MfFaction, text: String) : this(
        MfLawId.generate(),
        1,
        faction.id,
        text,
        null
    )

    companion object {
        /** The longest section name that is stored, matching the `mf_law.section` column. */
        const val MAX_SECTION_LENGTH = 64
    }
}
