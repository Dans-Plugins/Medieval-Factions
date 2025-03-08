package com.dansplugins.factionsystem.dynmap

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.dynmap.builders.ClaimPathBuilder
import com.dansplugins.factionsystem.dynmap.builders.FactionInfoBuilder
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import org.dynmap.DynmapAPI
import org.dynmap.markers.AreaMarker
import java.util.*
import java.util.concurrent.ConcurrentHashMap

typealias Point = Pair<Int, Int>
typealias LineSegment = Pair<Point, Point>
typealias Path = List<Point>

class MfDynmapService(private val plugin: MedievalFactions) {

    private val dynmap = plugin.server.pluginManager.getPlugin("dynmap") as DynmapAPI
    private val factionMarkersByFactionId = ConcurrentHashMap<MfFactionId, List<AreaMarker>>()
    private val updateTasks: MutableMap<MfFactionId, MutableList<BukkitTask>> =
        Collections.synchronizedMap(mutableMapOf<MfFactionId, MutableList<BukkitTask>>())

    private val debug = false

    private var scheduleUpdateClaimsInvocationCount = 0
    private var createUpdateTaskInvocationCount = 0
    private var updateClaimsInvocationCount = 0

    fun scheduleUpdateClaims(faction: MfFaction) {
        scheduleUpdateClaimsInvocationCount++
        if (debug) {
            plugin.logger.info("Scheduling update of ${faction.name} claims. Invocation count: $scheduleUpdateClaimsInvocationCount")
        }
        val factionUpdateTasks = updateTasks[faction.id]
        if (!factionUpdateTasks.isNullOrEmpty()) {
            factionUpdateTasks.forEach(BukkitTask::cancel)
            factionUpdateTasks.clear()
        }
        createUpdateTask(faction.id, { updateClaims(faction) }) { runTaskLater(plugin, 100L) }
    }

    private fun createUpdateTask(
        factionId: MfFactionId,
        runnable: Runnable,
        schedule: BukkitRunnable.() -> BukkitTask
    ) {
        createUpdateTaskInvocationCount++
        if (debug) {
            plugin.logger.info("Creating update task for faction $factionId. Invocation count: $createUpdateTaskInvocationCount")
        }
        val updateTask = object : BukkitRunnable() {
            override fun run() {
                if (debug) {
                    plugin.logger.info("Running update task for faction $factionId")
                }
                runnable.run()
                val factionUpdateTasks = updateTasks[factionId]
                factionUpdateTasks?.removeAll(plugin.server.scheduler.pendingTasks.filter { it.taskId == taskId })
            }
        }.schedule()
        val factionUpdateTasks = updateTasks[factionId]
        if (factionUpdateTasks == null) {
            updateTasks[factionId] = Collections.synchronizedList(mutableListOf(updateTask))
        } else {
            factionUpdateTasks.add(updateTask)
        }
    }

