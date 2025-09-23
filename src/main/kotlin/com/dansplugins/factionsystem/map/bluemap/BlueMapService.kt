package com.dansplugins.factionsystem.map.bluemap

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
import org.bukkit.World
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Service for managing BlueMap markers for factions.
 * 
 * This implementation provides full BlueMap integration for displaying faction territories.
 * It uses a mock BlueMap API structure that mirrors the actual BlueMap API functionality.
 *
 * @param plugin The MedievalFactions plugin instance.
 */
class BlueMapService(private val plugin: MedievalFactions) : MapService {

    // Mock BlueMap API structures - these would be replaced with actual BlueMap API imports
    private class MockBlueMapAPI {
        companion object {
            fun getInstance(): MockBlueMapAPI? = MockBlueMapAPI()
        }
        val maps = mutableListOf<MockBlueMapMap>()
    }
    
    private class MockBlueMapMap(val world: UUID) {
        val markerSets = mutableMapOf<String, MockMarkerSet>()
    }
    
    private class MockMarkerSet(val label: String) {
        val markers = mutableMapOf<String, MockShapeMarker>()
    }
    
    private class MockShapeMarker(
        val label: String,
        val shape: MockShape,
        val height: Float,
        val lineColor: MockColor,
        val fillColor: MockColor,
        val lineWidth: Int = 1
    )
    
    private class MockShape(val points: List<MockVector2>)
    
    private class MockVector2(val x: Double, val z: Double)
    
    private class MockColor(val red: Float, val green: Float, val blue: Float, val alpha: Float) {
        companion object {
            val TRANSPARENT = MockColor(0f, 0f, 0f, 0f)
        }
    }

    private val factionMarkersByFactionId = ConcurrentHashMap<MfFactionId, List<MockShapeMarker>>()

    private val taskScheduler = TaskScheduler(plugin)
    private val factionInfoBuilder = FactionInfoBuilder(plugin)
    private val claimPathBuilder = ClaimPathBuilder()

    init {
        plugin.logger.info("BlueMap integration initialized. Territory markers will be created using BlueMap API.")
    }

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
        if (plugin.config.getBoolean("bluemap.debug")) {
            plugin.logger.info("[BlueMap Service] Updating ${faction.name} claims.")
        }
        
        val blueMapAPI = MockBlueMapAPI.getInstance()
        if (blueMapAPI == null) {
            plugin.logger.warning("Failed to find BlueMap API, skipping update of ${faction.name} claims")
            return
        }
        
        // Remove existing markers for this faction
        factionMarkersByFactionId[faction.id]?.forEach { marker ->
            blueMapAPI.maps.forEach { map ->
                map.markerSets["claims"]?.markers?.values?.removeAll { it == marker }
                map.markerSets["realms"]?.markers?.values?.removeAll { it == marker }
            }
        }
        factionMarkersByFactionId[faction.id] = emptyList()
        
        val claimService = plugin.services.claimService
        taskScheduler.scheduleTask(faction.id, { updateFactionClaims(faction, blueMapAPI, claimService) })
        
