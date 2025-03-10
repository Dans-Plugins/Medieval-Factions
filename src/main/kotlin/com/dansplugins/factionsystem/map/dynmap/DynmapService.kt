package com.dansplugins.factionsystem.map.dynmap

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.claim.MfClaimService
import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.dynmap.TaskScheduler
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.map.MapService
import com.dansplugins.factionsystem.map.aliases.Point
import com.dansplugins.factionsystem.map.builders.ClaimPathBuilder
import com.dansplugins.factionsystem.map.builders.FactionInfoBuilder
import com.dansplugins.factionsystem.map.dynmap.helpers.MarkerSetHelper
import org.bukkit.World
import org.dynmap.DynmapAPI
import org.dynmap.markers.AreaMarker
import org.dynmap.markers.MarkerSet
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Service for managing Dynmap markers for factions.
 *
 * @param plugin The MedievalFactions plugin instance.
 */
class DynmapService(private val plugin: MedievalFactions) : MapService {

    private val dynmap = plugin.server.pluginManager.getPlugin("dynmap") as DynmapAPI
    private val factionMarkersByFactionId = ConcurrentHashMap<MfFactionId, List<AreaMarker>>()

    private val taskScheduler = TaskScheduler(plugin)
    private val factionInfoBuilder = FactionInfoBuilder(plugin)
    private val claimPathBuilder = ClaimPathBuilder()
    private val markerSetHelper = MarkerSetHelper()

    /**
     * Schedules an update for the claims of the specified faction.
     *
     * @param faction The faction whose claims need to be updated.
     */
    override fun scheduleUpdateClaims(faction: MfFaction) {
        taskScheduler.cancelTasks(faction.id)
        taskScheduler.scheduleTask(faction.id, { updateClaims(faction) }, 100L)
    }

    /**
     * Updates the claims for the specified faction.
     *
     * @param faction The faction whose claims need to be updated.
     */
    private fun updateClaims(faction: MfFaction) {
        if (plugin.config.getBoolean("dynmap.debug")) {
            plugin.logger.info("[Dynmap Service] Updating ${faction.name} claims.")
        }
        val markerApi = dynmap.markerAPI
        if (markerApi == null) {
            plugin.logger.warning("Failed to find Dynmap Marker API, skipping update of ${faction.name} claims")
            return
        }
        val claimsMarkerSet = markerSetHelper.getOrCreateMarkerSet(markerApi, "claims", "Claims")
        factionMarkersByFactionId[faction.id]?.forEach { marker -> marker.deleteMarker() }
        val claimService = plugin.services.claimService
        taskScheduler.scheduleTask(faction.id, { updateFactionClaims(faction, claimsMarkerSet, claimService) })
        if (plugin.config.getBoolean("dynmap.showRealms")) {
            val realmMarkerSet = markerSetHelper.getOrCreateMarkerSet(markerApi, "realms", "Realms")
            taskScheduler.scheduleTask(faction.id, { updateFactionRealm(faction, realmMarkerSet, claimService) })
        }
    }

    /**
     * Updates the claims for the specified faction in the given marker set.
     *
     * @param faction The faction whose claims need to be updated.
     * @param claimsMarkerSet The marker set for claims.
     * @param claimService The claim service.
     */
    private fun updateFactionClaims(faction: MfFaction, claimsMarkerSet: MarkerSet, claimService: MfClaimService) {
        val claims = claimService.getClaims(faction.id)
        if (plugin.config.getBoolean("dynmap.debug")) {
            plugin.logger.info("[Dynmap Service] Fetched ${claims.size} claims for faction ${faction.name}")
        }
        val factionInfo = factionInfoBuilder.build(faction)
        claims.groupBy { it.worldId }.forEach { (worldId, worldClaims) ->
            taskScheduler.scheduleTask(faction.id, { updateWorldClaims(faction, worldId, worldClaims, claimsMarkerSet, factionInfo) })
        }
    }

    /**
     * Updates the claims for the specified faction in the given world.
     *
     * @param faction The faction whose claims need to be updated.
     * @param worldId The ID of the world.
     * @param worldClaims The list of claims in the world.
     * @param claimsMarkerSet The marker set for claims.
     * @param factionInfo The faction information.
     */
    private fun updateWorldClaims(faction: MfFaction, worldId: UUID, worldClaims: List<MfClaimedChunk>, claimsMarkerSet: MarkerSet, factionInfo: String) {
        val world = plugin.server.getWorld(worldId)
        if (world != null) {
            taskScheduler.scheduleTask(faction.id, { createAreaMarkers(faction, world, worldClaims, claimsMarkerSet, factionInfo) })
        }
    }

    /**
     * Creates area markers for the specified faction in the given world.
     *
     * @param faction The faction whose area markers need to be created.
     * @param world The world.
     * @param worldClaims The list of claims in the world.
     * @param claimsMarkerSet The marker set for claims.
     * @param factionInfo The faction information.
     */
    private fun createAreaMarkers(faction: MfFaction, world: World, worldClaims: List<MfClaimedChunk>, claimsMarkerSet: MarkerSet, factionInfo: String) {
        val paths = claimPathBuilder.getPaths(worldClaims)
        if (plugin.config.getBoolean("dynmap.debug")) {
            plugin.logger.info("[Dynmap Service] Generated ${paths.size} paths for world ${world.name}")
        }
        paths.forEachIndexed { index, path ->
            val corners = getCorners(path)
            taskScheduler.scheduleTask(faction.id, { createAreaMarker(faction, world, corners, claimsMarkerSet, factionInfo, index) })
        }
        worldClaims.forEachIndexed { index, claim ->
            taskScheduler.scheduleTask(faction.id, { createClaimMarker(faction, world, claim, claimsMarkerSet, factionInfo, index) })
        }
    }

