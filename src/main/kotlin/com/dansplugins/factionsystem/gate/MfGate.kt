package com.dansplugins.factionsystem.gate

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.area.MfCuboidArea
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.gate.MfGateStatus.CLOSED
import dev.forkhandles.result4k.onFailure
import org.bukkit.Material
import org.bukkit.Sound
import java.util.logging.Level

data class MfGate(
    private val plugin: MedievalFactions,
    @get:JvmName("getId")
    val id: MfGateId = MfGateId.generate(),
    val version: Int = 0,
    @get:JvmName("getFactionId")
    val factionId: MfFactionId,
    val area: MfCuboidArea,
    val trigger: MfBlockPosition,
    val material: Material,
    val status: MfGateStatus = CLOSED
) {

    fun shouldOpen(): Boolean {
        val world = plugin.server.getWorld(trigger.worldId) ?: return false
        if (!world.isChunkLoaded(trigger.x / 16, trigger.z / 16)) return false
        val triggerBukkitBlock = trigger.toBukkitBlock() ?: return false
        return triggerBukkitBlock.isBlockIndirectlyPowered || triggerBukkitBlock.isBlockPowered
    }

    fun shouldClose(): Boolean {
        val world = plugin.server.getWorld(trigger.worldId) ?: return false
        if (!world.isChunkLoaded(trigger.x / 16, trigger.z / 16)) return false
        val triggerBukkitBlock = trigger.toBukkitBlock() ?: return false
        return !triggerBukkitBlock.isBlockIndirectlyPowered && !triggerBukkitBlock.isBlockPowered
    }

    fun open() {
        if (status == MfGateStatus.OPENING || status == MfGateStatus.OPEN) return
        val gateService = plugin.services.gateService
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                gateService.save(copy(status = MfGateStatus.OPENING)).onFailure {
                    plugin.logger.log(Level.SEVERE, "Failed to save gate: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
            }
        )
    }

    fun continueOpening() {
        val gateService = plugin.services.gateService
        val centerBukkitBlock = area.centerPosition.toBukkitBlock()
        if (centerBukkitBlock != null) {
            centerBukkitBlock.world.playSound(centerBukkitBlock.location, Sound.BLOCK_ANVIL_HIT, 1f, 0f)
        }
        val lowestBlocks = area.blocks.filter { it.toBukkitBlock()?.type == material }
            .groupBy { it.y }
            .minByOrNull { it.key }
            ?.value
        lowestBlocks?.map { it.toBukkitBlock() }?.forEach { it?.type = Material.AIR }
        if (area.blocks.all { it.toBukkitBlock()?.type == Material.AIR }) {
            plugin.server.scheduler.runTaskAsynchronously(
                plugin,
                Runnable {
                    gateService.save(copy(status = MfGateStatus.OPEN)).onFailure {
                        plugin.logger.log(Level.SEVERE, "Failed to save gate: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                }
            )
        }
    }

    fun close() {
        if (status == MfGateStatus.CLOSING || status == CLOSED) return
        val gateService = plugin.services.gateService
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                gateService.save(copy(status = MfGateStatus.CLOSING)).onFailure {
                    plugin.logger.log(Level.SEVERE, "Failed to save gate: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
            }
        )
        val centerBukkitBlock = area.centerPosition.toBukkitBlock()
        if (centerBukkitBlock != null) {
            centerBukkitBlock.world.playSound(centerBukkitBlock.location, Sound.BLOCK_ANVIL_HIT, 1f, 0f)
        }
    }

    fun continueClosing() {
        val gateService = plugin.services.gateService
        val centerBukkitBlock = area.centerPosition.toBukkitBlock()
        if (centerBukkitBlock != null) {
            centerBukkitBlock.world.playSound(centerBukkitBlock.location, Sound.BLOCK_ANVIL_HIT, 1f, 0f)
        }
        val highestEmptyBlocks = area.blocks.filter { it.toBukkitBlock()?.type != material }
            .groupBy { it.y }
            .maxByOrNull { it.key }
            ?.value
        highestEmptyBlocks?.map { it.toBukkitBlock() }?.forEach { it?.type = material }
        if (area.blocks.all { it.toBukkitBlock()?.type == material }) {
            plugin.server.scheduler.runTaskAsynchronously(
                plugin,
                Runnable {
                    gateService.save(copy(status = CLOSED)).onFailure {
                        plugin.logger.log(Level.SEVERE, "Failed to save gate: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                }
            )
        }
    }
}
