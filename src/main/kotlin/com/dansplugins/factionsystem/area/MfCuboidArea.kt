package com.dansplugins.factionsystem.area

import kotlin.math.max
import kotlin.math.min

data class MfCuboidArea(
    val position1: MfBlockPosition,
    val position2: MfBlockPosition
) {
    init {
        if (position1.worldId != position2.worldId) {
            throw IllegalStateException("Position worlds do not match")
        }
    }

    val minPosition: MfBlockPosition
        get() = MfBlockPosition(
            position1.worldId,
            min(position1.x, position2.x),
            min(position1.y, position2.y),
            min(position1.z, position2.z)
        )

    val maxPosition: MfBlockPosition
        get() = MfBlockPosition(
            position1.worldId,
            max(position1.x, position2.x),
            max(position1.y, position2.y),
            max(position1.z, position2.z)
        )

    val height: Int
        get() = maxPosition.y - minPosition.y

    val width: Int
        get() = maxPosition.x - minPosition.x

    val depth: Int
        get() = maxPosition.z - minPosition.z

    val centerPosition: MfBlockPosition
        get() = MfBlockPosition(
            position1.worldId,
            (position1.x + position2.x) / 2,
            (position1.y + position2.y) / 2,
            (position1.z + position2.z) / 2
        )

    val blocks: List<MfBlockPosition>
        get() = (minPosition.x..maxPosition.x).flatMap { x ->
            (minPosition.y..maxPosition.y).flatMap { y ->
                (minPosition.z..maxPosition.z).map { z ->
                    MfBlockPosition(position1.worldId, x, y, z)
                }
            }
        }

    fun contains(position: MfBlockPosition) =
        position.worldId == minPosition.worldId &&
            position.x >= minPosition.x &&
            position.y >= minPosition.y &&
            position.z >= minPosition.z &&
            position.x <= maxPosition.x &&
            position.y <= maxPosition.y &&
            position.z <= maxPosition.z

    fun distanceSquared(position: MfBlockPosition): Int {
        val x = position.x
        val y = position.y
        val z = position.z
        val closestX = when {
            x < minPosition.x -> minPosition.x
            minPosition.x <= x && x <= maxPosition.x -> x
            else -> maxPosition.x
        }
        val closestY = when {
            y < minPosition.y -> minPosition.y
            minPosition.y <= y && y <= maxPosition.y -> y
            else -> maxPosition.y
        }
        val closestZ = when {
            z < minPosition.z -> minPosition.z
            minPosition.z <= z && z <= maxPosition.z -> z
            else -> maxPosition.z
        }
        return ((closestX - x) * (closestX - x)) + ((closestY - y) * (closestY - y)) + ((closestZ - z) * (closestZ - z))
    }
}
