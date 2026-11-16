package com.dansplugins.factionsystem.lockedarea

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.failure.ServiceFailure
import com.dansplugins.factionsystem.failure.ServiceFailureType
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.resultFrom
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap

/** Locked areas (#1393), cached in memory so the protection checks on every block action stay cheap. */
class MfLockedAreaService(private val plugin: MedievalFactions, private val repository: MfLockedAreaRepository) {

    private val lockedAreas: MutableMap<String, MfLockedArea> = ConcurrentHashMap()

    /** Each player's selected corners, kept only in memory until they create an area. */
    private val selections: MutableMap<String, Pair<MfBlockPosition?, MfBlockPosition?>> = ConcurrentHashMap()

    init {
        lockedAreas.putAll(repository.getLockedAreas().associateBy { it.id.value })
        plugin.logger.info("${lockedAreas.size} locked areas loaded")
    }

    @JvmName("getLockedAreasByFactionId")
    fun getLockedAreas(factionId: MfFactionId): List<MfLockedArea> =
        lockedAreas.values.filter { it.factionId.value == factionId.value }.sortedBy { it.name.lowercase() }

    @JvmName("getLockedAreaByFactionIdAndName")
    fun getLockedArea(factionId: MfFactionId, name: String): MfLockedArea? =
        getLockedAreas(factionId).singleOrNull { it.name.equals(name, ignoreCase = true) }

    fun save(lockedArea: MfLockedArea): Result4k<MfLockedArea, ServiceFailure> = resultFrom {
        repository.upsert(lockedArea).also { lockedAreas[it.id.value] = it }
    }.mapFailure { ServiceFailure(it.toServiceFailureType(), "Service error: ${it.message}", it) }

    @JvmName("deleteLockedAreaById")
    fun delete(id: MfLockedAreaId): Result4k<Unit, ServiceFailure> = resultFrom {
        repository.delete(id)
        lockedAreas.remove(id.value)
        Unit
    }.mapFailure { ServiceFailure(it.toServiceFailureType(), "Service error: ${it.message}", it) }

    @JvmName("selectPosition")
    fun select(playerId: MfPlayerId, corner: Int, position: MfBlockPosition) {
        selections.compute(playerId.value) { _, current ->
            val (first, second) = current ?: (null to null)
            if (corner == 1) position to second else first to position
        }
    }

    @JvmName("getSelection")
    fun getSelection(playerId: MfPlayerId): Pair<MfBlockPosition?, MfBlockPosition?> = selections[playerId.value] ?: (null to null)

    @JvmName("clearSelection")
    fun clearSelection(playerId: MfPlayerId) {
        selections.remove(playerId.value)
    }

    /**
     * The locked area that refuses [mfPlayer] building or interacting at [position] in [claim], or null
     * when none does. Only areas of the claim's own faction count, so an area whose land was unclaimed
     * protects nothing. A member whose role has MANAGE_LOCKED_AREAS, and a player with `/f bypass` on,
     * are never refused.
     */
    fun getRefusingArea(mfPlayer: MfPlayer, player: Player, claim: MfClaimedChunk, position: MfBlockPosition): MfLockedArea? {
        val areas = lockedAreas.values.filter { it.factionId.value == claim.factionId.value && position in it }
        if (areas.isEmpty()) return null
        if (mfPlayer.isBypassEnabled && player.hasPermission("mf.bypass")) return null
        val faction = plugin.services.factionService.getFaction(claim.factionId) ?: return null
        val role = faction.getRole(mfPlayer.id)
        if (role != null && role.hasPermission(faction, plugin.factionPermissions.manageLockedAreas)) return null
        return areas.firstOrNull { !it.allows(mfPlayer.id, role?.id) }
    }

    private fun Exception.toServiceFailureType(): ServiceFailureType = when (this) {
        is OptimisticLockingFailureException -> ServiceFailureType.CONFLICT
        else -> ServiceFailureType.GENERAL
    }
}
