package com.dansplugins.factionsystem.faction

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfPosition
import com.dansplugins.factionsystem.event.faction.FactionCreateEvent
import com.dansplugins.factionsystem.event.faction.FactionDescriptionChangeEvent
import com.dansplugins.factionsystem.event.faction.FactionDisbandEvent
import com.dansplugins.factionsystem.event.faction.FactionJoinEvent
import com.dansplugins.factionsystem.event.faction.FactionLeaveEvent
import com.dansplugins.factionsystem.event.faction.FactionPrefixChangeEvent
import com.dansplugins.factionsystem.event.faction.FactionRenameEvent
import com.dansplugins.factionsystem.exception.EventCancelledException
import com.dansplugins.factionsystem.faction.field.MfFactionField
import com.dansplugins.factionsystem.faction.flag.MfFlagValues
import com.dansplugins.factionsystem.faction.role.MfFactionRoles
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.failure.ServiceFailure
import com.dansplugins.factionsystem.failure.ServiceFailureType
import com.dansplugins.factionsystem.failure.ServiceFailureType.CONFLICT
import com.dansplugins.factionsystem.failure.ServiceFailureType.GENERAL
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.onFailure
import dev.forkhandles.result4k.resultFrom
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.logging.Level.SEVERE

class MfFactionService(private val plugin: MedievalFactions, private val repository: MfFactionRepository) {

    private val factionsById: MutableMap<MfFactionId, MfFaction> = ConcurrentHashMap()
    val factions: List<MfFaction>
        get() = factionsById.values.toList()

    private val _fields: MutableList<MfFactionField> = CopyOnWriteArrayList()
    val fields: List<MfFactionField>
        get() = _fields

    init {
        plugin.logger.info("Loading factions...")
        var startTime = System.currentTimeMillis()
        factionsById.putAll(repository.getFactions().associateBy(MfFaction::id))
        plugin.logger.info("${factionsById.size} factions loaded (${System.currentTimeMillis() - startTime}ms)")
        if (!plugin.config.getBoolean("factions.allowNeutrality")) {
            plugin.logger.info("Disabling neutrality for existing factions due to config setting...")
            startTime = System.currentTimeMillis()
            // Writes through the repository because save() needs plugin.services, which is assigned after this service
            // is built. Only a flag changes here, so save() would raise no event, and territories render during startup.
            val updatedFactions = factions.filter { it.flags[plugin.flags.isNeutral] }.map { faction ->
                repository.upsert(faction.copy(flags = faction.flags + (plugin.flags.isNeutral to false)))
            }.associateBy(MfFaction::id)
            if (updatedFactions.isNotEmpty()) {
                factionsById.putAll(updatedFactions)
                plugin.logger.info("Updated neutrality setting for ${updatedFactions.size} factions (${System.currentTimeMillis() - startTime}ms)")
            } else {
                plugin.logger.info("No factions required updating.")
            }
        }
    }

    fun getFaction(name: String): MfFaction? = factions.singleOrNull { it.name == name }

    @JvmName("getFactionByPlayerId")
    fun getFaction(playerId: MfPlayerId): MfFaction? = factions.singleOrNull { faction ->
        faction.members.any { member -> member.playerId == playerId }
    }

    @JvmName("getFactionByFactionId")
    fun getFaction(factionId: MfFactionId): MfFaction? = factionsById[factionId]

