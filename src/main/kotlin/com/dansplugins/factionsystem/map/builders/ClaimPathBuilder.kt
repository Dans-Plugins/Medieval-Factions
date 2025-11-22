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

        // Use a map to track line segments at each point
        val lineSegments = mutableMapOf<Point, List<LineSegment>>()

        // Sort claims to ensure consistent path ordering (required for tests)
        claims.sortedWith { a, b ->
            val xComp = a.x.compareTo(b.x)
            if (xComp != 0) return@sortedWith xComp
            return@sortedWith a.z.compareTo(b.z)
        }.forEach { claim ->
            val x = claim.x
            val z = claim.z

            // Check north
            if ((x to z - 1) !in claimSet) {
                val lineSegment = (x to z) to (x + 1 to z)
                val lineSegmentsAtFirst = lineSegments[lineSegment.first] ?: emptyList()
                val lineSegmentsAtSecond = lineSegments[lineSegment.second] ?: emptyList()
                lineSegments[lineSegment.first] = lineSegmentsAtFirst + lineSegment
                lineSegments[lineSegment.second] = lineSegmentsAtSecond + lineSegment
            }

            // Check east
            if ((x + 1 to z) !in claimSet) {
                val lineSegment = (x + 1 to z) to (x + 1 to z + 1)
                val lineSegmentsAtFirst = lineSegments[lineSegment.first] ?: emptyList()
                val lineSegmentsAtSecond = lineSegments[lineSegment.second] ?: emptyList()
                lineSegments[lineSegment.first] = lineSegmentsAtFirst + lineSegment
                lineSegments[lineSegment.second] = lineSegmentsAtSecond + lineSegment
            }

            // Check south
            if ((x to z + 1) !in claimSet) {
                val lineSegment = (x to z + 1) to (x + 1 to z + 1)
                val lineSegmentsAtFirst = lineSegments[lineSegment.first] ?: emptyList()
                val lineSegmentsAtSecond = lineSegments[lineSegment.second] ?: emptyList()
                lineSegments[lineSegment.first] = lineSegmentsAtFirst + lineSegment
                lineSegments[lineSegment.second] = lineSegmentsAtSecond + lineSegment
            }

            // Check west
            if ((x - 1 to z) !in claimSet) {
                val lineSegment = (x to z) to (x to z + 1)
                val lineSegmentsAtFirst = lineSegments[lineSegment.first] ?: emptyList()
                val lineSegmentsAtSecond = lineSegments[lineSegment.second] ?: emptyList()
                lineSegments[lineSegment.first] = lineSegmentsAtFirst + lineSegment
                lineSegments[lineSegment.second] = lineSegmentsAtSecond + lineSegment
            }
        }

        if (lineSegments.isEmpty()) return emptyList()

        val paths = mutableListOf<List<Pair<Int, Int>>>()
        var (point, lineSegmentsAtPoint) = lineSegments.entries.first { (_, segs) -> segs.isNotEmpty() }
        var currentPath = mutableListOf<Pair<Int, Int>>()
        var startPoint = point

        while (!lineSegments.values.all { it.isEmpty() }) {
            currentPath.add(point)
            val lineSegmentToFollow = lineSegmentsAtPoint.first()
            val (first, second) = lineSegmentToFollow

            // Remove segment from both endpoints
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

            // Check if we've completed a closed loop (returned to start)
            val completedLoop = currentPath.isNotEmpty() && point == startPoint
            // Or if current point has no segments (reached dead end in disconnected regions)
            val reachedDeadEnd = !lineSegments.values.all { it.isEmpty() } && lineSegmentsAtPoint.isEmpty()

            if (completedLoop || reachedDeadEnd) {
                paths.add(currentPath)
                currentPath = mutableListOf()
                // Always find a new starting point for the next path
                if (!lineSegments.values.all { it.isEmpty() }) {
                    val (newPoint, newLineSegmentsAtPoint) = lineSegments.entries.first { (_, segs) -> segs.isNotEmpty() }
                    point = newPoint
                    lineSegmentsAtPoint = newLineSegmentsAtPoint
                    startPoint = point
                }
            }
        }
        if (currentPath.isNotEmpty()) {
            paths.add(currentPath)
        }
        return paths
    }
}
