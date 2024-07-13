package com.dansplugins.factionsystem.faction

import com.dansplugins.factionsystem.player.MfPlayerId

public data class MfFactionApplication(
    @get:JvmName("getApplicantId")
    val applicantId: MfPlayerId,
    @get:JvmName("getFactionId")
    val factionId: MfFactionId
)