    fun save(faction: MfFaction): Result4k<MfFaction, ServiceFailure> = resultFrom {
        val previousState = getFaction(faction.id)
        var factionToSave = faction
        var leavingMembers = emptyList<MfPlayerId>()
        if (previousState == null) {
            val event = FactionCreateEvent(faction.id, faction, !plugin.server.isPrimaryThread)
            plugin.server.pluginManager.callEvent(event)
            if (event.isCancelled) {
                throw EventCancelledException("Event cancelled")
            }
            factionToSave = event.faction
        } else {
            if (previousState.name != faction.name) {
                val event = FactionRenameEvent(faction.id, faction.name, !plugin.server.isPrimaryThread)
                plugin.server.pluginManager.callEvent(event)
                if (event.isCancelled) {
                    throw EventCancelledException("Event cancelled")
                }
            }
            if (previousState.description != faction.description) {
                val event = FactionDescriptionChangeEvent(faction.id, faction.description, !plugin.server.isPrimaryThread)
                plugin.server.pluginManager.callEvent(event)
                if (event.isCancelled) {
                    throw EventCancelledException("Event cancelled")
                }
            }
            if (previousState.prefix != faction.prefix) {
                val event = FactionPrefixChangeEvent(faction.id, faction.prefix, !plugin.server.isPrimaryThread)
                plugin.server.pluginManager.callEvent(event)
                if (event.isCancelled) {
                    throw EventCancelledException("Event cancelled")
                }
            }
            val newMembers = faction.members.map(MfFactionMember::playerId) - previousState.members.map(MfFactionMember::playerId)
            newMembers.forEach { newMember ->
                val event = FactionJoinEvent(faction.id, newMember, !plugin.server.isPrimaryThread)
                plugin.server.pluginManager.callEvent(event)
                if (event.isCancelled) {
                    throw EventCancelledException("Event cancelled")
                }
            }
            leavingMembers = previousState.members.map(MfFactionMember::playerId) - faction.members.map(MfFactionMember::playerId)
            leavingMembers.forEach { leavingMember ->
                val event = FactionLeaveEvent(faction.id, leavingMember, !plugin.server.isPrimaryThread)
                plugin.server.pluginManager.callEvent(event)
                if (event.isCancelled) {
                    throw EventCancelledException("Event cancelled")
                }
            }
        }
        val result = repository.upsert(factionToSave)
        factionsById[result.id] = result
        leavingMembers.forEach(::deleteLocksOfFormerMember)
        val mapService = plugin.services.mapService
        if (mapService != null && !plugin.config.getBoolean("dynmap.onlyRenderTerritoriesUponStartup")) {
            plugin.server.scheduler.runTask(
                plugin,
                Runnable {
                    mapService.scheduleUpdateClaims(result)
                }
            )
        }
        return@resultFrom result
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }

    /**
     * Runs after the faction is stored, so a failure here is logged and not reported as a failed save: the player has
     * already left, and a caller told otherwise would treat them as still a member.
     */
    private fun deleteLocksOfFormerMember(playerId: MfPlayerId) {
        val lockService = plugin.services.lockService
        lockService.getLockedBlocks(playerId).forEach { lockedBlock ->
            lockService.delete(lockedBlock.block).onFailure { failure ->
                plugin.logger.log(
                    SEVERE,
                    "Failed to delete lock at ${lockedBlock.block} of former member ${playerId.value}: ${failure.reason.message}",
                    failure.reason.cause
                )
                return@forEach
            }
        }
    }

    @JvmName("deleteFactionByFactionId")
    fun delete(factionId: MfFactionId): Result4k<Unit, ServiceFailure> = resultFrom {
        val event = FactionDisbandEvent(factionId, !plugin.server.isPrimaryThread)
        plugin.server.pluginManager.callEvent(event)
        if (event.isCancelled) {
            throw EventCancelledException("Event cancelled")
        }
        val claimService = plugin.services.claimService
        claimService.deleteAllClaims(factionId).onFailure {
            throw it.reason.cause
        }
        val gateService = plugin.services.gateService
        gateService.deleteAllGates(factionId).onFailure {
            throw it.reason.cause
        }
        val result = repository.delete(factionId)
        factionsById.remove(factionId)
        return@resultFrom result
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }

    @JvmOverloads
    fun createFaction(
        name: String,
        id: String = MfFactionId.generate().value,
        description: String = "",
        members: List<MfFactionMember> = emptyList(),
        invites: List<MfFactionInvite> = emptyList(),
        flags: MfFlagValues = plugin.flags.defaults(),
        prefix: String? = null,
        home: MfPosition? = null,
        bonusPower: Double = 0.0,
        autoclaim: Boolean = false,
        roles: MfFactionRoles = MfFactionRoles.defaults(plugin, MfFactionId(id))
    ): Result4k<MfFaction, ServiceFailure> {
        return save(
            MfFaction(
                plugin = plugin,
                id = MfFactionId(id),
                name = name,
                description = description,
                members = members,
                invites = invites,
                flags = flags,
                prefix = prefix,
                home = home,
                bonusPower = bonusPower,
                autoclaim = autoclaim,
                roles = roles
            )
        )
    }

    fun addField(field: MfFactionField) {
        _fields.add(field)
    }

    private fun Exception.toServiceFailureType(): ServiceFailureType {
        return when (this) {
            is OptimisticLockingFailureException -> CONFLICT
            else -> GENERAL
        }
    }

    fun cancelAllApplicationsForPlayer(player: MfPlayer) {
        plugin.logger.info("Cancelling all applications for player ${player.name}")
        factions.forEach { faction ->
            plugin.logger.info("Checking faction ${faction.name}")
            save(
                faction.copy(
                    applications = faction.applications.filter { it.applicantId != player.id }
                )
            ).onFailure {
                plugin.logger.warning("Failed to cancel applications for player ${player.name} in faction ${faction.name}: ${it.reason.message}")
                throw it.reason.cause
            }
        }
    }
}
