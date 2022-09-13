package com.dansplugins.factionsystem.service

import com.dansplugins.factionsystem.claim.MfClaimService
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.interaction.MfInteractionService
import com.dansplugins.factionsystem.law.MfLawService
import com.dansplugins.factionsystem.locks.MfLockService
import com.dansplugins.factionsystem.player.MfPlayerService
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipService

class Services(
    val playerService: MfPlayerService,
    val factionService: MfFactionService,
    val lawService: MfLawService,
    val factionRelationshipService: MfFactionRelationshipService,
    val claimService: MfClaimService,
    val lockService: MfLockService,
    val interactionService: MfInteractionService
)