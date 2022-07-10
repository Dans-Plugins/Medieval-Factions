package dansplugins.factionsystem.service

import dansplugins.factionsystem.faction.MfFactionService
import dansplugins.factionsystem.law.MfLawService
import dansplugins.factionsystem.player.MfPlayerService

class Services(
    val playerService: MfPlayerService,
    val factionService: MfFactionService,
    val lawService: MfLawService
)