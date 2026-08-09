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

    /**
     * The squared distance between [position] and the nearest point of this area, or null if [position] is in a
     * different world. Callers must treat null as "not comparable" rather than as a distance, in the same way that
     * [contains] reports false for a position in another world.
     *
     * The result is a [Long] because a squared distance exceeds `Int.MAX_VALUE` from roughly 46,341 blocks away on a
     * single axis, which is reachable on a large world.
     */
    fun distanceSquared(position: MfBlockPosition): Long? {
        if (position.worldId != minPosition.worldId) return null
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
        val deltaX = closestX.toLong() - x.toLong()
        val deltaY = closestY.toLong() - y.toLong()
        val deltaZ = closestZ.toLong() - z.toLong()
        return (deltaX * deltaX) + (deltaY * deltaY) + (deltaZ * deltaZ)
    }
}
