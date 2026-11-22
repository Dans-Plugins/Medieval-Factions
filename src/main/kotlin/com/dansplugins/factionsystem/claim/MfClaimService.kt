package com.dansplugins.factionsystem.claim

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfChunkPosition
import com.dansplugins.factionsystem.area.MfPosition
import com.dansplugins.factionsystem.event.faction.FactionClaimEvent
import com.dansplugins.factionsystem.event.faction.FactionUnclaimEvent
import com.dansplugins.factionsystem.exception.EventCancelledException
import com.dansplugins.factionsystem.exception.WorldClaimBlockedException
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.failure.ServiceFailure
import com.dansplugins.factionsystem.failure.ServiceFailureType
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipType
import com.dansplugins.factionsystem.utils.MfMaterialList
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.onFailure
import dev.forkhandles.result4k.resultFrom
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.ChatMessageType.ACTION_BAR
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Chunk
import org.bukkit.Material
import org.bukkit.World
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.floor

class MfClaimService @JvmOverloads constructor(
    private val plugin: MedievalFactions,
    private val repository: MfClaimedChunkRepository,
    private val regionClaimGuard: MfRegionClaimGuard? = null
) {

    private data class ClaimKey(val worldId: UUID, val x: Int, val z: Int) {
        constructor(claimedChunk: MfClaimedChunk) : this(claimedChunk.worldId, claimedChunk.x, claimedChunk.z)
    }

    private val claimsByKey: MutableMap<ClaimKey, MfClaimedChunk> = ConcurrentHashMap()
    val claims: List<MfClaimedChunk>
        get() = claimsByKey.values.toList()

    init {
        plugin.logger.info("Loading claims...")
        val startTime = System.currentTimeMillis()
        claimsByKey.putAll(repository.getClaims().associateBy { ClaimKey(it.worldId, it.x, it.z) })
        plugin.logger.info("${claimsByKey.size} claims loaded (${System.currentTimeMillis() - startTime}ms)")
    }

    fun getClaim(worldId: UUID, x: Int, z: Int): MfClaimedChunk? = claimsByKey[ClaimKey(worldId, x, z)]
    fun getClaim(world: World, x: Int, z: Int): MfClaimedChunk? = getClaim(world.uid, x, z)
    fun getClaim(chunk: Chunk): MfClaimedChunk? = getClaim(chunk.world, chunk.x, chunk.z)
    fun getClaim(chunkPosition: MfChunkPosition): MfClaimedChunk? = getClaim(chunkPosition.worldId, chunkPosition.x, chunkPosition.z)

    fun isClaimingBlockedInWorld(worldName: String): Boolean {
        val blockedWorlds = plugin.config.getStringList("factions.blockedClaimWorlds")
        return blockedWorlds.contains(worldName)
    }

    fun isClaimingBlockedInWorld(world: World): Boolean {
        return isClaimingBlockedInWorld(world.name)
    }

    /**
     * Whether a new claim on this chunk is refused because the chunk overlaps a WorldGuard region (#1630).
     * Always false unless `worldGuard.blockClaimsInRegions` is true and WorldGuard is installed.
     * Only the player-facing claim paths consult this; existing claims and data migration are unaffected.
     */
    fun isClaimBlockedByWorldGuard(chunk: MfChunkPosition): Boolean {
        if (!plugin.config.getBoolean("worldGuard.blockClaimsInRegions")) return false
        return regionClaimGuard?.overlapsProtectedRegion(chunk) == true
    }

    /**
     * Checks whether a chunk is listed in `factions.blockedClaimChunks`, which holds entries of the form
     * `world:x:z` (one chunk) or `world:x1:z1:x2:z2` (an inclusive rectangle of chunks). Coordinates are chunk
     * coordinates, not block coordinates. Entries that cannot be parsed are ignored.
     */
    fun isChunkClaimBlocked(worldName: String, x: Int, z: Int): Boolean {
        return plugin.config.getStringList("factions.blockedClaimChunks").any { entry ->
            val parts = entry.trim().split(":")
            if (parts.size != 3 && parts.size != 5) return@any false
            if (parts[0] != worldName) return@any false
            val coordinates = parts.drop(1).map { it.trim().toIntOrNull() ?: return@any false }
            if (coordinates.size == 2) {
                coordinates[0] == x && coordinates[1] == z
            } else {
                x in minOf(coordinates[0], coordinates[2])..maxOf(coordinates[0], coordinates[2]) &&
                    z in minOf(coordinates[1], coordinates[3])..maxOf(coordinates[1], coordinates[3])
            }
        }
    }

    fun isChunkClaimBlocked(chunk: MfChunkPosition): Boolean {
        val worldName = plugin.server.getWorld(chunk.worldId)?.name ?: return false
        return isChunkClaimBlocked(worldName, chunk.x, chunk.z)
    }

    /**
     * Checks whether a claim of the given chunks would be further than `factions.maxClaimDistance` chunks from
     * every claim the faction already holds in the same world. A negative value (the default) disables the check,
     * and so does the faction having no claims in that world yet. The claim passes if any one of the chunks is
     * close enough, so a radius claim is judged by its nearest chunk.
     */
    fun exceedsMaxClaimDistance(factionId: MfFactionId, vararg chunks: MfChunkPosition): Boolean {
        val maxDistance = plugin.config.getInt("factions.maxClaimDistance", -1)
        if (maxDistance < 0 || chunks.isEmpty()) return false
        val factionClaims = getClaims(factionId)
        val maxDistanceSquared = maxDistance.toLong() * maxDistance
        return chunks.groupBy { it.worldId }.none { (worldId, worldChunks) ->
            val worldClaims = factionClaims.filter { it.worldId == worldId }
            if (worldClaims.isEmpty()) return@none true
            worldChunks.any { chunk ->
                worldClaims.any { claim ->
                    val dx = (claim.x - chunk.x).toLong()
                    val dz = (claim.z - chunk.z).toLong()
                    dx * dx + dz * dz <= maxDistanceSquared
                }
            }
        }
    }

    /**
     * Checks whether a chunk held by a faction is surrounded on all four sides by that same faction's claims. When
     * `factions.preventConqueringSurroundedChunks` is enabled, such a chunk cannot be conquered until one of its
     * neighbours has been taken, so a war has to work inwards from the border.
     */
    fun isConquestBlockedBySurroundingClaims(chunk: MfChunkPosition, defendingFactionId: MfFactionId): Boolean {
        if (!plugin.config.getBoolean("factions.preventConqueringSurroundedChunks")) return false
        return getClaim(chunk.worldId, chunk.x - 1, chunk.z)?.factionId == defendingFactionId &&
            getClaim(chunk.worldId, chunk.x + 1, chunk.z)?.factionId == defendingFactionId &&
            getClaim(chunk.worldId, chunk.x, chunk.z - 1)?.factionId == defendingFactionId &&
            getClaim(chunk.worldId, chunk.x, chunk.z + 1)?.factionId == defendingFactionId
    }

    /**
     * The power one claimed chunk in the named world counts against a faction's power when `factions.limitLand` is
     * on: the world's entry in `factions.claimPowerCostByWorld` if it has one, otherwise `factions.claimPowerCost`.
     * Both default to 1, which is one power per chunk, as before these options existed. Negative values count as 0.
     */
    fun getClaimPowerCost(worldName: String?): Double {
        val defaultCost = plugin.config.getDouble("factions.claimPowerCost", 1.0)
        val worldCosts = plugin.config.getConfigurationSection("factions.claimPowerCostByWorld")?.getValues(false)
        val worldCost = worldName?.let { worldCosts?.get(it) as? Number }?.toDouble()
        return (worldCost ?: defaultCost).coerceAtLeast(0.0)
    }

    fun getClaimPowerCost(worldId: UUID): Double = getClaimPowerCost(plugin.server.getWorld(worldId)?.name)

    /** The total power the given chunks would count against a faction's power. */
    fun getClaimPowerCost(chunks: Collection<MfChunkPosition>): Double =
        chunks.groupingBy { it.worldId }.eachCount().entries.sumOf { (worldId, count) -> getClaimPowerCost(worldId) * count }

    /**
     * The total number of chunks a faction with the given power could hold if all its further claims were made in the
     * given world: the denominator wherever claims are shown against what the faction can hold (the "you may not
     * claim more than N chunks" message, `/f power` and the Dynmap demesne line). At the default cost of 1 this is the
     * faction's power rounded down, as the message showed before claim costs were configurable.
     */
    fun getClaimLimit(factionId: MfFactionId, power: Double, worldId: UUID?): Double {
        // Without a world (the console, or a map popup for a faction with no land) the default cost is used
        val cost = if (worldId == null) getClaimPowerCost(null as String?) else getClaimPowerCost(worldId)
        if (cost <= 0.0) return floor(power)
        return getClaims(factionId).size + floor((power - getClaimPowerUsed(factionId)) / cost)
    }

    /** The power a faction's existing claims count against its power, which is its claim count at the default cost. */
    fun getClaimPowerUsed(factionId: MfFactionId): Double =
        getClaims(factionId).groupingBy { it.worldId }.eachCount().entries.sumOf { (worldId, count) -> getClaimPowerCost(worldId) * count }

    @JvmName("getClaimsByFactionId")
    fun getClaims(factionId: MfFactionId): List<MfClaimedChunk> = claims.filter { it.factionId == factionId }

    @JvmName("isInteractionAllowedForPlayerInChunk")
    fun isInteractionAllowed(playerId: MfPlayerId, claim: MfClaimedChunk): Boolean {
        val factionService = plugin.services.factionService
        val playerFaction = factionService.getFaction(playerId) ?: return false
        if (claim.factionId == playerFaction.id) return true
        val claimFaction = factionService.getFaction(claim.factionId) ?: return true
        val relationshipService = plugin.services.factionRelationshipService
        val vassals = relationshipService.getVassalTree(claim.factionId)
        if (claimFaction.flags[plugin.flags.vassalageTreeCanInteractWithLand] && vassals.contains(playerFaction.id)) return true
        val lieges = relationshipService.getLiegeChain(claim.factionId)
        if (claimFaction.flags[plugin.flags.liegeChainCanInteractWithLand] && lieges.contains(playerFaction.id)) return true
        val allies = relationshipService.getRelationships(claim.factionId, MfFactionRelationshipType.ALLY).map { it.targetId }
        if (claimFaction.flags[plugin.flags.alliesCanInteractWithLand] && allies.contains(playerFaction.id)) return true
        val atWar = relationshipService.getRelationships(claim.factionId, MfFactionRelationshipType.AT_WAR).map { it.targetId }
        if (plugin.config.getBoolean("pvp.enableWartimeBlockDestruction") && atWar.contains(playerFaction.id)) return true
        return false
    }

    /**
     * Checks if ladder placement is allowed in enemy territory during wartime.
     * This is used to allow players to place ladders for sieges when their faction is at war with the territory owner.
     *
     * @param playerId The ID of the player attempting to place a ladder
     * @param claim The claimed chunk where the player is attempting the action
     * @param isLadder Whether the item being placed is a ladder
     * @return true if ladder placement should be allowed, false otherwise
     */
    fun isWartimeLadderPlacementAllowed(playerId: MfPlayerId, claim: MfClaimedChunk, isLadder: Boolean): Boolean {
        if (!isLadder) return false
        if (!plugin.config.getBoolean("factions.laddersPlaceableInEnemyFactionTerritory")) return false

        val factionService = plugin.services.factionService
        val playerFaction = factionService.getFaction(playerId) ?: return false
        val claimFactionId = claim.factionId

        val relationshipService = plugin.services.factionRelationshipService
        return relationshipService.getFactionsAtWarWith(playerFaction.id).contains(claimFactionId)
    }

    /**
     * Checks if placing a block of the given material is allowed in enemy territory during wartime
     * because it appears in the `factions.wartimePlaceableBlocks` config list.
     *
     * @param playerId The ID of the player attempting to place the block
     * @param claim The claimed chunk where the player is attempting the action
     * @param material The material of the block being placed
     * @return true if placement should be allowed, false otherwise
     */
    fun isWartimePlaceableBlock(playerId: MfPlayerId, claim: MfClaimedChunk, material: Material): Boolean =
        isWartimeBlockActionAllowed(playerId, claim, material, "factions.wartimePlaceableBlocks")

    /**
     * Checks if breaking a block of the given material is allowed in enemy territory during wartime
     * because it appears in the `factions.wartimeBreakableBlocks` config list.
     *
     * @param playerId The ID of the player attempting to break the block
     * @param claim The claimed chunk where the player is attempting the action
     * @param material The material of the block being broken
     * @return true if breaking should be allowed, false otherwise
     */
    fun isWartimeBreakableBlock(playerId: MfPlayerId, claim: MfClaimedChunk, material: Material): Boolean =
        isWartimeBlockActionAllowed(playerId, claim, material, "factions.wartimeBreakableBlocks")

    /**
     * Checks if interacting with a block of the given material is allowed in enemy territory during wartime
     * because it appears in the `factions.wartimeInteractableBlocks` config list.
     *
     * @param playerId The ID of the player attempting to interact with the block
     * @param claim The claimed chunk where the player is attempting the action
     * @param material The material of the block being interacted with
     * @return true if interaction should be allowed, false otherwise
     */
    fun isWartimeInteractableBlock(playerId: MfPlayerId, claim: MfClaimedChunk, material: Material): Boolean =
        isWartimeBlockActionAllowed(playerId, claim, material, "factions.wartimeInteractableBlocks")

    private fun isWartimeBlockActionAllowed(playerId: MfPlayerId, claim: MfClaimedChunk, material: Material, configKey: String): Boolean {
        val blocks = MfMaterialList(plugin.config.getStringList(configKey))
        if (material !in blocks) return false
        val factionService = plugin.services.factionService
        val playerFaction = factionService.getFaction(playerId) ?: return false
        val relationshipService = plugin.services.factionRelationshipService
        return relationshipService.getFactionsAtWarWith(playerFaction.id).contains(claim.factionId)
    }

    // Checks whether a set of chunks has at least one chunk that is adjacent to an existing claim. Works across multiple worlds.
    fun isClaimAdjacent(id: MfFactionId, vararg chunks: MfChunkPosition): Boolean {
        return chunks.any { chunk ->
            getClaim(chunk.worldId, chunk.x - 1, chunk.z)?.factionId == id ||
                getClaim(chunk.worldId, chunk.x + 1, chunk.z)?.factionId == id ||
                getClaim(chunk.worldId, chunk.x, chunk.z - 1)?.factionId == id ||
                getClaim(chunk.worldId, chunk.x, chunk.z + 1)?.factionId == id
        }
    }

    fun save(claim: MfClaimedChunk) = resultFrom {
        val world = plugin.server.getWorld(claim.worldId)
        if (world != null && isClaimingBlockedInWorld(world)) {
            throw WorldClaimBlockedException("Claims are not allowed in this world")
        }
        val factionService = plugin.services.factionService
        val faction = factionService.getFaction(claim.factionId).let(::requireNotNull)
        val event = FactionClaimEvent(claim.factionId, claim, !plugin.server.isPrimaryThread)
        plugin.server.pluginManager.callEvent(event)
        if (event.isCancelled) throw EventCancelledException("Event cancelled")
        val previousOwner = claimsByKey[ClaimKey(event.claim)]?.factionId
        val result = repository.upsert(event.claim)
        claimsByKey[ClaimKey(result)] = result
        if (previousOwner != null && previousOwner != result.factionId) {
            clearHomeIfInLostChunks(previousOwner, listOf(result))
        }
        plugin.server.scheduler.runTask(
            plugin,
            Runnable {
                val world = plugin.server.getWorld(event.claim.worldId)
                if (world != null) {
                    val players = world.players.filter { it.location.chunk.x == claim.x && it.location.chunk.z == claim.z }
                    if (players.isNotEmpty()) {
                        plugin.server.scheduler.runTask(
                            plugin,
                            Runnable {
                                players.forEach { player ->
                                    val title = "${ChatColor.of(faction.flags[plugin.flags.color])}${faction.name}"
                                    val subtitle = "${ChatColor.of(faction.flags[plugin.flags.color])}${faction.description}"
                                    if (plugin.config.getBoolean("factions.titleTerritoryIndicator")) {
                                        player.resetTitle()
                                        player.sendTitle(
                                            title,
                                            subtitle,
                                            plugin.config.getInt("factions.titleTerritoryFadeInLength"),
                                            plugin.config.getInt("factions.titleTerritoryDuration"),
                                            plugin.config.getInt("factions.titleTerritoryFadeOutLength")
                                        )
                                    }
                                    if (plugin.config.getBoolean("factions.actionBarTerritoryIndicator")) {
                                        player.spigot().sendMessage(ACTION_BAR, *TextComponent.fromLegacyText(title))
                                    }
                                }
                            }
                        )
                    }
                }
            }
        )
        val mapService = plugin.services.mapService
        if (mapService != null && !plugin.config.getBoolean("dynmap.onlyRenderTerritoriesUponStartup")) {
            plugin.server.scheduler.runTask(
                plugin,
                Runnable {
                    mapService.scheduleUpdateClaims(faction)
                }
            )
        }
        return@resultFrom result
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }

    fun delete(claim: MfClaimedChunk) = resultFrom {
        val event = FactionUnclaimEvent(claim.factionId, claim, !plugin.server.isPrimaryThread)
        plugin.server.pluginManager.callEvent(event)
        if (event.isCancelled) throw EventCancelledException("Event cancelled")
        val result = repository.delete(event.claim.worldId, event.claim.x, event.claim.z)
        claimsByKey.remove(ClaimKey(event.claim))
        clearHomeIfInLostChunks(event.claim.factionId, listOf(event.claim))
        plugin.server.scheduler.runTask(
            plugin,
            Runnable {
                val world = plugin.server.getWorld(event.claim.worldId)
                if (world != null) {
                    val players = world.players.filter { it.location.chunk.x == claim.x && it.location.chunk.z == claim.z }
                    if (players.isNotEmpty()) {
                        players.forEach { player ->
                            val title =
                                "${ChatColor.of(plugin.config.getString("wilderness.color"))}${plugin.language["Wilderness"]}"
                            if (plugin.config.getBoolean("factions.titleTerritoryIndicator")) {
                                player.resetTitle()
                                player.sendTitle(
                                    title,
                                    null,
                                    plugin.config.getInt("factions.titleTerritoryFadeInLength"),
                                    plugin.config.getInt("factions.titleTerritoryDuration"),
                                    plugin.config.getInt("factions.titleTerritoryFadeOutLength")
                                )
                            }
                            if (plugin.config.getBoolean("factions.actionBarTerritoryIndicator")) {
                                player.spigot().sendMessage(ACTION_BAR, *TextComponent.fromLegacyText(title))
                            }
                        }
                    }
                }
            }
        )
        val mapService = plugin.services.mapService
        if (mapService != null) {
            val factionService = plugin.services.factionService
            val faction = factionService.getFaction(claim.factionId)
            if (faction != null && !plugin.config.getBoolean("dynmap.onlyRenderTerritoriesUponStartup")) {
                plugin.server.scheduler.runTask(
                    plugin,
                    Runnable {
                        mapService.scheduleUpdateClaims(faction)
                    }
                )
            }
        }
        val lockService = plugin.services.lockService
        lockService.unloadLockedBlocks(claim)
        return@resultFrom result
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }

    @JvmName("deleteAllClaimsByFactionId")
    fun deleteAllClaims(factionId: MfFactionId) = deleteAllClaims(factionId, clearHome = true)

    /**
     * [clearHome] is false only when the faction itself is being disbanded: its home goes with it, so there is nothing
     * to clear and no member to tell.
     */
    internal fun deleteAllClaims(factionId: MfFactionId, clearHome: Boolean) = resultFrom {
        val result = repository.deleteAll(factionId)
        val claimsToDelete = claimsByKey.filterValues { it.factionId == factionId }
        claimsToDelete.forEach { (key, value) ->
            claimsByKey.remove(key, value)
        }
        if (clearHome) clearHomeIfInLostChunks(factionId, claimsToDelete.values)
        return@resultFrom result
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }

    /**
     * With `factions.clearHomeOnChunkLoss` on, removes the home of [factionId] when it stands in one of [lostChunks],
     * chunks that faction no longer holds (unclaimed, unclaimed in bulk, or taken by another faction). The claim change
     * has already been stored, so a failure here is logged and does not fail it.
     */
    private fun clearHomeIfInLostChunks(factionId: MfFactionId, lostChunks: Collection<MfClaimedChunk>) {
        if (lostChunks.isEmpty() || !plugin.config.getBoolean("factions.clearHomeOnChunkLoss", false)) return
        val factionService = plugin.services.factionService
        val home = factionService.getFaction(factionId)?.home ?: return
        if (lostChunks.none { it.contains(home) }) return
        val updated = factionService.update(factionId) { faction ->
            if (faction.home != null && lostChunks.any { it.contains(faction.home) }) faction.copy(home = null) else faction
        }.onFailure {
            plugin.logger.warning("Failed to clear the home of faction ${factionId.value} after it lost the chunk: ${it.reason.message}")
            return
        }
        updated.sendMessage(
            plugin.language["FactionHomeClearedOnChunkLossNotificationTitle"],
            plugin.language["FactionHomeClearedOnChunkLossNotificationBody"]
        )
    }

    private fun MfClaimedChunk.contains(position: MfPosition) =
        worldId == position.worldId && x == floor(position.x / 16).toInt() && z == floor(position.z / 16).toInt()

    private fun Exception.toServiceFailureType(): ServiceFailureType {
        return when (this) {
            is OptimisticLockingFailureException -> ServiceFailureType.CONFLICT
            else -> ServiceFailureType.GENERAL
        }
    }
}