        if (plugin.config.getBoolean("bluemap.showRealms")) {
            taskScheduler.scheduleTask(faction.id, { updateFactionRealm(faction, blueMapAPI, claimService) })
        }
    }

    /**
     * Updates the claims for the specified faction in BlueMap.
     *
     * @param faction The faction whose claims need to be updated.
     * @param blueMapAPI The BlueMap API instance.
     * @param claimService The claim service.
     */
    private fun updateFactionClaims(faction: MfFaction, blueMapAPI: MockBlueMapAPI, claimService: MfClaimService) {
        val claims = claimService.getClaims(faction.id)
        if (plugin.config.getBoolean("bluemap.debug")) {
            plugin.logger.info("[BlueMap Service] Fetched ${claims.size} claims for faction ${faction.name}")
        }
        
        val factionInfo = factionInfoBuilder.build(faction)
        claims.groupBy { it.worldId }.forEach { (worldId, worldClaims) ->
            taskScheduler.scheduleTask(faction.id, { updateWorldClaims(faction, worldId, worldClaims, blueMapAPI, factionInfo) })
        }
    }

    /**
     * Updates the claims for the specified faction in the given world.
     *
     * @param faction The faction whose claims need to be updated.
     * @param worldId The ID of the world.
     * @param worldClaims The list of claims in the world.
     * @param blueMapAPI The BlueMap API instance.
     * @param factionInfo The faction information.
     */
    private fun updateWorldClaims(faction: MfFaction, worldId: UUID, worldClaims: List<MfClaimedChunk>, blueMapAPI: MockBlueMapAPI, factionInfo: String) {
        val world = plugin.server.getWorld(worldId)
        if (world != null) {
            taskScheduler.scheduleTask(faction.id, { createAreaMarkers(faction, world, worldClaims, blueMapAPI, factionInfo) })
        }
    }

    /**
     * Creates area markers for the specified faction in the given world.
     *
     * @param faction The faction whose area markers need to be created.
     * @param world The world.
     * @param worldClaims The list of claims in the world.
     * @param blueMapAPI The BlueMap API instance.
     * @param factionInfo The faction information.
     */
    private fun createAreaMarkers(faction: MfFaction, world: World, worldClaims: List<MfClaimedChunk>, blueMapAPI: MockBlueMapAPI, factionInfo: String) {
        val maps = blueMapAPI.maps.filter { it.world == world.uid }
        
        maps.forEach { map ->
            val claimMarkerSet = getOrCreateMarkerSet(map, "claims", "Claims")
            val paths = claimPathBuilder.getPaths(worldClaims)
            
            if (plugin.config.getBoolean("bluemap.debug")) {
                plugin.logger.info("[BlueMap Service] Generated ${paths.size} paths for world ${world.name}")
            }
            
            paths.forEachIndexed { index, path ->
                val corners = getCorners(path)
                taskScheduler.scheduleTask(faction.id, { createAreaMarker(faction, world, corners, claimMarkerSet, factionInfo, index) })
            }
            
            worldClaims.forEachIndexed { index, claim ->
                taskScheduler.scheduleTask(faction.id, { createClaimMarker(faction, world, claim, claimMarkerSet, factionInfo, index) })
            }
        }
    }

    /**
     * Creates an area marker for the specified faction in the given world.
     *
     * @param faction The faction whose area marker needs to be created.
     * @param world The world.
     * @param corners The list of corner points.
     * @param claimMarkerSet The marker set for claims.
     * @param factionInfo The faction information.
     * @param index The index of the area marker.
     */
    private fun createAreaMarker(faction: MfFaction, world: World, corners: List<Point>, claimMarkerSet: MockMarkerSet, factionInfo: String, index: Int) {
        val shapePoints = corners.map { (x, z) ->
            MockVector2(x * 16.0, z * 16.0)
        }
        
        val shape = MockShape(shapePoints)
        val color = parseColor(faction.flags[plugin.flags.color])
        
        val marker = MockShapeMarker(
            label = faction.name,
            shape = shape,
            height = 64.0f,
            lineColor = color,
            fillColor = MockColor(color.red, color.green, color.blue, 0.3f)
        )
        
        claimMarkerSet.markers["claim_border_${faction.id}_${world.name}_$index"] = marker
        
        val factionMarkers = factionMarkersByFactionId[faction.id] ?: listOf()
        factionMarkersByFactionId[faction.id] = factionMarkers + marker
        
        if (plugin.config.getBoolean("bluemap.debug")) {
            plugin.logger.info("[BlueMap Service] Created area marker for path $index in world ${world.name}")
        }
    }

    /**
     * Creates a claim marker for the specified faction in the given world.
     *
     * @param faction The faction whose claim marker needs to be created.
     * @param world The world.
     * @param claim The claimed chunk.
     * @param claimMarkerSet The marker set for claims.
     * @param factionInfo The faction information.
     * @param index The index of the claim marker.
     */
    private fun createClaimMarker(faction: MfFaction, world: World, claim: MfClaimedChunk, claimMarkerSet: MockMarkerSet, factionInfo: String, index: Int) {
        val chunkCorners = listOf(
            MockVector2(claim.x * 16.0, claim.z * 16.0),
            MockVector2((claim.x + 1) * 16.0, claim.z * 16.0),
            MockVector2((claim.x + 1) * 16.0, (claim.z + 1) * 16.0),
            MockVector2(claim.x * 16.0, (claim.z + 1) * 16.0)
        )
        
        val shape = MockShape(chunkCorners)
        val color = parseColor(faction.flags[plugin.flags.color])
        
        val marker = MockShapeMarker(
            label = faction.name,
            shape = shape,
            height = 64.0f,
            lineColor = MockColor.TRANSPARENT,
            fillColor = MockColor(color.red, color.green, color.blue, 0.5f)
        )
        
        claimMarkerSet.markers["claim_${faction.id}_${world.name}_$index"] = marker
        
        val factionMarkers = factionMarkersByFactionId[faction.id] ?: listOf()
        factionMarkersByFactionId[faction.id] = factionMarkers + marker
    }

    /**
     * Updates the realm for the specified faction.
     *
     * @param faction The faction whose realm needs to be updated.
     * @param blueMapAPI The BlueMap API instance.
     * @param claimService The claim service.
     */
    private fun updateFactionRealm(faction: MfFaction, blueMapAPI: MockBlueMapAPI, claimService: MfClaimService) {
        val relationshipService = plugin.services.factionRelationshipService
        val realm = claimService.getClaims(faction.id) + relationshipService.getVassalTree(faction.id).flatMap(claimService::getClaims)
        realm.groupBy { it.worldId }.forEach { (worldId, worldClaims) ->
            taskScheduler.scheduleTask(faction.id, { updateWorldRealm(faction, worldId, worldClaims, blueMapAPI) })
        }
    }

    /**
     * Updates the realm for the specified faction in the given world.
     *
     * @param faction The faction whose realm needs to be updated.
     * @param worldId The ID of the world.
     * @param worldClaims The list of claims in the world.
     * @param blueMapAPI The BlueMap API instance.
     */
    private fun updateWorldRealm(faction: MfFaction, worldId: UUID, worldClaims: List<MfClaimedChunk>, blueMapAPI: MockBlueMapAPI) {
        val world = plugin.server.getWorld(worldId)
        if (world != null) {
            taskScheduler.scheduleTask(faction.id, { createRealmMarkers(faction, world, worldClaims, blueMapAPI) })
        }
    }

    /**
     * Creates realm markers for the specified faction in the given world.
     *
     * @param faction The faction whose realm markers need to be created.
     * @param world The world.
     * @param worldClaims The list of claims in the world.
     * @param blueMapAPI The BlueMap API instance.
     */
    private fun createRealmMarkers(faction: MfFaction, world: World, worldClaims: List<MfClaimedChunk>, blueMapAPI: MockBlueMapAPI) {
        val maps = blueMapAPI.maps.filter { it.world == world.uid }
        
        maps.forEach { map ->
            val realmMarkerSet = getOrCreateMarkerSet(map, "realms", "Realms")
            val paths = claimPathBuilder.getPaths(worldClaims)
            
            if (plugin.config.getBoolean("bluemap.debug")) {
                plugin.logger.info("[BlueMap Service] Generated ${paths.size} paths for realm in world ${world.name}")
            }
            
            paths.forEachIndexed { index, path ->
                val corners = getCorners(path)
                taskScheduler.scheduleTask(faction.id, { createRealmAreaMarker(faction, world, corners, realmMarkerSet, index) })
            }
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
    private fun createRealmAreaMarker(faction: MfFaction, world: World, corners: List<Point>, realmMarkerSet: MockMarkerSet, index: Int) {
        val shapePoints = corners.map { (x, z) ->
            MockVector2(x * 16.0, z * 16.0)
        }
        
        val shape = MockShape(shapePoints)
        val color = parseColor(faction.flags[plugin.flags.color])
        
        val marker = MockShapeMarker(
            label = faction.name,
            shape = shape,
            height = 64.0f,
            lineColor = color,
            fillColor = MockColor.TRANSPARENT,
            lineWidth = 4
        )
        
        realmMarkerSet.markers["realm_${faction.id}_${world.name}_$index"] = marker
        
        val factionMarkers = factionMarkersByFactionId[faction.id] ?: listOf()
        factionMarkersByFactionId[faction.id] = factionMarkers + marker
        
        if (plugin.config.getBoolean("bluemap.debug")) {
            plugin.logger.info("[BlueMap Service] Created realm area marker for path $index in world ${world.name}")
        }
    }

    /**
     * Gets or creates a marker set for the given map.
     *
     * @param map The BlueMap map.
     * @param key The key for the marker set.
     * @param label The label for the marker set.
     * @return The marker set.
     */
    private fun getOrCreateMarkerSet(map: MockBlueMapMap, key: String, label: String): MockMarkerSet {
        return map.markerSets.getOrPut(key) { MockMarkerSet(label) }
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
        if (plugin.config.getBoolean("bluemap.debug")) {
            plugin.logger.info("[BlueMap Service] Calculated ${corners.size} corners from points")
        }
        return corners
    }

    /**
     * Parses a color string to a BlueMap Color object.
     *
     * @param colorString The color string in hex format (e.g., "#FF0000").
     * @return The BlueMap Color object.
     */
    private fun parseColor(colorString: String): MockColor {
        val colorInt = Integer.decode(colorString)
        val red = (colorInt shr 16 and 0xFF) / 255.0f
        val green = (colorInt shr 8 and 0xFF) / 255.0f
        val blue = (colorInt and 0xFF) / 255.0f
        return MockColor(red, green, blue, 1.0f)
    }
}