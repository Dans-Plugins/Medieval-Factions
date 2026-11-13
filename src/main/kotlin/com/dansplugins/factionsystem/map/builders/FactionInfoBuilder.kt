package com.dansplugins.factionsystem.map.builders

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * A builder class for creating faction information in HTML format.
 *
 * @param plugin The MedievalFactions plugin instance.
 */
class FactionInfoBuilder(private val plugin: MedievalFactions) {

    private val decimalFormat = DecimalFormat("0", DecimalFormatSymbols.getInstance(plugin.language.locale))

    /**
     * Builds the faction information as an HTML string.
     *
     * @param faction The faction for which the information is being built.
     * @return The faction information in HTML format.
     */
    fun build(faction: MfFaction): String {
        val factionService = plugin.services.factionService
        val relationshipService = plugin.services.factionRelationshipService
        val claimService = plugin.services.claimService
        val playerService = plugin.services.playerService
        return buildString {
            append("<h1>${faction.name.escapeHtml()}</h1>")
            if (plugin.config.getBoolean("dynmap.showDescription")) {
                append("<h2>Description</h2>")
                append(faction.description.escapeHtml())
            }
            if (plugin.config.getBoolean("dynmap.showMembers")) {
                append("<h2>Members (${faction.members.size})</h2>")
                append(
                    faction.members.groupBy { it.role }.map { (role, members) ->
                        """
                <h3>${role.name.escapeHtml()} (${faction.members.count { it.role.id == role.id }})</h3>
                ${members.joinToString { member -> (playerService.getPlayer(member.playerId)?.name ?: plugin.language["UnknownPlayer"]).escapeHtml() }}
                        """.trimIndent()
                    }.joinToString("<br />")
                )
            }
            if (plugin.config.getBoolean("dynmap.showLiege")) {
                val liegeId = relationshipService.getLiege(faction.id)
                val liege = liegeId?.let(factionService::getFaction)
                if (liege != null) {
                    append("<h2>Liege</h2>")
                    append(liege.name.escapeHtml())
                    append("<br />")
                }
            }
            if (plugin.config.getBoolean("dynmap.showVassals")) {
                val vassals = relationshipService.getVassals(faction.id).mapNotNull(factionService::getFaction)
                if (vassals.isNotEmpty()) {
                    append("<h2>Vassals</h2>")
                    append(vassals.joinToString { it.name.escapeHtml() })
                    append("<br />")
                }
            }
            if (plugin.config.getBoolean("dynmap.showAllies")) {
                val allies = relationshipService.getAllies(faction.id).mapNotNull(factionService::getFaction)
                if (allies.isNotEmpty()) {
                    append("<h2>Allies</h2>")
                    append(allies.joinToString { it.name.escapeHtml() })
                    append("<br />")
                }
            }
            if (plugin.config.getBoolean("dynmap.showAtWarWith")) {
                val atWarWith = relationshipService.getFactionsAtWarWith(faction.id).mapNotNull(factionService::getFaction)
                if (atWarWith.isNotEmpty()) {
                    append("<h2>At war with</h2>")
                    append(atWarWith.joinToString { it.name.escapeHtml() })
                    append("<br />")
                }
            }
            if (plugin.config.getBoolean("dynmap.showPower")) {
                append("<h2>Power</h2>")
                append(decimalFormat.format(floor(faction.power)))
                append("<br />")
            }
            if (plugin.config.getBoolean("dynmap.showDemesne")) {
                append("<h2>Demesne</h2>")
                val claims = claimService.getClaims(faction.id)
                append("${claims.size}/${floor(faction.power).roundToInt()}")
            }
        }
    }

    /**
     * Escapes the characters that are significant in HTML, so that player-controlled text such as faction
     * names and descriptions is shown as text in the Dynmap popup rather than interpreted as markup.
     */
    private fun String.escapeHtml(): String = buildString(length) {
        for (char in this@escapeHtml) {
            when (char) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&#39;")
                else -> append(char)
            }
        }
    }
}
