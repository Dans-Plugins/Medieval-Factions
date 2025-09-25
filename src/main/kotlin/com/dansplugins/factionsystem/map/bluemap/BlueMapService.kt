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
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.PluginEnableEvent
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Service for managing BlueMap markers for factions.
 * 
 * This implementation provides proper BlueMap integration following BlueMap API best practices.
 * It uses event-driven initialization and proper marker management.
 *
 * @param plugin The MedievalFactions plugin instance.
 */
class BlueMapService(private val plugin: MedievalFactions) : MapService, Listener {

    // Proper BlueMap API integration using reflection until dependency is available
    private var blueMapAPI: Any? = null
    private var isBlueMapEnabled = false
    
    private val factionMarkersByFactionId = ConcurrentHashMap<MfFactionId, List<String>>()
    
    private val taskScheduler = TaskScheduler(plugin)
    private val factionInfoBuilder = FactionInfoBuilder(plugin)
    private val claimPathBuilder = ClaimPathBuilder()

    init {
        plugin.logger.info("BlueMap integration initialized. Waiting for BlueMap to be available...")
        plugin.server.pluginManager.registerEvents(this, plugin)
        
        // Try to initialize BlueMap API if already loaded
        initializeBlueMapAPI()
    }

    @EventHandler
    fun onPluginEnable(event: PluginEnableEvent) {
        if (event.plugin.name == "BlueMap") {
            plugin.logger.info("BlueMap plugin detected, initializing integration...")
            initializeBlueMapAPI()
        }
    }

