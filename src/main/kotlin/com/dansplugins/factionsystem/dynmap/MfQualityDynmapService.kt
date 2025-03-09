package com.dansplugins.factionsystem.dynmap

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.claim.MfClaimService
import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.dynmap.builders.ClaimPathBuilder
import com.dansplugins.factionsystem.dynmap.builders.FactionInfoBuilder
import com.dansplugins.factionsystem.dynmap.helpers.MarkerSetHelper
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import org.bukkit.World
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import org.dynmap.DynmapAPI
import org.dynmap.markers.AreaMarker
import org.dynmap.markers.MarkerSet
import java.util.*
import java.util.concurrent.ConcurrentHashMap

typealias Point = Pair<Int, Int>
typealias LineSegment = Pair<Point, Point>
typealias Path = List<Point>

/**
 * Service for managing Dynmap markers with high-quality, detailed geometry.
 *
 * @property plugin The main plugin instance.
 */
class MfQualityDynmapService(private val plugin: MedievalFactions) : MfDynmapService {

    private val dynmap = plugin.server.pluginManager.getPlugin("dynmap") as DynmapAPI
    private val factionMarkersByFactionId = ConcurrentHashMap<MfFactionId, List<AreaMarker>>()
    private val updateTasks: MutableMap<MfFactionId, MutableList<BukkitTask>> =
        Collections.synchronizedMap(mutableMapOf<MfFactionId, MutableList<BukkitTask>>())

    private val factionInfoBuilder = FactionInfoBuilder(plugin)
    private val claimPathBuilder = ClaimPathBuilder()
    private val markerSetHelper = MarkerSetHelper()

    private val debug = false

    private var scheduleUpdateClaimsInvocationCount = 0
    private var createUpdateTaskInvocationCount = 0
    private var updateClaimsInvocationCount = 0

