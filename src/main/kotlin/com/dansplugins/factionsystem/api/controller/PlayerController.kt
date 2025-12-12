package com.dansplugins.factionsystem.api.controller

import com.dansplugins.factionsystem.api.dto.ErrorResponse
import com.dansplugins.factionsystem.api.dto.PlayerDto
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.player.MfPlayerService
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import java.util.UUID

class PlayerController(
    private val playerService: MfPlayerService,
    private val factionService: MfFactionService
) {

    fun getAll(ctx: Context) {
        // Build a map of player IDs to faction IDs to avoid N+1 lookups
        val allPlayers = playerService.players
        val playerIdToFactionId = mutableMapOf<MfPlayerId, String>()
        
        // Get all factions and build the mapping
        factionService.factions.forEach { faction ->
            faction.members.forEach { member ->
                playerIdToFactionId[member.playerId] = faction.id.value.toString()
            }
        }
        
        val players = allPlayers.map { player ->
            PlayerDto.fromPlayer(player, playerIdToFactionId[player.id])
        }
        ctx.json(players).status(HttpStatus.OK)
    }

    fun getById(ctx: Context) {
        val id = ctx.pathParam("id")
        try {
            val playerId = MfPlayerId(UUID.fromString(id))
            val player = playerService.getPlayer(playerId)
            
            if (player != null) {
                val faction = factionService.getFaction(player.id)
                ctx.json(PlayerDto.fromPlayer(player, faction?.id?.value?.toString())).status(HttpStatus.OK)
            } else {
                ctx.json(ErrorResponse("NOT_FOUND", "Player not found")).status(HttpStatus.NOT_FOUND)
            }
        } catch (e: IllegalArgumentException) {
            ctx.json(ErrorResponse("INVALID_ID", "Invalid player ID format")).status(HttpStatus.BAD_REQUEST)
        }
    }
}