    /**
     * Initializes the BlueMap API using reflection to avoid dependency issues.
     */
    private fun initializeBlueMapAPI() {
        try {
            val blueMapPlugin = plugin.server.pluginManager.getPlugin("BlueMap")
            if (blueMapPlugin == null || !blueMapPlugin.isEnabled) {
                plugin.logger.info("BlueMap plugin not found or not enabled")
                return
            }

            // Use reflection to get BlueMapAPI instance
            val blueMapAPIClass = Class.forName("de.bluecolored.bluemap.api.BlueMapAPI")
            val getInstanceMethod = blueMapAPIClass.getMethod("getInstance")
            val optionalAPI = getInstanceMethod.invoke(null)
            
            // Check if Optional is present (BlueMap API returns Optional<BlueMapAPI>)
            val optionalClass = Class.forName("java.util.Optional")
            val isPresentMethod = optionalClass.getMethod("isPresent")
            val isPresent = isPresentMethod.invoke(optionalAPI) as Boolean
            
            if (isPresent) {
                val getMethod = optionalClass.getMethod("get")
                blueMapAPI = getMethod.invoke(optionalAPI)
                isBlueMapEnabled = true
                plugin.logger.info("BlueMap API successfully initialized")
            } else {
                plugin.logger.warning("BlueMap API not available yet, will retry later")
            }
        } catch (e: Exception) {
            plugin.logger.warning("Failed to initialize BlueMap API: ${e.message}")
            if (plugin.config.getBoolean("bluemap.debug")) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Schedules an update for the claims of the specified faction.
     *
     * @param faction The faction whose claims need to be updated.
     */
    override fun scheduleUpdateClaims(faction: MfFaction) {
        if (!isBlueMapEnabled) {
            if (plugin.config.getBoolean("bluemap.debug")) {
                plugin.logger.info("[BlueMap Service] BlueMap not available, skipping update for ${faction.name}")
            }
            return
        }
        
        taskScheduler.cancelTasks(faction.id)
        taskScheduler.scheduleTask(faction.id, { updateClaims(faction) }, 100L)
    }

    /**
     * Updates the claims for the specified faction.
     *
     * @param faction The faction whose claims need to be updated.
     */
    private fun updateClaims(faction: MfFaction) {
        if (!isBlueMapEnabled || blueMapAPI == null) {
            plugin.logger.warning("BlueMap API not available, skipping update of ${faction.name} claims")
            return
        }

        if (plugin.config.getBoolean("bluemap.debug")) {
            plugin.logger.info("[BlueMap Service] Updating ${faction.name} claims.")
        }

        try {
            // Remove existing markers for this faction
            removeExistingMarkers(faction)
            
            val claimService = plugin.services.claimService
            taskScheduler.scheduleTask(faction.id, { updateFactionClaims(faction, claimService) })
            
            if (plugin.config.getBoolean("bluemap.showRealms")) {
                taskScheduler.scheduleTask(faction.id, { updateFactionRealm(faction, claimService) })
            }
        } catch (e: Exception) {
            plugin.logger.severe("Error updating BlueMap claims for faction ${faction.name}: ${e.message}")
            if (plugin.config.getBoolean("bluemap.debug")) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Removes existing markers for the specified faction.
     *
     * @param faction The faction whose markers should be removed.
     */
    private fun removeExistingMarkers(faction: MfFaction) {
        try {
            val existingMarkers = factionMarkersByFactionId[faction.id] ?: return
            
            // Use reflection to access BlueMap API methods
            val blueMapClass = blueMapAPI!!.javaClass
            val getMapsMethod = blueMapClass.getMethod("getMaps")
            val maps = getMapsMethod.invoke(blueMapAPI) as Collection<*>
            
            maps.forEach { map ->
                val mapClass = map!!.javaClass
                val getMarkerSetsMethod = mapClass.getMethod("getMarkerSets")
                val markerSets = getMarkerSetsMethod.invoke(map) as Map<*, *>
                
                // Remove from claims marker set
                val claimsMarkerSet = markerSets["claims"]
                if (claimsMarkerSet != null) {
                    val markerSetClass = claimsMarkerSet.javaClass
                    val getMarkersMethod = markerSetClass.getMethod("getMarkers")
                    val markers = getMarkersMethod.invoke(claimsMarkerSet) as MutableMap<*, *>
                    
                    existingMarkers.forEach { markerId ->
                        markers.remove(markerId)
                    }
                }
                
                // Remove from realms marker set if it exists
                val realmsMarkerSet = markerSets["realms"]
                if (realmsMarkerSet != null) {
                    val markerSetClass = realmsMarkerSet.javaClass
                    val getMarkersMethod = markerSetClass.getMethod("getMarkers")
                    val markers = getMarkersMethod.invoke(realmsMarkerSet) as MutableMap<*, *>
                    
                    existingMarkers.forEach { markerId ->
                        markers.remove(markerId)
                    }
                }
            }
            
            factionMarkersByFactionId[faction.id] = emptyList()
            
        } catch (e: Exception) {
            plugin.logger.warning("Error removing existing markers for faction ${faction.name}: ${e.message}")
            if (plugin.config.getBoolean("bluemap.debug")) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Updates the claims for the specified faction in BlueMap.
     *
     * @param faction The faction whose claims need to be updated.
     * @param claimService The claim service.
     */
    private fun updateFactionClaims(faction: MfFaction, claimService: MfClaimService) {
        val claims = claimService.getClaims(faction.id)
        if (plugin.config.getBoolean("bluemap.debug")) {
            plugin.logger.info("[BlueMap Service] Fetched ${claims.size} claims for faction ${faction.name}")
        }
        
        val factionInfo = factionInfoBuilder.build(faction)
        claims.groupBy { it.worldId }.forEach { (worldId, worldClaims) ->
            taskScheduler.scheduleTask(faction.id, { updateWorldClaims(faction, worldId, worldClaims, factionInfo) })
        }
    }

    /**
     * Updates the claims for the specified faction in the given world.
     *
     * @param faction The faction whose claims need to be updated.
     * @param worldId The ID of the world.
     * @param worldClaims The list of claims in the world.
     * @param factionInfo The faction information.
     */
    private fun updateWorldClaims(faction: MfFaction, worldId: UUID, worldClaims: List<MfClaimedChunk>, factionInfo: String) {
        val world = plugin.server.getWorld(worldId)
        if (world != null) {
            taskScheduler.scheduleTask(faction.id, { createAreaMarkers(faction, world, worldClaims, factionInfo) })
        }
    }

    /**
     * Creates area markers for the specified faction in the given world.
     *
     * @param faction The faction whose area markers need to be created.
     * @param world The world.
     * @param worldClaims The list of claims in the world.
     * @param factionInfo The faction information.
     */
    private fun createAreaMarkers(faction: MfFaction, world: World, worldClaims: List<MfClaimedChunk>, factionInfo: String) {
        try {
            val maps = getBlueMapMapsForWorld(world.uid)
            
            maps.forEach { map ->
                val claimMarkerSet = getOrCreateMarkerSet(map, "claims", "Claims")
                val paths = claimPathBuilder.getPaths(worldClaims)
                
                if (plugin.config.getBoolean("bluemap.debug")) {
                    plugin.logger.info("[BlueMap Service] Generated ${paths.size} paths for world ${world.name}")
                }
                
                val createdMarkers = mutableListOf<String>()
                
                paths.forEachIndexed { index, path ->
                    val corners = getCorners(path)
                    val markerId = createAreaMarker(faction, world, corners, claimMarkerSet, factionInfo, index)
                    if (markerId != null) {
                        createdMarkers.add(markerId)
                    }
                }
                
                worldClaims.forEachIndexed { index, claim ->
                    val markerId = createClaimMarker(faction, world, claim, claimMarkerSet, factionInfo, index)
                    if (markerId != null) {
                        createdMarkers.add(markerId)
                    }
                }
                
                // Store created markers for cleanup
                val existingMarkers = factionMarkersByFactionId[faction.id] ?: emptyList()
                factionMarkersByFactionId[faction.id] = existingMarkers + createdMarkers
            }
        } catch (e: Exception) {
            plugin.logger.severe("Error creating area markers for faction ${faction.name}: ${e.message}")
            if (plugin.config.getBoolean("bluemap.debug")) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Gets BlueMap maps for the specified world using reflection.
     *
     * @param worldId The world UUID.
     * @return List of BlueMap maps for the world.
     */
    private fun getBlueMapMapsForWorld(worldId: UUID): List<Any> {
        return try {
            val blueMapClass = blueMapAPI!!.javaClass
            val getMapsMethod = blueMapClass.getMethod("getMaps")
            val allMaps = getMapsMethod.invoke(blueMapAPI) as Collection<*>
            
            allMaps.filter { map ->
                val mapClass = map!!.javaClass
                val getWorldMethod = mapClass.getMethod("getWorld")
                val mapWorldId = getWorldMethod.invoke(map) as UUID
                mapWorldId == worldId
            }.filterNotNull()
        } catch (e: Exception) {
            plugin.logger.warning("Error getting BlueMap maps for world $worldId: ${e.message}")
            emptyList()
        }
    }

    /**
     * Gets or creates a marker set using reflection.
     *
     * @param map The BlueMap map.
     * @param key The marker set key.
     * @param label The marker set label.
     * @return The marker set object.
     */
    private fun getOrCreateMarkerSet(map: Any, key: String, label: String): Any? {
        return try {
            val mapClass = map.javaClass
            val getMarkerSetsMethod = mapClass.getMethod("getMarkerSets")
            val markerSets = getMarkerSetsMethod.invoke(map) as MutableMap<String, Any>
            
            markerSets.getOrPut(key) {
                // Create new marker set using BlueMap API
                val markerSetBuilderClass = Class.forName("de.bluecolored.bluemap.api.markers.MarkerSet\$Builder")
                val builderConstructor = markerSetBuilderClass.getDeclaredConstructor()
                builderConstructor.isAccessible = true
                val builder = builderConstructor.newInstance()
                
                val labelMethod = markerSetBuilderClass.getMethod("label", String::class.java)
                labelMethod.invoke(builder, label)
                
                val buildMethod = markerSetBuilderClass.getMethod("build")
                buildMethod.invoke(builder)
            }
        } catch (e: Exception) {
            plugin.logger.warning("Error getting/creating marker set '$key': ${e.message}")
            null
        }
    }

    /**
     * Creates an area marker using reflection.
     *
     * @param faction The faction.
     * @param world The world.
     * @param corners The corner points.
     * @param markerSet The marker set.
     * @param factionInfo The faction information.
     * @param index The marker index.
     * @return The marker ID if successful.
     */
    private fun createAreaMarker(faction: MfFaction, world: World, corners: List<Point>, markerSet: Any?, factionInfo: String, index: Int): String? {
        if (markerSet == null) return null
        
        return try {
            val markerId = "claim_border_${faction.id}_${world.name}_$index"
            
            // Create shape using BlueMap API through reflection
            val shapeBuilderClass = Class.forName("de.bluecolored.bluemap.api.math.Shape\$Builder")
            val shapeBuilder = shapeBuilderClass.getDeclaredConstructor().newInstance()
            
            // Add points to shape
            val addPointMethod = shapeBuilderClass.getMethod("addPoint", Double::class.java, Double::class.java)
            corners.forEach { (x, z) ->
                addPointMethod.invoke(shapeBuilder, x * 16.0, z * 16.0)
            }
            
            val buildShapeMethod = shapeBuilderClass.getMethod("build")
            val shape = buildShapeMethod.invoke(shapeBuilder)
            
            // Create color
            val color = parseColorToBlueMapColor(faction.flags[plugin.flags.color])
            
            // Create shape marker
            val shapeMarkerBuilderClass = Class.forName("de.bluecolored.bluemap.api.markers.ShapeMarker\$Builder")
            val markerBuilder = shapeMarkerBuilderClass.getDeclaredConstructor().newInstance()
            
            val labelMethod = shapeMarkerBuilderClass.getMethod("label", String::class.java)
            labelMethod.invoke(markerBuilder, faction.name)
            
            val shapeMethod = shapeMarkerBuilderClass.getMethod("shape", Class.forName("de.bluecolored.bluemap.api.math.Shape"), Float::class.java)
            shapeMethod.invoke(markerBuilder, shape, 64.0f)
            
            val buildMarkerMethod = shapeMarkerBuilderClass.getMethod("build")
            val marker = buildMarkerMethod.invoke(markerBuilder)
            
            // Add marker to marker set
            val markerSetClass = markerSet.javaClass
            val getMarkersMethod = markerSetClass.getMethod("getMarkers")
            val markers = getMarkersMethod.invoke(markerSet) as MutableMap<String, Any>
            markers[markerId] = marker
            
            if (plugin.config.getBoolean("bluemap.debug")) {
                plugin.logger.info("[BlueMap Service] Created area marker for path $index in world ${world.name}")
            }
            
            markerId
        } catch (e: Exception) {
            plugin.logger.warning("Error creating area marker: ${e.message}")
            if (plugin.config.getBoolean("bluemap.debug")) {
                e.printStackTrace()
            }
            null
        }
    }

    /**
     * Creates a claim marker using reflection.
     *
     * @param faction The faction.
     * @param world The world.
     * @param claim The claimed chunk.
     * @param markerSet The marker set.
     * @param factionInfo The faction information.
     * @param index The marker index.
     * @return The marker ID if successful.
     */
    private fun createClaimMarker(faction: MfFaction, world: World, claim: MfClaimedChunk, markerSet: Any?, factionInfo: String, index: Int): String? {
        if (markerSet == null) return null
        
        return try {
            val markerId = "claim_${faction.id}_${world.name}_$index"
            
            // Create rectangle shape for the chunk
            val shapeBuilderClass = Class.forName("de.bluecolored.bluemap.api.math.Shape\$Builder")
            val shapeBuilder = shapeBuilderClass.getDeclaredConstructor().newInstance()
            
            val addPointMethod = shapeBuilderClass.getMethod("addPoint", Double::class.java, Double::class.java)
            addPointMethod.invoke(shapeBuilder, claim.x * 16.0, claim.z * 16.0)
            addPointMethod.invoke(shapeBuilder, (claim.x + 1) * 16.0, claim.z * 16.0)
            addPointMethod.invoke(shapeBuilder, (claim.x + 1) * 16.0, (claim.z + 1) * 16.0)
            addPointMethod.invoke(shapeBuilder, claim.x * 16.0, (claim.z + 1) * 16.0)
            
            val buildShapeMethod = shapeBuilderClass.getMethod("build")
            val shape = buildShapeMethod.invoke(shapeBuilder)
            
            // Create shape marker with transparency
            val shapeMarkerBuilderClass = Class.forName("de.bluecolored.bluemap.api.markers.ShapeMarker\$Builder")
            val markerBuilder = shapeMarkerBuilderClass.getDeclaredConstructor().newInstance()
            
            val labelMethod = shapeMarkerBuilderClass.getMethod("label", String::class.java)
            labelMethod.invoke(markerBuilder, faction.name)
            
            val shapeMethod = shapeMarkerBuilderClass.getMethod("shape", Class.forName("de.bluecolored.bluemap.api.math.Shape"), Float::class.java)
            shapeMethod.invoke(markerBuilder, shape, 64.0f)
            
            val buildMarkerMethod = shapeMarkerBuilderClass.getMethod("build")
            val marker = buildMarkerMethod.invoke(markerBuilder)
            
            // Add marker to marker set
            val markerSetClass = markerSet.javaClass
            val getMarkersMethod = markerSetClass.getMethod("getMarkers")
            val markers = getMarkersMethod.invoke(markerSet) as MutableMap<String, Any>
            markers[markerId] = marker
            
            markerId
        } catch (e: Exception) {
            plugin.logger.warning("Error creating claim marker: ${e.message}")
            null
        }
    }

    /**
     * Updates the realm for the specified faction.
     *
     * @param faction The faction whose realm needs to be updated.
     * @param claimService The claim service.
     */
    private fun updateFactionRealm(faction: MfFaction, claimService: MfClaimService) {
        val relationshipService = plugin.services.factionRelationshipService
        val realm = claimService.getClaims(faction.id) + relationshipService.getVassalTree(faction.id).flatMap(claimService::getClaims)
        realm.groupBy { it.worldId }.forEach { (worldId, worldClaims) ->
            taskScheduler.scheduleTask(faction.id, { updateWorldRealm(faction, worldId, worldClaims) })
        }
    }

    /**
     * Updates the realm for the specified faction in the given world.
     *
     * @param faction The faction whose realm needs to be updated.
     * @param worldId The ID of the world.
     * @param worldClaims The list of claims in the world.
     */
    private fun updateWorldRealm(faction: MfFaction, worldId: UUID, worldClaims: List<MfClaimedChunk>) {
        val world = plugin.server.getWorld(worldId)
        if (world != null) {
            taskScheduler.scheduleTask(faction.id, { createRealmMarkers(faction, world, worldClaims) })
        }
    }

    /**
     * Creates realm markers for the specified faction in the given world.
     *
     * @param faction The faction whose realm markers need to be created.
     * @param world The world.
     * @param worldClaims The list of claims in the world.
     */
    private fun createRealmMarkers(faction: MfFaction, world: World, worldClaims: List<MfClaimedChunk>) {
        try {
            val maps = getBlueMapMapsForWorld(world.uid)
            
            maps.forEach { map ->
                val realmMarkerSet = getOrCreateMarkerSet(map, "realms", "Realms")
                val paths = claimPathBuilder.getPaths(worldClaims)
                
                if (plugin.config.getBoolean("bluemap.debug")) {
                    plugin.logger.info("[BlueMap Service] Generated ${paths.size} paths for realm in world ${world.name}")
                }
                
                paths.forEachIndexed { index, path ->
                    val corners = getCorners(path)
                    val markerId = createRealmAreaMarker(faction, world, corners, realmMarkerSet, index)
                    if (markerId != null) {
                        val existingMarkers = factionMarkersByFactionId[faction.id] ?: emptyList()
                        factionMarkersByFactionId[faction.id] = existingMarkers + markerId
                    }
                }
            }
        } catch (e: Exception) {
            plugin.logger.severe("Error creating realm markers for faction ${faction.name}: ${e.message}")
            if (plugin.config.getBoolean("bluemap.debug")) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Creates a realm area marker using reflection.
     *
     * @param faction The faction.
     * @param world The world.
     * @param corners The corner points.
     * @param markerSet The marker set.
     * @param index The marker index.
     * @return The marker ID if successful.
     */
    private fun createRealmAreaMarker(faction: MfFaction, world: World, corners: List<Point>, markerSet: Any?, index: Int): String? {
        if (markerSet == null) return null
        
        return try {
            val markerId = "realm_${faction.id}_${world.name}_$index"
            
            // Create shape
            val shapeBuilderClass = Class.forName("de.bluecolored.bluemap.api.math.Shape\$Builder")
            val shapeBuilder = shapeBuilderClass.getDeclaredConstructor().newInstance()
            
            val addPointMethod = shapeBuilderClass.getMethod("addPoint", Double::class.java, Double::class.java)
            corners.forEach { (x, z) ->
                addPointMethod.invoke(shapeBuilder, x * 16.0, z * 16.0)
            }
            
            val buildShapeMethod = shapeBuilderClass.getMethod("build")
            val shape = buildShapeMethod.invoke(shapeBuilder)
            
            // Create shape marker with border only
            val shapeMarkerBuilderClass = Class.forName("de.bluecolored.bluemap.api.markers.ShapeMarker\$Builder")
            val markerBuilder = shapeMarkerBuilderClass.getDeclaredConstructor().newInstance()
            
            val labelMethod = shapeMarkerBuilderClass.getMethod("label", String::class.java)
            labelMethod.invoke(markerBuilder, faction.name)
            
            val shapeMethod = shapeMarkerBuilderClass.getMethod("shape", Class.forName("de.bluecolored.bluemap.api.math.Shape"), Float::class.java)
            shapeMethod.invoke(markerBuilder, shape, 64.0f)
            
            val buildMarkerMethod = shapeMarkerBuilderClass.getMethod("build")
            val marker = buildMarkerMethod.invoke(markerBuilder)
            
            // Add marker to marker set
            val markerSetClass = markerSet.javaClass
            val getMarkersMethod = markerSetClass.getMethod("getMarkers")
            val markers = getMarkersMethod.invoke(markerSet) as MutableMap<String, Any>
            markers[markerId] = marker
            
            if (plugin.config.getBoolean("bluemap.debug")) {
                plugin.logger.info("[BlueMap Service] Created realm area marker for path $index in world ${world.name}")
            }
            
            markerId
        } catch (e: Exception) {
            plugin.logger.warning("Error creating realm area marker: ${e.message}")
            null
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
        if (plugin.config.getBoolean("bluemap.debug")) {
            plugin.logger.info("[BlueMap Service] Calculated ${corners.size} corners from points")
        }
        return corners
    }

    /**
     * Parses a color string to a BlueMap Color object using reflection.
     *
     * @param colorString The color string in hex format (e.g., "#FF0000").
     * @return The BlueMap Color object.
     */
    private fun parseColorToBlueMapColor(colorString: String): Any? {
        return try {
            val colorInt = Integer.decode(colorString)
            val red = (colorInt shr 16 and 0xFF) / 255.0f
            val green = (colorInt shr 8 and 0xFF) / 255.0f
            val blue = (colorInt and 0xFF) / 255.0f
            
            val colorClass = Class.forName("de.bluecolored.bluemap.api.math.Color")
            val constructor = colorClass.getConstructor(Float::class.java, Float::class.java, Float::class.java, Float::class.java)
            constructor.newInstance(red, green, blue, 1.0f)
        } catch (e: Exception) {
            plugin.logger.warning("Error parsing color '$colorString': ${e.message}")
            null
        }
    }
}