    override fun scheduleUpdateClaims(faction: MfFaction) {
        scheduleUpdateClaimsInvocationCount++
        if (plugin.config.getBoolean("dynmap.debug")) {
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
        if (plugin.config.getBoolean("dynmap.debug")) {
            plugin.logger.info("Creating update task for faction $factionId. Invocation count: $createUpdateTaskInvocationCount")
        }
        val updateTask = object : BukkitRunnable() {
            override fun run() {
                if (plugin.config.getBoolean("dynmap.debug")) {
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

    private fun updateClaims(faction: MfFaction) {
        updateClaimsInvocationCount++
        if (plugin.config.getBoolean("dynmap.debug")) {
            plugin.logger.info("Updating ${faction.name} claims. Invocation count: $updateClaimsInvocationCount")
        }
        val markerApi = dynmap.markerAPI
        if (markerApi == null) {
            plugin.logger.warning("Failed to find Dynmap Marker API, skipping update of ${faction.name} claims")
            return
        }
        val claimsMarkerSet = markerSetHelper.getOrCreateMarkerSet(markerApi, "claims", "Claims")
        val realmMarkerSet = markerSetHelper.getOrCreateMarkerSet(markerApi, "realms", "Realms")
        factionMarkersByFactionId[faction.id]?.forEach { marker -> marker.deleteMarker() }
        val claimService = plugin.services.claimService
        createUpdateTask(faction.id, { updateFactionClaims(faction, claimsMarkerSet, claimService) }) { runTaskAsynchronously(plugin) }
        createUpdateTask(faction.id, { updateFactionRealm(faction, realmMarkerSet, claimService) }) { runTaskAsynchronously(plugin) }
    }

    private fun updateFactionClaims(faction: MfFaction, claimsMarkerSet: MarkerSet, claimService: MfClaimService) {
        val claims = claimService.getClaims(faction.id)
        if (plugin.config.getBoolean("dynmap.debug")) {
            plugin.logger.info("Fetched ${claims.size} claims for faction ${faction.name}")
        }
        val factionInfo = factionInfoBuilder.build(faction)
        claims.groupBy { it.worldId }.forEach { (worldId, worldClaims) ->
            createUpdateTask(faction.id, { updateWorldClaims(faction, worldId, worldClaims, claimsMarkerSet, factionInfo) }) { runTask(plugin) }
        }
    }

    private fun updateWorldClaims(faction: MfFaction, worldId: UUID, worldClaims: List<MfClaimedChunk>, claimsMarkerSet: MarkerSet, factionInfo: String) {
        val world = plugin.server.getWorld(worldId)
        if (world != null) {
            createUpdateTask(faction.id, { createAreaMarkers(faction, world, worldClaims, claimsMarkerSet, factionInfo) }) { runTaskAsynchronously(plugin) }
        }
    }

    private fun createAreaMarkers(faction: MfFaction, world: World, worldClaims: List<MfClaimedChunk>, claimsMarkerSet: MarkerSet, factionInfo: String) {
        val paths = claimPathBuilder.getPaths(worldClaims)
        if (plugin.config.getBoolean("dynmap.debug")) {
            plugin.logger.info("Generated ${paths.size} paths for world ${world.name}")
        }
        paths.forEachIndexed { index, path ->
            val corners = getCorners(path)
            createUpdateTask(faction.id, { createAreaMarker(faction, world, corners, claimsMarkerSet, factionInfo, index) }) { runTask(plugin) }
        }
        worldClaims.forEachIndexed { index, claim ->
            createUpdateTask(faction.id, { createClaimMarker(faction, world, claim, claimsMarkerSet, factionInfo, index) }) { runTask(plugin) }
        }
    }

    private fun createAreaMarker(faction: MfFaction, world: World, corners: List<Point>, claimsMarkerSet: MarkerSet, factionInfo: String, index: Int) {
        val areaMarker = claimsMarkerSet.createAreaMarker(
            "claim_border_${faction.id.value}_${world.name}_$index",
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
            val factionMarkers = factionMarkersByFactionId[faction.id] ?: listOf()
            factionMarkersByFactionId[faction.id] = factionMarkers + areaMarker
            if (plugin.config.getBoolean("dynmap.debug")) {
                plugin.logger.info("Created area marker for path $index in world ${world.name}")
            }
        }
    }

    private fun createClaimMarker(faction: MfFaction, world: World, claim: MfClaimedChunk, claimsMarkerSet: MarkerSet, factionInfo: String, index: Int) {
        val areaMarker = claimsMarkerSet.createAreaMarker(
            "claim_${faction.id.value}_${world.name}_$index",
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
        if (plugin.config.getBoolean("dynmap.debug")) {
            plugin.logger.info("Created area marker for claim $index in world ${world.name}")
        }
    }

    private fun updateFactionRealm(faction: MfFaction, realmMarkerSet: MarkerSet, claimService: MfClaimService) {
        val relationshipService = plugin.services.factionRelationshipService
        val realm = claimService.getClaims(faction.id) + relationshipService.getVassalTree(faction.id).flatMap(claimService::getClaims)
        realm.groupBy { it.worldId }.forEach { (worldId, worldClaims) ->
            createUpdateTask(faction.id, { updateWorldRealm(faction, worldId, worldClaims, realmMarkerSet) }) { runTask(plugin) }
        }
    }

    private fun updateWorldRealm(faction: MfFaction, worldId: UUID, worldClaims: List<MfClaimedChunk>, realmMarkerSet: MarkerSet) {
        val world = plugin.server.getWorld(worldId)
        if (world != null) {
            createUpdateTask(faction.id, { createRealmMarkers(faction, world, worldClaims, realmMarkerSet) }) { runTaskAsynchronously(plugin) }
        }
    }

    private fun createRealmMarkers(faction: MfFaction, world: World, worldClaims: List<MfClaimedChunk>, realmMarkerSet: MarkerSet) {
        val paths = claimPathBuilder.getPaths(worldClaims)
        if (plugin.config.getBoolean("dynmap.debug")) {
            plugin.logger.info("Generated ${paths.size} paths for realm in world ${world.name}")
        }
        paths.forEachIndexed { index, path ->
            val corners = getCorners(path)
            createUpdateTask(faction.id, { createRealmAreaMarker(faction, world, corners, realmMarkerSet, index) }) { runTask(plugin) }
        }
    }

    private fun createRealmAreaMarker(faction: MfFaction, world: World, corners: List<Point>, realmMarkerSet: MarkerSet, index: Int) {
        val areaMarker = realmMarkerSet.createAreaMarker(
            "realm_${faction.id.value}_${world.name}_$index",
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
        areaMarker.description = FactionInfoBuilder(plugin).build(faction)
        val factionMarkers = factionMarkersByFactionId[faction.id] ?: listOf()
        factionMarkersByFactionId[faction.id] = factionMarkers + areaMarker
        if (plugin.config.getBoolean("dynmap.debug")) {
            plugin.logger.info("Created realm area marker for path $index in world ${world.name}")
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
        if (plugin.config.getBoolean("dynmap.debug")) {
            plugin.logger.info("Calculated ${corners.size} corners from points")
        }
        return corners
    }
}
