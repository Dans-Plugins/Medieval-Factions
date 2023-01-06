package com.dansplugins.factionsystem.locks

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.failure.ServiceFailure
import com.dansplugins.factionsystem.failure.ServiceFailureType
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.resultFrom
import org.bukkit.block.Chest
import org.bukkit.block.DoubleChest
import org.bukkit.block.data.Bisected
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.block.data.Bisected.Half.BOTTOM
import org.bukkit.block.BlockFace.DOWN
import org.bukkit.block.BlockFace.UP
import org.bukkit.block.Block
import com.dansplugins.factionsystem.interaction.MfInteractionStatus.*
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.RED
import java.util.logging.Level.SEVERE

class MfLockService(private val plugin: MedievalFactions, private val repository: MfLockRepository) {

    private val lockedBlocks: MutableMap<MfBlockPosition, MfLockedBlock> = ConcurrentHashMap()

    init {
        plugin.logger.info("Loading locked blocks...")
        val startTime = System.currentTimeMillis()
        lockedBlocks.putAll(repository.getLockedBlocks().map { it.block to it })
        plugin.logger.info("${lockedBlocks.size} locked blocks loaded (${System.currentTimeMillis() - startTime}ms)")
    }

    fun lock(block: MfBlockPosition, claim: MfClaimedChunk, player: MfPlayer): Result4k<MfLockedBlock, ServiceFailure> = resultFrom {
        val lockedBlock = repository.upsert(MfLockedBlock(
            block = MfBlockPosition(
                worldId = block.worldId,
                x = block.x,
                y = block.y,
                z = block.z
            ),
            chunkX = claim.x,
            chunkZ = claim.z,
            playerId = player.id,
            accessors = emptyList()
        ))
        lockedBlocks[block] = lockedBlock
        return@resultFrom lockedBlock
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }

    public fun unlock(player: Player, block: Block) {
        val blockData = block.blockData
        val holder = (block.state as? Chest)?.inventory?.holder
        val blocks = if (blockData is Bisected) {
            if (blockData.half == BOTTOM) {
                listOf(block, block.getRelative(UP))
            } else {
                listOf(block, block.getRelative(DOWN))
            }
        } else if (holder is DoubleChest) {
            val left = holder.leftSide as? Chest
            val right = holder.rightSide as? Chest
            listOfNotNull(left?.block, right?.block)
        } else {
            listOf(block)
        }
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val playerService = plugin.services.playerService
            val mfPlayer = playerService.getPlayer(player) ?: playerService.save(MfPlayer(plugin, player)).onFailure {
                player.sendMessage("$RED${plugin.language["BlockUnlockFailedToSavePlayer"]}")
                plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                return@Runnable
            }
            val lockedBlocks = blocks.mapNotNull { getLockedBlock(MfBlockPosition.fromBukkitBlock(it)) }
            val lockedBlock = lockedBlocks.firstOrNull()
            if (lockedBlock == null) {
                player.sendMessage("$RED${plugin.language["BlockUnlockNotLocked"]}")
                return@Runnable
            }
            if (lockedBlock.playerId.value != mfPlayer.id.value) {
                val lockOwner = playerService.getPlayer(lockedBlock.playerId)
                val ownerName = if (lockOwner == null) {
                    plugin.language["UnknownPlayer"]
                } else {
                    lockOwner.toBukkit().name ?: plugin.language["UnknownPlayer"]
                }
                if (!player.hasPermission("mf.force.unlock")) {
                    player.sendMessage("$RED${plugin.language["BlockUnlockOwnedByOtherPlayer", ownerName]}")
                    return@Runnable
                } else {
                    player.sendMessage("$RED${plugin.language["BlockUnlockProtectionBypassed", ownerName]}")
                }
            }
            delete(lockedBlock.block).onFailure {
                player.sendMessage("$RED${plugin.language["BlockUnlockFailedToDeleteBlock"]}")
                plugin.logger.log(SEVERE, "Failed to delete block: ${it.reason.message}", it.reason.cause)
                return@Runnable
            }
            val interactionService = plugin.services.interactionService
            interactionService.setInteractionStatus(mfPlayer.id, null).onFailure {
                player.sendMessage("$RED${plugin.language["BlockUnlockFailedToSetInteractionStatus"]}")
                plugin.logger.log(SEVERE, "Failed to save interaction status: ${it.reason.message}", it.reason.cause)
                return@Runnable
            }
            player.sendMessage("$RED${plugin.language["BlockUnlockSuccessful"]}")
        })
    }

    fun save(lockedBlock: MfLockedBlock): Result4k<MfLockedBlock, ServiceFailure> = resultFrom {
        val upsertedLockedBlock = repository.upsert(lockedBlock)
        lockedBlocks.remove(lockedBlock.block)
        lockedBlocks[upsertedLockedBlock.block] = upsertedLockedBlock
        return@resultFrom upsertedLockedBlock
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }

    fun delete(block: MfBlockPosition): Result4k<Unit, ServiceFailure> = resultFrom {
        repository.delete(block)
        lockedBlocks.remove(block)
        return@resultFrom
    }.mapFailure { exception ->
        ServiceFailure(exception.toServiceFailureType(), "Service error: ${exception.message}", exception)
    }

    fun getLockedBlock(block: MfBlockPosition): MfLockedBlock? {
        return lockedBlocks[block]
    }

    @JvmName("getLockedBlockByLockedBlockId")
    fun getLockedBlock(id: MfLockedBlockId): MfLockedBlock? {
        return lockedBlocks.values.singleOrNull { it.id == id }
    }

    @JvmName("getLockedBlocksByPlayerId")
    fun getLockedBlocks(playerId: MfPlayerId): List<MfLockedBlock> {
        return lockedBlocks.values.filter { it.playerId == playerId }
    }

    @JvmName("getLockedBlocksByClaim")
    fun getLockedBlocks(claim: MfClaimedChunk): List<MfLockedBlock> {
        return lockedBlocks.values.filter {
            it.block.worldId == claim.worldId
                    && it.chunkX == claim.x
                    && it.chunkZ == claim.z
        }
    }

    internal fun unloadLockedBlocks(claim: MfClaimedChunk) {
        getLockedBlocks(claim).forEach { lockedBlocks.remove(it.block) }
    }

    private fun Exception.toServiceFailureType(): ServiceFailureType {
        return when (this) {
            is OptimisticLockingFailureException -> ServiceFailureType.CONFLICT
            else -> ServiceFailureType.GENERAL
        }
    }

}