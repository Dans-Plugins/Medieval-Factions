package com.dansplugins.factionsystem.service

import com.dansplugins.factionsystem.chat.MfChatService
import com.dansplugins.factionsystem.claim.MfClaimService
import com.dansplugins.factionsystem.duel.MfDuelService
import com.dansplugins.factionsystem.dynmap.MfDynmapService
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.gate.MfGateService
import com.dansplugins.factionsystem.interaction.MfInteractionService
import com.dansplugins.factionsystem.law.MfLawService
import com.dansplugins.factionsystem.locks.MfLockService
import com.dansplugins.factionsystem.notification.MfNotificationService
import com.dansplugins.factionsystem.player.MfPlayerService
import com.dansplugins.factionsystem.potion.MfPotionService
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipService
import com.dansplugins.factionsystem.teleport.MfTeleportService

class Services(
    val playerService: MfPlayerService,
    val factionService: MfFactionService,
    val lawService: MfLawService,
    val factionRelationshipService: MfFactionRelationshipService,
    val claimService: MfClaimService,
    val lockService: MfLockService,
    val interactionService: MfInteractionService,
    val notificationService: MfNotificationService,
    val gateService: MfGateService,
    val chatService: MfChatService,
    val duelService: MfDuelService,
    val potionService: MfPotionService,
    val teleportService: MfTeleportService,
    val dynmapService: MfDynmapService?
)
