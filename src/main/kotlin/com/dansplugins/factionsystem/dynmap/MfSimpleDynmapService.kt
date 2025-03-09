package com.dansplugins.factionsystem.dynmap

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.dynmap.builders.ClaimPathBuilder
import com.dansplugins.factionsystem.dynmap.builders.FactionInfoBuilder
import com.dansplugins.factionsystem.dynmap.helpers.MarkerSetHelper
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import org.dynmap.DynmapAPI
import org.dynmap.markers.AreaMarker
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Service for managing Dynmap markers with simple, basic geometry.
 *
 * @property plugin The main plugin instance.
 */
class MfSimpleDynmapService(private val plugin: MedievalFactions) : MfDynmapService {

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
        val markerApi = dynmap.markerAPI ?: return
        val claimsMarkerSet = markerSetHelper.getOrCreateMarkerSet(markerApi, "claims", "Claims")
        factionMarkersByFactionId[faction.id]?.forEach { it.deleteMarker() }
        val claimService = plugin.services.claimService
        val claims = claimService.getClaims(faction.id)
        if (plugin.config.getBoolean("dynmap.debug")) {
            plugin.logger.info("Fetched ${claims.size} claims for faction ${faction.name}")
        }
        val factionInfo = factionInfoBuilder.build(faction)
        claims.groupBy { it.worldId }.forEach { (worldId, worldClaims) ->
            val world = plugin.server.getWorld(worldId) ?: return@forEach
            val paths = claimPathBuilder.getPaths(worldClaims)
            if (plugin.config.getBoolean("dynmap.debug")) {
                plugin.logger.info("Generated ${paths.size} paths for world ${world.name}")
            }
            paths.forEachIndexed { index, path ->
                val corners = getCorners(path)
                val areaMarker = claimsMarkerSet.createAreaMarker(
                    "claim_${faction.id.value}_${world.name}_$index",
                    faction.name,
                    false,
                    world.name,
                    corners.map { (x, _) -> x * 16.0 }.toDoubleArray(),
                    corners.map { (_, z) -> z * 16.0 }.toDoubleArray(),
                    false
                )
                areaMarker?.description = factionInfo
            }
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
