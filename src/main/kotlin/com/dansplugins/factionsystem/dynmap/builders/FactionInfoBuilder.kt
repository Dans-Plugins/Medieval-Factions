package com.dansplugins.factionsystem.dynmap.builders

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import kotlin.math.floor
import kotlin.math.roundToInt

class FactionInfoBuilder(private val plugin: MedievalFactions) {

    private val decimalFormat = DecimalFormat("0", DecimalFormatSymbols.getInstance(plugin.language.locale))

    fun build(faction: MfFaction): String {
        val factionService = plugin.services.factionService
        val relationshipService = plugin.services.factionRelationshipService
        val claimService = plugin.services.claimService
        val playerService = plugin.services.playerService
        return buildString {
            append("<h1>${faction.name}</h1>")
            append("<h2>Description</h2>")
            append(faction.description)
            append("<h2>Members (${faction.members.size})</h2>")
            append(
                faction.members.groupBy { it.role }.map { (role, members) ->
                    """
                        <h3>${role.name} (${faction.members.count { it.role.id == role.id }})</h3>
                        ${members.joinToString { member -> playerService.getPlayer(member.playerId)?.name ?: plugin.language["UnknownPlayer"] }}
                    """.trimIndent()
                }.joinToString("<br />")
            )
            val liegeId = relationshipService.getLiege(faction.id)
            val liege = liegeId?.let(factionService::getFaction)
            if (liege != null) {
                append("<h2>Liege</h2>")
                append(liege.name)
                append("<br />")
            }
            val vassals = relationshipService.getVassals(faction.id).mapNotNull(factionService::getFaction)
            if (vassals.isNotEmpty()) {
                append("<h2>Vassals</h2>")
                append(vassals.joinToString(transform = MfFaction::name))
                append("<br />")
            }
            val allies = relationshipService.getAllies(faction.id).mapNotNull(factionService::getFaction)
            if (allies.isNotEmpty()) {
                append("<h2>Allies</h2>")
                append(allies.joinToString(transform = MfFaction::name))
                append("<br />")
            }
            val atWarWith = relationshipService.getFactionsAtWarWith(faction.id).mapNotNull(factionService::getFaction)
            if (atWarWith.isNotEmpty()) {
                append("<h2>At war with</h2>")
                append(atWarWith.joinToString(transform = MfFaction::name))
                append("<br />")
            }
            append("<h2>Power</h2>")
            append(decimalFormat.format(floor(faction.power)))
            append("<br />")
            append("<h2>Demesne</h2>")
            val claims = claimService.getClaims(faction.id)
            append("${claims.size}/${floor(faction.power).roundToInt()}")
        }
    }
}