    /**
     * Creates an area marker for the specified faction in the given world.
     *
     * @param faction The faction whose area marker needs to be created.
     * @param world The world.
     * @param corners The list of corner points.
     * @param claimsMarkerSet The marker set for claims.
     * @param factionInfo The faction information.
     * @param index The index of the area marker.
     */
    private fun createAreaMarker(faction: MfFaction, world: World, corners: List<Point>, claimsMarkerSet: MarkerSet, factionInfo: String, index: Int) {
        val areaMarker = claimsMarkerSet.createAreaMarker(
            "claim_border_${faction.id}_${world.name}_$index",
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
                plugin.logger.info("[Dynmap Service] Created area marker for path $index in world ${world.name}")
            }
        }
    }

    /**
     * Creates a claim marker for the specified faction in the given world.
     *
     * @param faction The faction whose claim marker needs to be created.
     * @param world The world.
     * @param claim The claimed chunk.
     * @param claimsMarkerSet The marker set for claims.
     * @param factionInfo The faction information.
     * @param index The index of the claim marker.
     */
    private fun createClaimMarker(faction: MfFaction, world: World, claim: MfClaimedChunk, claimsMarkerSet: MarkerSet, factionInfo: String, index: Int) {
        val areaMarker = claimsMarkerSet.createAreaMarker(
            "claim_${faction.id}_${world.name}_$index",
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
    }

    /**
     * Updates the realm for the specified faction in the given marker set.
     *
     * @param faction The faction whose realm needs to be updated.
     * @param realmMarkerSet The marker set for realms.
     * @param claimService The claim service.
     */
    private fun updateFactionRealm(faction: MfFaction, realmMarkerSet: MarkerSet, claimService: MfClaimService) {
        val relationshipService = plugin.services.factionRelationshipService
        val realm = claimService.getClaims(faction.id) + relationshipService.getVassalTree(faction.id).flatMap(claimService::getClaims)
        realm.groupBy { it.worldId }.forEach { (worldId, worldClaims) ->
            taskScheduler.scheduleTask(faction.id, { updateWorldRealm(faction, worldId, worldClaims, realmMarkerSet) })
        }
    }

    /**
     * Updates the realm for the specified faction in the given world.
     *
     * @param faction The faction whose realm needs to be updated.
     * @param worldId The ID of the world.
     * @param worldClaims The list of claims in the world.
     * @param realmMarkerSet The marker set for realms.
     */
    private fun updateWorldRealm(faction: MfFaction, worldId: UUID, worldClaims: List<MfClaimedChunk>, realmMarkerSet: MarkerSet) {
        val world = plugin.server.getWorld(worldId)
        if (world != null) {
            taskScheduler.scheduleTask(faction.id, { createRealmMarkers(faction, world, worldClaims, realmMarkerSet) })
        }
    }

    /**
     * Creates realm markers for the specified faction in the given world.
     *
     * @param faction The faction whose realm markers need to be created.
     * @param world The world.
     * @param worldClaims The list of claims in the world.
     * @param realmMarkerSet The marker set for realms.
     */
    private fun createRealmMarkers(faction: MfFaction, world: World, worldClaims: List<MfClaimedChunk>, realmMarkerSet: MarkerSet) {
        val paths = claimPathBuilder.getPaths(worldClaims)
        if (plugin.config.getBoolean("dynmap.debug")) {
            plugin.logger.info("[Dynmap Service] Generated ${paths.size} paths for realm in world ${world.name}")
        }
        paths.forEachIndexed { index, path ->
            val corners = getCorners(path)
            taskScheduler.scheduleTask(faction.id, { createRealmAreaMarker(faction, world, corners, realmMarkerSet, index) })
        }
    }

    /**
     * Creates a realm area marker for the specified faction in the given world.
     *
     * @param faction The faction whose realm area marker needs to be created.
     * @param world The world.
     * @param corners The list of corner points.
     * @param realmMarkerSet The marker set for realms.
     * @param index The index of the realm area marker.
     */
    private fun createRealmAreaMarker(faction: MfFaction, world: World, corners: List<Point>, realmMarkerSet: MarkerSet, index: Int) {
        val areaMarker = realmMarkerSet.createAreaMarker(
            "realm_${faction.id}_${world.name}_$index",
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
        areaMarker.description = factionInfoBuilder.build(faction)
        val factionMarkers = factionMarkersByFactionId[faction.id] ?: listOf()
        factionMarkersByFactionId[faction.id] = factionMarkers + areaMarker
        if (plugin.config.getBoolean("dynmap.debug")) {
            plugin.logger.info("[Dynmap Service] Created realm area marker for path $index in world ${world.name}")
        }
    }

    /**
     * Calculates the corners from a list of points.
     *
     * @param points The list of points.
     * @return The list of corner points.
     */
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
            plugin.logger.info("[Dynmap Service] Calculated ${corners.size} corners from points")
        }
        return corners
    }
}
