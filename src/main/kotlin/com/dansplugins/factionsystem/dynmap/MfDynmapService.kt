package com.dansplugins.factionsystem.dynmap

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import org.dynmap.DynmapAPI
import org.dynmap.markers.AreaMarker
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.floor
import kotlin.math.roundToInt

private typealias Point = Pair<Int, Int>
private typealias LineSegment = Pair<Point, Point>
private typealias Path = List<Point>

class MfDynmapService(private val plugin: MedievalFactions) {

    private val dynmap = plugin.server.pluginManager.getPlugin("dynmap") as DynmapAPI
    private val factionMarkersByFactionId = ConcurrentHashMap<MfFactionId, List<AreaMarker>>()
    private val decimalFormat = DecimalFormat("0", DecimalFormatSymbols.getInstance(plugin.language.locale))

    fun updateClaims(faction: MfFaction) {
        val markerApi = dynmap.markerAPI
        if (markerApi == null) {
            plugin.logger.warning("Failed to find Dynmap Marker API, skipping update of ${faction.name} claims")
            return
        }
        val claimsMarkerSet = markerApi.getMarkerSet("claims")
            ?: markerApi.createMarkerSet("claims", "Claims", null, false)
        val realmMarkerSet = markerApi.getMarkerSet("realms")
            ?: markerApi.createMarkerSet("realms", "Realms", null, false)
        factionMarkersByFactionId[faction.id]?.forEach { marker -> marker.deleteMarker() }
        val claimService = plugin.services.claimService
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val claims = claimService.getClaims(faction.id)
                val factionInfo = buildFactionInfo(faction)
                claims.groupBy { it.worldId }.forEach { (worldId, worldClaims) ->
                    plugin.server.scheduler.runTask(
                        plugin,
                        Runnable {
                            val world = plugin.server.getWorld(worldId)
                            if (world != null) {
                                plugin.server.scheduler.runTaskAsynchronously(
                                    plugin,
                                    Runnable {
                                        val paths = getPaths(worldClaims)
                                        paths.forEachIndexed { index, path ->
                                            val corners = getCorners(path)
                                            plugin.server.scheduler.runTask(
                                                plugin,
                                                Runnable {
                                                    val areaMarker = claimsMarkerSet.createAreaMarker(
                                                        "claim_border_${faction.id.value}_${worldId}_$index",
                                                        faction.name,
                                                        false,
                                                        world.name,
                                                        corners.map { (x, _) -> x * 16.0 }.toDoubleArray(),
                                                        corners.map { (_, z) -> z * 16.0 }.toDoubleArray(),
                                                        false
                                                    )
                                                    val color = Integer.decode(faction.flags[plugin.flags.color])
                                                    areaMarker.setFillStyle(0.0, color)
                                                    areaMarker.setLineStyle(1, 1.0, color)
                                                    areaMarker.description = factionInfo
                                                    val factionMarkers = factionMarkersByFactionId[faction.id] ?: listOf()
                                                    factionMarkersByFactionId[faction.id] = factionMarkers + areaMarker
                                                }
                                            )
                                        }
                                        worldClaims.forEachIndexed { index, claim ->
                                            plugin.server.scheduler.runTask(
                                                plugin,
                                                Runnable {
                                                    val areaMarker = claimsMarkerSet.createAreaMarker(
                                                        "claim_${faction.id.value}_${worldId}_$index",
                                                        faction.name,
                                                        false,
                                                        world.name,
                                                        doubleArrayOf(claim.x * 16.0, (claim.x + 1) * 16.0, (claim.x + 1) * 16.0, claim.x * 16.0),
                                                        doubleArrayOf(claim.z * 16.0, claim.z * 16.0, (claim.z + 1) * 16.0, (claim.z + 1) * 16.0),
                                                        false
                                                    )
                                                    val color = Integer.decode(faction.flags[plugin.flags.color])
                                                    areaMarker.setFillStyle(0.3, color)
                                                    areaMarker.setLineStyle(0, 0.0, color)
                                                    areaMarker.description = factionInfo
                                                    val factionMarkers = factionMarkersByFactionId[faction.id] ?: listOf()
                                                    factionMarkersByFactionId[faction.id] = factionMarkers + areaMarker
                                                }
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    )
                }
                val relationshipService = plugin.services.factionRelationshipService
                val realm = claims + relationshipService.getVassalTree(faction.id).flatMap(claimService::getClaims)
                realm.groupBy { it.worldId }.forEach { (worldId, worldClaims) ->
                    plugin.server.scheduler.runTask(
                        plugin,
                        Runnable {
                            val world = plugin.server.getWorld(worldId)
                            if (world != null) {
                                plugin.server.scheduler.runTaskAsynchronously(
                                    plugin,
                                    Runnable {
                                        val paths = getPaths(worldClaims)
                                        paths.forEachIndexed { index, path ->
                                            val corners = getCorners(path)
                                            plugin.server.scheduler.runTask(
                                                plugin,
                                                Runnable {
                                                    val areaMarker = realmMarkerSet.createAreaMarker(
                                                        "realm_${faction.id.value}_${worldId}_$index",
                                                        faction.name,
                                                        false,
                                                        world.name,
                                                        corners.map { (x, _) -> x * 16.0 }.toDoubleArray(),
                                                        corners.map { (_, z) -> z * 16.0 }.toDoubleArray(),
                                                        false
                                                    )
                                                    areaMarker.label = faction.name
                                                    val color = Integer.decode(faction.flags[plugin.flags.color])
                                                    areaMarker.setFillStyle(0.0, color)
                                                    areaMarker.setLineStyle(4, 1.0, color)
                                                    areaMarker.description = factionInfo
                                                    val factionMarkers = factionMarkersByFactionId[faction.id] ?: listOf()
                                                    factionMarkersByFactionId[faction.id] = factionMarkers + areaMarker
                                                }
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    )
                }
            }
        )
    }

    private fun getCorners(points: List<Point>): List<Point> {
        val corners = mutableListOf<Pair<Int, Int>>()
        for (i in points.indices) {
            val (prevX, prevZ) = if (i > 0) points[i - 1] else points.last()
            val (nextX, nextZ) = if (i < points.lastIndex) points[i + 1] else points.first()
            if (prevX != nextX && prevZ != nextZ) {
                corners.add(points[i])
            }
        }
        return corners
    }

    private fun getPaths(claims: List<MfClaimedChunk>): List<Path> {
        // lineSegments maps points to line segments that have an ending at that position
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

    private fun buildFactionInfo(faction: MfFaction): String {
        val factionService = plugin.services.factionService
        val relationshipService = plugin.services.factionRelationshipService
        val claimService = plugin.services.claimService
        val playerService = plugin.services.playerService
        return buildString {
            append("<h1>${faction.name}</h1>")
            append("<h2>Description</h2>")
            append(faction.description)
            append("<h2>Members (${faction.members.size})</h2>")
            append(
                faction.members.groupBy { it.role }.map { (role, members) ->
                    """
                        <h3>${role.name} (${faction.members.count { it.role.id == role.id }})</h3>
                        ${members.joinToString { member -> playerService.getPlayer(member.playerId)?.name ?: plugin.language["UnknownPlayer"] }}
                    """.trimIndent()
                }.joinToString("<br />")
            )
            val liegeId = relationshipService.getLiege(faction.id)
            val liege = liegeId?.let(factionService::getFaction)
            if (liege != null) {
                append("<h2>Liege</h2>")
                append(liege.name)
                append("<br />")
            }
            val vassals = relationshipService.getVassals(faction.id).mapNotNull(factionService::getFaction)
            if (vassals.isNotEmpty()) {
                append("<h2>Vassals</h2>")
                append(vassals.joinToString(transform = MfFaction::name))
                append("<br />")
            }
            val allies = relationshipService.getAllies(faction.id).mapNotNull(factionService::getFaction)
            if (allies.isNotEmpty()) {
                append("<h2>Allies</h2>")
                append(allies.joinToString(transform = MfFaction::name))
                append("<br />")
            }
            val atWarWith = relationshipService.getFactionsAtWarWith(faction.id).mapNotNull(factionService::getFaction)
            if (atWarWith.isNotEmpty()) {
                append("<h2>At war with</h2>")
                append(atWarWith.joinToString(transform = MfFaction::name))
                append("<br />")
            }
            append("<h2>Power</h2>")
            append(decimalFormat.format(floor(faction.power)))
            append("<br />")
            append("<h2>Demesne</h2>")
            val claims = claimService.getClaims(faction.id)
            append("${claims.size}/${floor(faction.power).roundToInt()}")
        }
    }
}
