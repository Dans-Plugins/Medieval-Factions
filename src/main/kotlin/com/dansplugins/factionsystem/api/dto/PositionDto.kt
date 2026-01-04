package com.dansplugins.factionsystem.api.dto

import com.dansplugins.factionsystem.area.MfPosition

data class PositionDto(
    val world: String,
    val x: Double,
    val y: Double,
    val z: Double
) {
    companion object {
        fun fromPosition(position: MfPosition): PositionDto {
            return PositionDto(
                world = position.world,
                x = position.x,
                y = position.y,
                z = position.z
            )
        }
    }
}
