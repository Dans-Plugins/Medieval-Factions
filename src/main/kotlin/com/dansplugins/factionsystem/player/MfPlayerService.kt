package com.dansplugins.factionsystem.player

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.failure.ServiceFailure
import com.dansplugins.factionsystem.failure.ServiceFailureType
import com.dansplugins.factionsystem.failure.ServiceFailureType.CONFLICT
import com.dansplugins.factionsystem.failure.ServiceFailureType.GENERAL
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.onFailure
import dev.forkhandles.result4k.resultFrom
import org.bukkit.OfflinePlayer
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level.SEVERE
import kotlin.collections.set

class MfPlayerService(private val plugin: MedievalFactions, private val playerRepository: MfPlayerRepository) {

    private val playersById: MutableMap<MfPlayerId, MfPlayer> = ConcurrentHashMap()

    init {
        plugin.logger.info("Loading players...")
        val startTime = System.currentTimeMillis()
        playersById.putAll(playerRepository.getPlayers().associateBy(MfPlayer::id))
        val playersToUpdate = playersById.values
            .associateWith(MfPlayer::toBukkit)
            .mapNotNull { (mfPlayer, bukkitPlayer) ->
                try {
                    val bukkitPlayerName = bukkitPlayer.name
                    if (bukkitPlayerName != null && bukkitPlayerName != mfPlayer.name) {
                        mfPlayer to bukkitPlayerName
                    } else {
                        null
                    }
                } catch (e: NoSuchElementException) {
                    plugin.logger.warning("Failed to get name for player ${mfPlayer.id.value}: ${e.message}")
                    null
                }
            }
        playersToUpdate.forEach { (mfPlayer, bukkitPlayerName) ->
            val updatedPlayer = resultFrom {
                playerRepository.upsert(mfPlayer.copy(name = bukkitPlayerName))
            }.onFailure {
                plugin.logger.log(SEVERE, "Failed to update player: ${it.reason.message}", it.reason.cause)
                return@forEach
            }
            playersById[updatedPlayer.id] = updatedPlayer
        }
        plugin.logger.info("${playersById.size} players loaded (${System.currentTimeMillis() - startTime}ms)")
    }

    @JvmName("getPlayerByPlayerId")
    fun getPlayer(id: MfPlayerId): MfPlayer? {
        return playersById[id]
    }

    @JvmName("getPlayerByBukkitPlayer")
    fun getPlayer(player: OfflinePlayer): MfPlayer? = getPlayer(MfPlayerId(player.uniqueId.toString()))

    /**
     * Saves [player], which must carry the version it was read at.
     *
     * A write from a stale snapshot fails with [CONFLICT] instead of overwriting the newer row.
     * On a conflict the cached entry is refreshed from storage, so the next read sees the
     * current row. Callers that change an existing player should prefer [update], which
     * re-applies their change to the current row when a conflict occurs.
     *
     * A brand-new default player (version 0, nothing set beyond what a new player starts
     * with) that loses a race against another create is not a conflict: the row that was
     * created first is kept and returned, rather than being reset to the starting values.
     */
    fun save(player: MfPlayer): Result4k<MfPlayer, ServiceFailure> = save(player, keepStoredOnDefaultCreate = true)

    private fun save(player: MfPlayer, keepStoredOnDefaultCreate: Boolean): Result4k<MfPlayer, ServiceFailure> = resultFrom {
        val result = try {
            playerRepository.upsert(player)
        } catch (exception: OptimisticLockingFailureException) {
            val stored = playerRepository.getPlayer(player.id)
            if (stored != null) playersById[stored.id] = stored
            if (stored == null || !keepStoredOnDefaultCreate || !isUnsavedDefault(player)) throw exception
            plugin.logger.fine("Player ${player.id.value} was created concurrently; keeping the stored record")
            stored
        }
        playersById[result.id] = result
        val mapService = plugin.services.mapService
        if (mapService != null) {
            val factionService = plugin.services.factionService
            val faction = factionService.getFaction(result.id)
            if (faction != null && !plugin.config.getBoolean("dynmap.onlyRenderTerritoriesUponStartup")) {
                plugin.server.scheduler.runTask(
                    plugin,
                    Runnable {
                        mapService.scheduleUpdateClaims(faction)
                    }
                )
            }
        }
        return@resultFrom result
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }

    /**
     * Applies [transform] to [player] and saves the result. If the save conflicts because the
     * stored row changed since [player] was read (for example, the scheduled power task ran in
     * between), the current row is re-read from storage and [transform] is applied to it
     * again, up to [MAX_UPDATE_ATTEMPTS] times. [transform] should therefore express the
     * change relative to the player it is given (e.g. `power - lost`), not a value computed
     * from an older snapshot.
     */
    fun update(player: MfPlayer, transform: (MfPlayer) -> MfPlayer): Result4k<MfPlayer, ServiceFailure> {
        var base = player
        var attempt = 1
        while (true) {
            // A change applied through update is never absorbed as a duplicate create: it
            // conflicts and is re-applied to the stored row instead.
            val result = save(transform(base), keepStoredOnDefaultCreate = false)
            if (result !is Failure || result.reason.type != CONFLICT || attempt >= MAX_UPDATE_ATTEMPTS) return result
            base = try {
                playerRepository.getPlayer(player.id) ?: player.copy(version = 0)
            } catch (exception: Exception) {
                return result
            }
            attempt++
        }
    }

    private fun isUnsavedDefault(player: MfPlayer): Boolean {
        val initialPower = plugin.config.getDouble("players.initialPower")
        return player.version == 0 &&
            player.power == initialPower &&
            player.powerAtLogout == initialPower &&
            !player.isBypassEnabled &&
            player.chatChannel == null
    }

    @JvmName("updatePlayerPower")
    fun updatePlayerPower(onlinePlayerIds: List<MfPlayerId>): Result4k<Unit, ServiceFailure> {
        return resultFrom {
            playerRepository.increaseOnlinePlayerPower(onlinePlayerIds)
            playerRepository.decreaseOfflinePlayerPower(onlinePlayerIds)
            playersById.putAll(playerRepository.getPlayers().associateBy(MfPlayer::id))
            val mapService = plugin.services.mapService
            if (mapService != null && !plugin.config.getBoolean("dynmap.onlyRenderTerritoriesUponStartup")) {
                val factionService = plugin.services.factionService
                factionService.factions.forEach { faction ->
                    plugin.server.scheduler.runTask(
                        plugin,
                        Runnable {
                            mapService.scheduleUpdateClaims(faction)
                        }
                    )
                }
            }
        }.mapFailure { exception ->
            ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
        }
    }

    companion object {
        const val MAX_UPDATE_ATTEMPTS = 3
    }

    private fun Exception.toServiceFailureType(): ServiceFailureType {
        return when (this) {
            is OptimisticLockingFailureException -> CONFLICT
            else -> GENERAL
        }
    }
}
