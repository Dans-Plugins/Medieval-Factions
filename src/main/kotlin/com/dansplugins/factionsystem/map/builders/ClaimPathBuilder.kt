package com.dansplugins.factionsystem.map.builders

import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.map.aliases.LineSegment
import com.dansplugins.factionsystem.map.aliases.Path
import com.dansplugins.factionsystem.map.aliases.Point

/**
 * Responsible for building paths from claimed chunks.
 */
class ClaimPathBuilder {

    /**
     * Generates a list of paths from the given list of claimed chunks.
     *
     * @param claims The list of claimed chunks.
     * @return A list of paths, where each path is a list of points representing the boundary of the claimed chunks.
     */
    fun getPaths(claims: List<MfClaimedChunk>): List<Path> {
        val lineSegments = mutableMapOf<Point, List<LineSegment>>()
        claims.sortedWith { a, b ->
            val xComp = a.x.compareTo(b.x)
            if (xComp != 0) return@sortedWith xComp
            return@sortedWith a.z.compareTo(b.z)
        }.forEach { claim ->
            val x = claim.x
            val z = claim.z
            val isNorthClaimed = claims.any { it.x == x && it.z == z - 1 }
            if (!isNorthClaimed) {
                val lineSegment = (x to z) to (x + 1 to z)
                val lineSegmentsAtFirstPoint = lineSegments[lineSegment.first] ?: emptyList()
                val lineSegmentsAtSecondPoint = lineSegments[lineSegment.second] ?: emptyList()
                lineSegments[lineSegment.first] = lineSegmentsAtFirstPoint + lineSegment
                lineSegments[lineSegment.second] = lineSegmentsAtSecondPoint + lineSegment
            }
            val isEastClaimed = claims.any { it.x == x + 1 && it.z == z }
            if (!isEastClaimed) {
                val lineSegment = (x + 1 to z) to (x + 1 to z + 1)
                val lineSegmentsAtFirstPoint = lineSegments[lineSegment.first] ?: emptyList()
                val lineSegmentsAtSecondPoint = lineSegments[lineSegment.second] ?: emptyList()
                lineSegments[lineSegment.first] = lineSegmentsAtFirstPoint + lineSegment
                lineSegments[lineSegment.second] = lineSegmentsAtSecondPoint + lineSegment
            }
            val isSouthClaimed = claims.any { it.x == x && it.z == z + 1 }
            if (!isSouthClaimed) {
                val lineSegment = (x to z + 1) to (x + 1 to z + 1)
                val lineSegmentsAtFirstPoint = lineSegments[lineSegment.first] ?: emptyList()
                val lineSegmentsAtSecondPoint = lineSegments[lineSegment.second] ?: emptyList()
                lineSegments[lineSegment.first] = lineSegmentsAtFirstPoint + lineSegment
                lineSegments[lineSegment.second] = lineSegmentsAtSecondPoint + lineSegment
            }
            val isWestClaimed = claims.any { it.x == x - 1 && it.z == z }
            if (!isWestClaimed) {
                val lineSegment = (x to z) to (x to z + 1)
                val lineSegmentsAtFirstPoint = lineSegments[lineSegment.first] ?: emptyList()
                val lineSegmentsAtSecondPoint = lineSegments[lineSegment.second] ?: emptyList()
                lineSegments[lineSegment.first] = lineSegmentsAtFirstPoint + lineSegment
                lineSegments[lineSegment.second] = lineSegmentsAtSecondPoint + lineSegment
            }
        }
        if (lineSegments.isEmpty()) return emptyList()
        val paths = mutableListOf<List<Pair<Int, Int>>>()
        var (point, lineSegmentsAtPoint) = lineSegments.entries.first { (_, lineSegmentsAtPoint) -> lineSegmentsAtPoint.isNotEmpty() }
        var currentPath = mutableListOf<Pair<Int, Int>>()
        while (!lineSegments.values.all { it.isEmpty() }) {
            currentPath.add(point)
            val lineSegmentToFollow = lineSegmentsAtPoint.first()
            val (first, second) = lineSegmentToFollow
            val lineSegmentsAtFirst = lineSegments[first]
            val lineSegmentsAtSecond = lineSegments[second]
            if (lineSegmentsAtFirst != null) {
                lineSegments[first] = lineSegmentsAtFirst - lineSegmentToFollow
            }
            if (lineSegmentsAtSecond != null) {
                lineSegments[second] = lineSegmentsAtSecond - lineSegmentToFollow
            }
            point = if (first == point) second else first
            lineSegmentsAtPoint = lineSegments[point] ?: emptyList()
            if (!lineSegments.values.all { it.isEmpty() } && lineSegmentsAtPoint.isEmpty()) {
                val (newPoint, newLineSegmentsAtPoint) = lineSegments.entries.first { (_, lineSegmentsAtPoint) -> lineSegmentsAtPoint.isNotEmpty() }
                point = newPoint
                lineSegmentsAtPoint = newLineSegmentsAtPoint
                paths.add(currentPath)
                currentPath = mutableListOf()
            }
        }
        paths.add(currentPath)
        return paths
    }
}
