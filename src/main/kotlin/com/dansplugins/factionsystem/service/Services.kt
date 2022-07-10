package com.dansplugins.factionsystem.service

import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.law.MfLawService
import com.dansplugins.factionsystem.player.MfPlayerService

class Services(
    val playerService: MfPlayerService,
    val factionService: MfFactionService,
    val lawService: MfLawService
)