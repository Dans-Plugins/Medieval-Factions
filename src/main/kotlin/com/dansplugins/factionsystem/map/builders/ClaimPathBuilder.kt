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
        if (claims.isEmpty()) return emptyList()

        // Build a HashSet for O(1) lookups instead of O(n) list searches
        val claimSet = claims.mapTo(HashSet()) { it.x to it.z }

        // Use mutable lists instead of repeated list concatenation
        val lineSegments = mutableMapOf<Point, MutableList<LineSegment>>()

        claims.forEach { claim ->
            val x = claim.x
            val z = claim.z

            // Check north
            if ((x to z - 1) !in claimSet) {
                val lineSegment = (x to z) to (x + 1 to z)
                lineSegments.getOrPut(lineSegment.first) { mutableListOf() }.add(lineSegment)
                lineSegments.getOrPut(lineSegment.second) { mutableListOf() }.add(lineSegment)
            }

            // Check east
            if ((x + 1 to z) !in claimSet) {
                val lineSegment = (x + 1 to z) to (x + 1 to z + 1)
                lineSegments.getOrPut(lineSegment.first) { mutableListOf() }.add(lineSegment)
                lineSegments.getOrPut(lineSegment.second) { mutableListOf() }.add(lineSegment)
            }

            // Check south
            if ((x to z + 1) !in claimSet) {
                val lineSegment = (x to z + 1) to (x + 1 to z + 1)
                lineSegments.getOrPut(lineSegment.first) { mutableListOf() }.add(lineSegment)
                lineSegments.getOrPut(lineSegment.second) { mutableListOf() }.add(lineSegment)
            }

            // Check west
            if ((x - 1 to z) !in claimSet) {
                val lineSegment = (x to z) to (x to z + 1)
                lineSegments.getOrPut(lineSegment.first) { mutableListOf() }.add(lineSegment)
                lineSegments.getOrPut(lineSegment.second) { mutableListOf() }.add(lineSegment)
            }
        }

        if (lineSegments.isEmpty()) return emptyList()

        val paths = mutableListOf<List<Pair<Int, Int>>>()
        var (point, lineSegmentsAtPoint) = lineSegments.entries.first { (_, segs) -> segs.isNotEmpty() }
        var currentPath = mutableListOf<Pair<Int, Int>>()

        while (!lineSegments.values.all { it.isEmpty() }) {
            currentPath.add(point)
            val lineSegmentToFollow = lineSegmentsAtPoint.first()
            val (first, second) = lineSegmentToFollow

            // Remove from both endpoints
            lineSegments[first]?.remove(lineSegmentToFollow)
            lineSegments[second]?.remove(lineSegmentToFollow)

            point = if (first == point) second else first
            lineSegmentsAtPoint = lineSegments[point] ?: mutableListOf()

            if (!lineSegments.values.all { it.isEmpty() } && lineSegmentsAtPoint.isEmpty()) {
                val (newPoint, newLineSegmentsAtPoint) = lineSegments.entries.first { (_, segs) -> segs.isNotEmpty() }
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
