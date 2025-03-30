package com.dansplugins.factionsystem.placeholder

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.flag.MfFlag
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.ALLY
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.AT_WAR
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.LIEGE
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType.VASSAL
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import me.clip.placeholderapi.expansion.Relational
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.ChatColor.WHITE
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import kotlin.math.floor

class MedievalFactionsPlaceholderExpansion(private val plugin: MedievalFactions) : PlaceholderExpansion(), Relational {

    private val decimalFormat = DecimalFormat("0", DecimalFormatSymbols.getInstance(plugin.language.locale))

    override fun getIdentifier() = plugin.name
    override fun getAuthor() = plugin.description.authors.joinToString()
    override fun getVersion() = plugin.description.version
    override fun persist() = true
    override fun canRegister() = true

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        if (player == null) return null
        return when (val paramsLowercase = params.lowercase()) {
            "faction_name" -> getFactionName(player)
            "faction_prefix" -> getFactionPrefix(player)
            "faction_total_claimed_chunks" -> getFactionTotalClaimedChunks(player)
            "faction_cumulative_power" -> getFactionPower(player)
            "faction_bonus_power" -> getFactionBonusPower(player)
            "faction_power" -> getFactionPowerWithoutBonus(player)
            "faction_ally_count" -> getFactionAllyCount(player)
            "faction_enemy_count" -> getFactionEnemyCount(player)
            "faction_vassal_count" -> getFactionVassalCount(player)
            "faction_gate_count" -> getFactionGateCount(player)
            "faction_liege" -> getFactionLiege(player)
            "faction_population" -> getFactionPopulation(player)
            "faction_rank" -> getFactionRole(player)
            "faction_player_power" -> getPower(player)
            "faction_player_max_power" -> getMaxPower()
            "faction_player_power_full" -> getFormattedPlayerPower(player)
            "faction_at_location" -> getFactionAtLocation(player)
            "faction_color", "faction_colour" -> getFactionColor(player)
            "player_chunk_location" -> getPlayerChunkLocation(player)
            "player_location" -> getPlayerLocation(player)
            "player_world" -> getPlayerWorld(player)
            else -> {
                val factionService = plugin.services.factionService
                val factions = factionService.factions
                if (paramsLowercase.startsWith("faction_") && paramsLowercase.endsWith("_enemy")) {
                    val matchingEnemyFaction =
                        factions.singleOrNull { paramsLowercase == "faction_${it.name.lowercase()}_enemy" }
                    if (matchingEnemyFaction != null) return isPlayerFactionEnemy(player, matchingEnemyFaction)
                }
                if (paramsLowercase.startsWith("faction_") && paramsLowercase.endsWith("ally")) {
                    val matchingAllyFaction = factions.singleOrNull { paramsLowercase == "faction_${it.name.lowercase()}_ally" }
                    if (matchingAllyFaction != null) return isPlayerFactionAlly(player, matchingAllyFaction)
                }
                if (paramsLowercase.startsWith("faction_flag_")) {
                    val matchingFlag = plugin.flags.singleOrNull { paramsLowercase == "faction_flag_" + it.name }
                    if (matchingFlag != null) return getFactionFlagValue(player, matchingFlag)
                }
                return null
            }
        }
    }

    override fun onPlaceholderRequest(one: Player?, two: Player?, identifier: String): String? {
        if (one == null || two == null) return null
        return when (identifier) {
            "faction_enemies" -> getPlayerFactionEnemies(one, two)
            "faction_allies" -> getPlayerFactionAllies(one, two)
            "faction_vassal" -> getPlayerFactionVassal(one, two)
            "faction_liege" -> getPlayerFactionLiege(one, two)
            else -> null
        }
    }

    private fun getPlayerFaction(player: OfflinePlayer): MfFaction? {
        val playerService = plugin.services.playerService
        val mfPlayer = playerService.getPlayer(player) ?: return null
        val factionService = plugin.services.factionService
        return factionService.getFaction(mfPlayer.id)
    }

    private fun getFactionName(player: OfflinePlayer): String {
        val faction = getPlayerFaction(player)
            ?: return (plugin.config.getString("factions.factionlessFactionName") ?: "Factionless")
        return faction.name
    }

    private fun getFactionPrefix(player: OfflinePlayer): String {
        val faction = getPlayerFaction(player)
            ?: return (plugin.config.getString("factions.factionlessFactionName") ?: "Factionless")
        return faction.prefix ?: faction.name
    }

    private fun getFactionTotalClaimedChunks(player: OfflinePlayer): String {
        val faction = getPlayerFaction(player)
            ?: return (plugin.config.getString("factions.factionlessFactionName") ?: "Factionless")
        val claimService = plugin.services.claimService
        return claimService.getClaims(faction.id).size.toString()
    }

    private fun getFactionPower(player: OfflinePlayer): String {
        val faction = getPlayerFaction(player)
            ?: return (plugin.config.getString("factions.factionlessFactionName") ?: "Factionless")
        return decimalFormat.format(floor(faction.power))
    }

    private fun getFactionBonusPower(player: OfflinePlayer): String {
        val faction = getPlayerFaction(player)
            ?: return (plugin.config.getString("factions.factionlessFactionName") ?: "Factionless")
        return decimalFormat.format(if (faction.flags[plugin.flags.acceptBonusPower]) floor(faction.bonusPower) else 0.0)
    }

    private fun getFactionPowerWithoutBonus(player: OfflinePlayer): String {
        val faction = getPlayerFaction(player)
            ?: return (plugin.config.getString("factions.factionlessFactionName") ?: "Factionless")
        return decimalFormat.format(floor(faction.power - (if (faction.flags[plugin.flags.acceptBonusPower]) faction.bonusPower else 0.0)))
    }

    private fun getFactionAllyCount(player: OfflinePlayer): String {
        val faction = getPlayerFaction(player)
            ?: return (plugin.config.getString("factions.factionlessFactionName") ?: "Factionless")
        val factionService = plugin.services.factionService
        val relationshipService = plugin.services.factionRelationshipService
        return factionService.factions.count {
            relationshipService.getRelationships(faction.id, it.id).any { relationship -> relationship.type == ALLY } &&
                relationshipService.getRelationships(it.id, faction.id).any { relationship -> relationship.type == ALLY }
        }.toString()
    }

    private fun getFactionEnemyCount(player: OfflinePlayer): String {
        val faction = getPlayerFaction(player)
            ?: return (plugin.config.getString("factions.factionlessFactionName") ?: "Factionless")
        val factionService = plugin.services.factionService
        val relationshipService = plugin.services.factionRelationshipService
        return factionService.factions.count {
            relationshipService.getRelationships(faction.id, it.id).any { relationship -> relationship.type == AT_WAR } &&
                relationshipService.getRelationships(it.id, faction.id).any { relationship -> relationship.type == AT_WAR }
        }.toString()
    }

    private fun getFactionVassalCount(player: OfflinePlayer): String {
        val faction = getPlayerFaction(player)
            ?: return (plugin.config.getString("factions.factionlessFactionName") ?: "Factionless")
        val factionService = plugin.services.factionService
        val relationshipService = plugin.services.factionRelationshipService
        return factionService.factions.count {
            relationshipService.getRelationships(faction.id, it.id).any { relationship -> relationship.type == VASSAL } &&
                relationshipService.getRelationships(it.id, faction.id).any { relationship -> relationship.type == LIEGE }
        }.toString()
    }

    private fun getFactionGateCount(player: OfflinePlayer): String {
        val faction = getPlayerFaction(player)
            ?: return (plugin.config.getString("factions.factionlessFactionName") ?: "Factionless")
        val gateService = plugin.services.gateService
        return gateService.getGatesByFaction(faction.id).size.toString()
    }

    private fun getFactionLiege(player: OfflinePlayer): String {
        val faction = getPlayerFaction(player)
            ?: return (plugin.config.getString("factions.factionlessFactionName") ?: "Factionless")
        val factionService = plugin.services.factionService
        val relationshipService = plugin.services.factionRelationshipService
        val liegeId = relationshipService.getRelationships(faction.id, LIEGE).singleOrNull()?.targetId ?: return "N/A"
        return factionService.getFaction(liegeId)?.name ?: "N/A"
    }

    private fun getFactionPopulation(player: OfflinePlayer): String {
        val faction = getPlayerFaction(player)
            ?: return (plugin.config.getString("factions.factionlessFactionName") ?: "Factionless")
        return faction.members.size.toString()
    }

    private fun getFactionRole(player: OfflinePlayer): String {
        val playerService = plugin.services.playerService
        val mfPlayer = playerService.getPlayer(player)
            ?: return (plugin.config.getString("factions.factionlessFactionName") ?: "Factionless")
        val faction = getPlayerFaction(player)
            ?: return (plugin.config.getString("factions.factionlessFactionName") ?: "Factionless")
        val role = faction.getRole(mfPlayer.id)
        return role?.name ?: "N/A"
    }

    private fun getPower(player: OfflinePlayer): String {
        val playerService = plugin.services.playerService
        val mfPlayer = playerService.getPlayer(player)
            ?: return decimalFormat.format(floor(plugin.config.getDouble("players.initialPower")))
        return decimalFormat.format(floor(mfPlayer.power))
    }

    private fun getMaxPower(): String {
        return decimalFormat.format(floor(plugin.config.getDouble("players.maxPower")))
    }

    private fun getFormattedPlayerPower(player: OfflinePlayer): String {
        return getPower(player) + "/" + getMaxPower()
    }

    private fun getFactionAtLocation(player: OfflinePlayer): String? {
        val onlinePlayer = player.player ?: return null
        val claimService = plugin.services.claimService
        val claim = claimService.getClaim(onlinePlayer.location.chunk)
        if (claim != null) {
            val factionService = plugin.services.factionService
            val faction = factionService.getFaction(claim.factionId)
            if (faction != null) return faction.name
        }
        return "Wilderness"
    }

    private fun getFactionColor(player: OfflinePlayer): String {
        val faction = getPlayerFaction(player) ?: return WHITE.toString()
        return ChatColor.of(faction.flags[plugin.flags.color]).toString()
    }

    private fun getPlayerChunkLocation(player: OfflinePlayer): String? {
        val onlinePlayer = player.player ?: return null
        val chunk = onlinePlayer.location.chunk
        return "${chunk.x}:${chunk.z}"
    }

    private fun getPlayerLocation(player: OfflinePlayer): String? {
        val onlinePlayer = player.player ?: return null
        val location = onlinePlayer.location
        return "${location.blockX}:${location.blockY}:${location.blockZ}"
    }

    private fun getPlayerWorld(player: OfflinePlayer): String? {
        val onlinePlayer = player.player ?: return null
        return onlinePlayer.location.world?.name ?: "World Undefined"
    }

    private fun isPlayerFactionEnemy(player: OfflinePlayer, faction: MfFaction): String {
        val playerFaction = getPlayerFaction(player)
            ?: return (plugin.config.getString("factions.factionlessFactionName") ?: "Factionless")
        val relationshipService = plugin.services.factionRelationshipService
        return (
            relationshipService.getRelationships(playerFaction.id, faction.id).any { it.type == AT_WAR } ||
                relationshipService.getRelationships(faction.id, playerFaction.id).any { it.type == AT_WAR }
            ).toString()
    }

    private fun isPlayerFactionAlly(player: OfflinePlayer, faction: MfFaction): String {
        val playerFaction = getPlayerFaction(player)
            ?: return (plugin.config.getString("factions.factionlessFactionName") ?: "Factionless")
        val relationshipService = plugin.services.factionRelationshipService
        return (
            relationshipService.getRelationships(playerFaction.id, faction.id).any { it.type == ALLY } &&
                relationshipService.getRelationships(faction.id, playerFaction.id).any { it.type == ALLY }
            ).toString()
    }

    private fun getFactionFlagValue(player: OfflinePlayer, flag: MfFlag<*>): String {
        val playerFaction = getPlayerFaction(player)
            ?: return (plugin.config.getString("factions.factionlessFactionName") ?: "Factionless")
        return playerFaction.flags[flag].toString()
    }

    private fun getPlayerFactionEnemies(one: Player, two: Player): String {
        val factionOne = getPlayerFaction(one)
            ?: return (plugin.config.getString("factions.factionlessFactionName") ?: "Factionless")
        val factionTwo = getPlayerFaction(two)
            ?: return (plugin.config.getString("factions.factionlessFactionName") ?: "Factionless")
        val relationshipService = plugin.services.factionRelationshipService
        return (
            relationshipService.getRelationships(factionOne.id, factionTwo.id).any { it.type == AT_WAR } ||
                relationshipService.getRelationships(factionTwo.id, factionOne.id).any { it.type == AT_WAR }
            ).toString()
    }

    private fun getPlayerFactionAllies(one: Player, two: Player): String {
        val factionOne = getPlayerFaction(one)
            ?: return (plugin.config.getString("factions.factionlessFactionName") ?: "Factionless")
        val factionTwo = getPlayerFaction(two)
            ?: return (plugin.config.getString("factions.factionlessFactionName") ?: "Factionless")
        val relationshipService = plugin.services.factionRelationshipService
        return (
            relationshipService.getRelationships(factionOne.id, factionTwo.id).any { it.type == ALLY } &&
                relationshipService.getRelationships(factionTwo.id, factionOne.id).any { it.type == ALLY }
            ).toString()
    }

    private fun getPlayerFactionVassal(one: Player, two: Player): String {
        val factionOne = getPlayerFaction(one)
            ?: return (plugin.config.getString("factions.factionlessFactionName") ?: "Factionless")
        val factionTwo = getPlayerFaction(two)
            ?: return (plugin.config.getString("factions.factionlessFactionName") ?: "Factionless")
        val relationshipService = plugin.services.factionRelationshipService
        return (
            relationshipService.getRelationships(factionOne.id, factionTwo.id).any { it.type == VASSAL } &&
                relationshipService.getRelationships(factionTwo.id, factionOne.id).any { it.type == LIEGE }
            ).toString()
    }

    private fun getPlayerFactionLiege(one: Player, two: Player): String {
        val factionOne = getPlayerFaction(one)
            ?: return (plugin.config.getString("factions.factionlessFactionName") ?: "Factionless")
        val factionTwo = getPlayerFaction(two)
            ?: return (plugin.config.getString("factions.factionlessFactionName") ?: "Factionless")
        val relationshipService = plugin.services.factionRelationshipService
        return (
            relationshipService.getRelationships(factionOne.id, factionTwo.id).any { it.type == LIEGE } &&
                relationshipService.getRelationships(factionTwo.id, factionOne.id).any { it.type == VASSAL }
            ).toString()
    }
}
