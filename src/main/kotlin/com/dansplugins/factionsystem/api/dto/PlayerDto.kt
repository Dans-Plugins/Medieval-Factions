package com.dansplugins.factionsystem.api.dto

import com.dansplugins.factionsystem.player.MfPlayer

data class PlayerDto(
    val id: String,
    val name: String?,
    val power: Double,
    val factionId: String?
) {
    companion object {
        fun fromPlayer(player: MfPlayer, factionId: String?): PlayerDto {
            return PlayerDto(
                id = player.id.value.toString(),
                name = player.name,
                power = player.power,
                factionId = factionId
            )
        }
    }
}