    fun updateClaims(faction: MfFaction) {
        updateClaimsInvocationCount++
        if (debug) {
            plugin.logger.info("Updating ${faction.name} claims. Invocation count: $updateClaimsInvocationCount")
        }
        val startTime = System.currentTimeMillis()
        val markerApi = dynmap.markerAPI
        if (markerApi == null) {
            plugin.logger.warning("Failed to find Dynmap Marker API, skipping update of ${faction.name} claims")
            return
        }
        val claimsMarkerSet =
            markerApi.getMarkerSet("claims") ?: markerApi.createMarkerSet("claims", "Claims", null, false)
        val realmMarkerSet =
            markerApi.getMarkerSet("realms") ?: markerApi.createMarkerSet("realms", "Realms", null, false)
        factionMarkersByFactionId[faction.id]?.forEach { marker -> marker.deleteMarker() }
        val claimService = plugin.services.claimService
        createUpdateTask(faction.id, {
            val claims = claimService.getClaims(faction.id)
            if (debug) {
                plugin.logger.info("Fetched ${claims.size} claims for faction ${faction.name}")
            }
            val factionInfo = FactionInfoBuilder(plugin).build(faction)
            claims.groupBy { it.worldId }.forEach { (worldId, worldClaims) ->
                createUpdateTask(
                    faction.id,
                    {
                        val world = plugin.server.getWorld(worldId)
                        if (world != null) {
                            createUpdateTask(
                                faction.id,
                                {
                                    val paths = ClaimPathBuilder.getPaths(worldClaims)
                                    if (debug) {
                                        plugin.logger.info("Generated ${paths.size} paths for world $worldId")
                                    }
                                    paths.forEachIndexed { index, path ->
                                        val corners = getCorners(path)
                                        createUpdateTask(
                                            faction.id,
                                            {
                                                val areaMarker = claimsMarkerSet.createAreaMarker(
                                                    "claim_border_${faction.id.value}_${worldId}_$index",
                                                    faction.name,
                                                    false,
                                                    world.name,
                                                    corners.map { (x, _) -> x * 16.0 }.toDoubleArray(),
                                                    corners.map { (_, z) -> z * 16.0 }.toDoubleArray(),
                                                    false
                                                )
                                                if (areaMarker != null) {
                                                    val color = Integer.decode(faction.flags[plugin.flags.color])
                                                    areaMarker.setFillStyle(0.0, color)
                                                    areaMarker.setLineStyle(1, 1.0, color)
                                                    areaMarker.description = factionInfo
                                                    val factionMarkers =
                                                        factionMarkersByFactionId[faction.id] ?: listOf()
                                                    factionMarkersByFactionId[faction.id] = factionMarkers + areaMarker
                                                    if (debug) {
                                                        plugin.logger.info("Created area marker for path $index in world $worldId")
                                                    }
                                                }
                                            }
                                        ) { runTask(plugin) }
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
                                                    doubleArrayOf(
                                                        claim.x * 16.0,
                                                        (claim.x + 1) * 16.0,
                                                        (claim.x + 1) * 16.0,
                                                        claim.x * 16.0
                                                    ),
                                                    doubleArrayOf(
                                                        claim.z * 16.0,
                                                        claim.z * 16.0,
                                                        (claim.z + 1) * 16.0,
                                                        (claim.z + 1) * 16.0
                                                    ),
                                                    false
                                                )
                                                val color = Integer.decode(faction.flags[plugin.flags.color])
                                                areaMarker.setFillStyle(0.3, color)
                                                areaMarker.setLineStyle(0, 0.0, color)
                                                areaMarker.description = factionInfo
                                                val factionMarkers = factionMarkersByFactionId[faction.id] ?: listOf()
                                                factionMarkersByFactionId[faction.id] = factionMarkers + areaMarker
                                                if (debug) {
                                                    plugin.logger.info("Created area marker for claim $index in world $worldId")
                                                }
                                            }
                                        )
                                    }
                                }
                            ) { runTaskAsynchronously(plugin) }
                        }
                    }
                ) { runTask(plugin) }
            }
            val relationshipService = plugin.services.factionRelationshipService
            val realm = claims + relationshipService.getVassalTree(faction.id).flatMap(claimService::getClaims)
            realm.groupBy { it.worldId }.forEach { (worldId, worldClaims) ->
                createUpdateTask(
                    faction.id,
                    {
                        val world = plugin.server.getWorld(worldId)
                        if (world != null) {
                            createUpdateTask(
                                faction.id,
                                {
                                    val paths = ClaimPathBuilder.getPaths(worldClaims)
                                    if (debug) {
                                        plugin.logger.info("Generated ${paths.size} paths for realm in world $worldId")
                                    }
                                    paths.forEachIndexed { index, path ->
                                        val corners = getCorners(path)
                                        createUpdateTask(
                                            faction.id,
                                            {
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
                                                if (debug) {
                                                    plugin.logger.info("Created realm area marker for path $index in world $worldId")
                                                }
                                            }
                                        ) { runTask(plugin) }
                                    }
                                }
                            ) { runTaskAsynchronously(plugin) }
                        }
                    }
                ) { runTask(plugin) }
            }
        }) { runTaskAsynchronously(plugin) }
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        if (debug) {
            plugin.logger.info("updateClaims for ${faction.name} took $duration ms")
        }
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
        if (debug) {
            plugin.logger.info("Calculated ${corners.size} corners from points")
        }
        return corners
    }
}
