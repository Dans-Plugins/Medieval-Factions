package com.dansplugins.factionsystem.api.dto

import com.dansplugins.factionsystem.faction.MfFaction

data class FactionDto(
    val id: String,
    val name: String,
    val description: String,
    val prefix: String?,
    val power: Double,
    val maxPower: Double,
    val memberCount: Int,
    val home: PositionDto?
) {
    companion object {
        fun fromFaction(faction: MfFaction): FactionDto {
            return FactionDto(
                id = faction.id.value.toString(),
                name = faction.name,
                description = faction.description,
                prefix = faction.prefix,
                power = faction.power,
                maxPower = faction.maxPower,
                memberCount = faction.members.size,
                home = faction.home?.let { PositionDto.fromPosition(it) }
            )
        }
    }
}
