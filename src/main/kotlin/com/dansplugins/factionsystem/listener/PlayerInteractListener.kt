package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.area.MfCuboidArea
import com.dansplugins.factionsystem.gate.MfGate
import com.dansplugins.factionsystem.gate.MfGateCreationContext
import com.dansplugins.factionsystem.interaction.MfInteractionStatus.ADDING_ACCESSOR
import com.dansplugins.factionsystem.interaction.MfInteractionStatus.CHECKING_ACCESS
import com.dansplugins.factionsystem.interaction.MfInteractionStatus.LOCKING
import com.dansplugins.factionsystem.interaction.MfInteractionStatus.REMOVING_ACCESSOR
import com.dansplugins.factionsystem.interaction.MfInteractionStatus.SELECTING_GATE_POSITION_1
import com.dansplugins.factionsystem.interaction.MfInteractionStatus.SELECTING_GATE_POSITION_2
import com.dansplugins.factionsystem.interaction.MfInteractionStatus.SELECTING_GATE_TRIGGER
import com.dansplugins.factionsystem.interaction.MfInteractionStatus.UNLOCKING
import com.dansplugins.factionsystem.locks.MfUnlockResult.FAILURE
import com.dansplugins.factionsystem.locks.MfUnlockResult.NOT_LOCKED
import com.dansplugins.factionsystem.locks.MfUnlockResult.SUCCESS
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.block.Block
import org.bukkit.block.BlockFace.DOWN
import org.bukkit.block.BlockFace.UP
import org.bukkit.block.Chest
import org.bukkit.block.DoubleChest
import org.bukkit.block.data.Bisected
import org.bukkit.block.data.Bisected.Half.BOTTOM
import org.bukkit.block.data.type.Door
import org.bukkit.block.data.type.TrapDoor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action.PHYSICAL
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot.HAND
import java.util.logging.Level.SEVERE

class PlayerInteractListener(private val plugin: MedievalFactions) : Listener {

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action == PHYSICAL) { // farmland, pressure plates, tripwire, etc...
            applyProtections(event)
            return
        }
        if (event.hand != HAND) return
        val clickedBlock = event.clickedBlock ?: return
        val interactionService = plugin.services.interactionService
        when (interactionService.getInteractionStatus(MfPlayerId(event.player.uniqueId.toString()))) {
            LOCKING -> {
                lock(event.player, clickedBlock)
                event.isCancelled = true
            }
            UNLOCKING -> {
                unlock(event.player, clickedBlock)
                event.isCancelled = true
            }
            CHECKING_ACCESS -> {
                checkAccess(event.player, clickedBlock)
                event.isCancelled = true
            }
            ADDING_ACCESSOR -> {
                addAccessor(event.player, clickedBlock)
                event.isCancelled = true
            }
            REMOVING_ACCESSOR -> {
                removeAccessor(event.player, clickedBlock)
                event.isCancelled = true
            }
            SELECTING_GATE_POSITION_1 -> {
                selectGatePosition1(event.player, clickedBlock)
                event.isCancelled = true
            }
            SELECTING_GATE_POSITION_2 -> {
                selectGatePosition2(event.player, clickedBlock)
                event.isCancelled = true
            }
            SELECTING_GATE_TRIGGER -> {
                selectGateTrigger(event.player, clickedBlock)
                event.isCancelled = true
            }
            null -> applyProtections(event)
        }
    }

    private fun applyProtections(event: PlayerInteractEvent) {
        val clickedBlock = event.clickedBlock ?: return
        val playerService = plugin.services.playerService
        val mfPlayer = playerService.getPlayer(event.player)
        if (mfPlayer == null) {
            event.isCancelled = true
            plugin.server.scheduler.runTaskAsynchronously(
                plugin,
                Runnable {
                    playerService.save(MfPlayer(plugin, event.player)).onFailure {
                        event.player.sendMessage("$RED${plugin.language["BlockInteractFailedToSavePlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                }
            )
            return
        }
        val lockService = plugin.services.lockService
        val blockData = clickedBlock.blockData
        val holder = (clickedBlock.state as? Chest)?.inventory?.holder
        val blocks = if (blockData is Bisected) {
            if (blockData.half == BOTTOM) {
                listOf(clickedBlock, clickedBlock.getRelative(UP))
            } else {
                listOf(clickedBlock, clickedBlock.getRelative(DOWN))
            }
        } else if (holder is DoubleChest) {
            val left = holder.leftSide as? Chest
            val right = holder.rightSide as? Chest
            listOfNotNull(left?.block, right?.block)
        } else {
            listOf(clickedBlock)
        }
        val lockedBlocks = blocks.mapNotNull { lockService.getLockedBlock(MfBlockPosition.fromBukkitBlock(it)) }
        val lockedBlock = lockedBlocks.firstOrNull()
        if (lockedBlock != null) {
            if (event.player.uniqueId.toString() !in (lockedBlock.accessors + lockedBlock.playerId).map(MfPlayerId::value)) {
                if (mfPlayer.isBypassEnabled && event.player.hasPermission("mf.bypass")) {
                    plugin.server.scheduler.runTaskAsynchronously(
                        plugin,
                        Runnable {
                            val owner = playerService.getPlayer(lockedBlock.playerId)
                            event.player.sendMessage("$RED${plugin.language["LockProtectionBypassed", owner?.toBukkit()?.name ?: plugin.language["UnknownPlayer"]]}")
                        }
                    )
                } else {
                    plugin.server.scheduler.runTaskAsynchronously(
                        plugin,
                        Runnable {
                            val owner = playerService.getPlayer(lockedBlock.playerId)
                            event.player.sendMessage("$RED${plugin.language["BlockLocked", owner?.toBukkit()?.name ?: plugin.language["UnknownPlayer"]]}")
                        }
                    )
                    event.isCancelled = true
                }
                return
            } else {
                return // bypass claim protection to allow access to locks where a player is an accessor
            }
        }

        if (plugin.config.getBoolean("factions.nonMembersCanInteractWithDoors")) {
            if (clickedBlock.state is Door || clickedBlock.state is TrapDoor) {
                return
            }
        }
        val claimService = plugin.services.claimService
        val claim = claimService.getClaim(clickedBlock.chunk)
        if (claim == null) {
            if (plugin.config.getBoolean("wilderness.interaction.prevent", false)) {
                event.isCancelled = true
                if (plugin.config.getBoolean("wilderness.interaction.alert", true)) {
                    event.player.sendMessage("$RED${plugin.language["CannotInteractBlockInWilderness"]}")
                }
            }
            return
        }
        val factionService = plugin.services.factionService
        val claimFaction = factionService.getFaction(claim.factionId) ?: return
        val item = event.item
        if (item != null) {
            if (item.type.isEdible && !clickedBlock.type.isInteractable) return
        }
        if (!claimService.isInteractionAllowed(mfPlayer.id, claim)) {
            if (mfPlayer.isBypassEnabled && event.player.hasPermission("mf.bypass")) {
                event.player.sendMessage("$RED${plugin.language["FactionTerritoryProtectionBypassed"]}")
            } else {
                event.isCancelled = true
                event.player.sendMessage("$RED${plugin.language["CannotInteractWithBlockInFactionTerritory", claimFaction.name]}")
            }
        }
        return
    }

    private fun lock(player: Player, block: Block) {
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
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(player) ?: playerService.save(MfPlayer(plugin, player)).onFailure {
                    player.sendMessage("$RED${plugin.language["BlockLockFailedToSavePlayer"]}")
                    plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                val factionService = plugin.services.factionService
                val playerFaction = factionService.getFaction(mfPlayer.id)
                if (playerFaction == null) {
                    player.sendMessage("$RED${plugin.language["BlockLockNoFaction"]}")
                    return@Runnable
                }
                val claimService = plugin.services.claimService
                val claim = claimService.getClaim(block.chunk)
                if (claim == null || claim.factionId != playerFaction.id) {
                    player.sendMessage("$RED${plugin.language["BlockLockNotInFactionTerritory"]}")
                    return@Runnable
                }
                val lockService = plugin.services.lockService
                val lockedBlocks = blocks.mapNotNull { lockService.getLockedBlock(MfBlockPosition.fromBukkitBlock(it)) }
                val existingLock = lockedBlocks.firstOrNull()
                if (existingLock != null) {
                    val existingLockOwner = playerService.getPlayer(existingLock.playerId)
                    player.sendMessage("$RED${plugin.language["BlockLockAlreadyLocked", existingLockOwner?.toBukkit()?.name ?: plugin.language["UnknownPlayer"]]}")
                    return@Runnable
                }
                lockService.lock(MfBlockPosition.fromBukkitBlock(block), claim, mfPlayer).onFailure {
                    player.sendMessage("$RED${plugin.language["BlockLockFailedToSaveLockedBlock"]}")
                    plugin.logger.log(SEVERE, "Failed to save locked block: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                val interactionService = plugin.services.interactionService
                interactionService.setInteractionStatus(mfPlayer.id, null).onFailure {
                    player.sendMessage("$RED${plugin.language["BlockLockFailedToSetInteractionStatus"]}")
                    plugin.logger.log(SEVERE, "Failed to save interaction status: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                player.sendMessage("$GREEN${plugin.language["BlockLockSuccessful"]}")
            }
        )
    }

    private fun unlock(player: Player, block: Block) {
        val lockService = plugin.services.lockService
        val lockedBlock = lockService.getLockedBlock(MfBlockPosition.fromBukkitBlock(block))
        if (lockedBlock == null) {
            player.sendMessage("$RED${plugin.language["BlockUnlockNotLocked"]}")
            return
        }
        val playerService = plugin.services.playerService
        val mfPlayer = playerService.getPlayer(player) ?: return
        if (lockedBlock.playerId.value != mfPlayer.id.value) {
            val lockOwner = playerService.getPlayer(lockedBlock.playerId)
            val ownerName = if (lockOwner == null) {
                plugin.language["UnknownPlayer"]
            } else {
                lockOwner.toBukkit().name ?: plugin.language["UnknownPlayer"]
            }
            if (!player.hasPermission("mf.force.unlock")) {
                player.sendMessage("$RED${plugin.language["BlockUnlockOwnedByOtherPlayer", ownerName]}")
                return
            } else {
                player.sendMessage("$RED${plugin.language["BlockUnlockProtectionBypassed", ownerName]}")
            }
        }
        lockService.unlock(block) { result ->
            when (result) {
                SUCCESS -> player.sendMessage("$GREEN${plugin.language["BlockUnlockSuccessful"]}")
                NOT_LOCKED -> player.sendMessage("$RED${plugin.language["BlockNotLocked"]}")
                FAILURE -> player.sendMessage("$RED${plugin.language["BlockUnlockFailedToSaveLockedBlock"]}")
            }
        }
    }

    private fun checkAccess(player: Player, block: Block) {
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
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(player) ?: playerService.save(MfPlayer(plugin, player)).onFailure {
                    player.sendMessage("$RED${plugin.language["BlockCheckAccessFailedToSavePlayer"]}")
                    plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                val lockService = plugin.services.lockService
                val lockedBlocks = blocks.mapNotNull { lockService.getLockedBlock(MfBlockPosition.fromBukkitBlock(it)) }
                val lockedBlock = lockedBlocks.firstOrNull()
                val interactionService = plugin.services.interactionService
                if (lockedBlock == null) {
                    player.sendMessage("$RED${plugin.language["BlockCheckAccessNotLocked"]}")
                    interactionService.setInteractionStatus(mfPlayer.id, null).onFailure {
                        player.sendMessage("$RED${plugin.language["BlockCheckAccessFailedToSetInteractionStatus"]}")
                        plugin.logger.log(SEVERE, "Failed to set interaction status: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                    return@Runnable
                }
                plugin.server.scheduler.runTask(
                    plugin,
                    Runnable {
                        player.performCommand("accessors list ${lockedBlock.block.x} ${lockedBlock.block.y} ${lockedBlock.block.z}")
                    }
                )
                interactionService.setInteractionStatus(mfPlayer.id, null).onFailure {
                    player.sendMessage("$RED${plugin.language["BlockCheckAccessFailedToSetInteractionStatus"]}")
                    plugin.logger.log(SEVERE, "Failed to set interaction status: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
            }
        )
    }

    private fun addAccessor(player: Player, block: Block) {
        player.performCommand("accessors add ${block.x} ${block.y} ${block.z}")
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(player) ?: playerService.save(MfPlayer(plugin, player)).onFailure {
                    player.sendMessage("$RED${plugin.language["BlockAddAccessorFailedToSavePlayer"]}")
                    plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                val interactionService = plugin.services.interactionService
                interactionService.setInteractionStatus(mfPlayer.id, null).onFailure {
                    player.sendMessage("$RED${plugin.language["BlockAddAccessorFailedToSetInteractionStatus"]}")
                    plugin.logger.log(SEVERE, "Failed to set interaction status: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
            }
        )
    }

    private fun removeAccessor(player: Player, block: Block) {
        player.performCommand("accessors remove ${block.x} ${block.y} ${block.z}")
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(player) ?: playerService.save(MfPlayer(plugin, player)).onFailure {
                    player.sendMessage("$RED${plugin.language["BlockRemoveAccessorFailedToSavePlayer"]}")
                    plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                val interactionService = plugin.services.interactionService
                interactionService.setInteractionStatus(mfPlayer.id, null).onFailure {
                    player.sendMessage("$RED${plugin.language["BlockRemoveAccessorFailedToSetInteractionStatus"]}")
                    plugin.logger.log(SEVERE, "Failed to set interaction status: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
            }
        )
    }

    private fun selectGatePosition1(player: Player, block: Block) {
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                if (block.type in plugin.services.gateService.restrictedBlockMaterials) {
                    player.sendMessage("$RED${plugin.language["GateCreateRestrictedBlock"]}")
                    return@Runnable
                }
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(player) ?: playerService.save(MfPlayer(plugin, player)).onFailure {
                    player.sendMessage("$RED${plugin.language["GateCreateSelectFirstPositionFailedToSavePlayer"]}")
                    plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                val gateService = plugin.services.gateService
                val ctx = gateService.getGateCreationContext(mfPlayer.id).onFailure {
                    player.sendMessage("$RED${plugin.language["GateCreateSelectFirstPositionFailedToGetGateCreationContext"]}")
                    plugin.logger.log(SEVERE, "Failed to get gate creation context: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                } ?: MfGateCreationContext(mfPlayer.id)
                gateService.save(ctx.copy(position1 = MfBlockPosition.fromBukkitBlock(block))).onFailure {
                    player.sendMessage("$RED${plugin.language["GateCreateSelectFirstPositionFailedToSaveGateCreationContext"]}")
                    plugin.logger.log(SEVERE, "Failed to save gate creation context: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                val interactionService = plugin.services.interactionService
                interactionService.setInteractionStatus(mfPlayer.id, SELECTING_GATE_POSITION_2).onFailure {
                    player.sendMessage("$RED${plugin.language["GateCreateSelectFirstPositionFailedToSetInteractionStatus"]}")
                    plugin.logger.log(SEVERE, "Failed to set interaction status: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                player.sendMessage("$GREEN${plugin.language["GateCreateSelectSecondPosition"]}")
            }
        )
    }

    private fun selectGatePosition2(player: Player, block: Block) {
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                if (block.type in plugin.services.gateService.restrictedBlockMaterials) {
                    player.sendMessage("$RED${plugin.language["GateCreateRestrictedBlock"]}")
                    return@Runnable
                }
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(player) ?: playerService.save(MfPlayer(plugin, player)).onFailure {
                    player.sendMessage("$RED${plugin.language["GateCreateSelectSecondPositionFailedToSavePlayer"]}")
                    plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                val gateService = plugin.services.gateService
                val ctx = gateService.getGateCreationContext(mfPlayer.id).onFailure {
                    player.sendMessage("$RED${plugin.language["GateCreateSelectSecondPositionFailedToGetGateCreationContext"]}")
                    plugin.logger.log(SEVERE, "Failed to get gate creation context: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                } ?: MfGateCreationContext(mfPlayer.id)
                gateService.save(ctx.copy(position2 = MfBlockPosition.fromBukkitBlock(block))).onFailure {
                    player.sendMessage("$RED${plugin.language["GateCreateSelectSecondPositionFailedToSaveGateCreationContext"]}")
                    plugin.logger.log(SEVERE, "Failed to save gate creation context: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                val interactionService = plugin.services.interactionService
                interactionService.setInteractionStatus(mfPlayer.id, SELECTING_GATE_TRIGGER).onFailure {
                    player.sendMessage("$RED${plugin.language["GateCreateSelectSecondPositionFailedToSetInteractionStatus"]}")
                    plugin.logger.log(SEVERE, "Failed to set interaction status: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                player.sendMessage("$GREEN${plugin.language["GateCreateSelectTrigger"]}")
            }
        )
    }

    private fun selectGateTrigger(player: Player, block: Block) {
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                if (block.type in plugin.services.gateService.restrictedBlockMaterials) {
                    player.sendMessage("$RED${plugin.language["GateCreateRestrictedBlock"]}")
                    return@Runnable
                }
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(player) ?: playerService.save(MfPlayer(plugin, player)).onFailure {
                    player.sendMessage("$RED${plugin.language["GateCreateSelectTriggerFailedToSavePlayer"]}")
                    plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                val gateService = plugin.services.gateService
                val ctx = gateService.getGateCreationContext(mfPlayer.id).onFailure {
                    player.sendMessage("$RED${plugin.language["GateCreateSelectTriggerFailedToGetGateCreationContext"]}")
                    plugin.logger.log(SEVERE, "Failed to get gate creation context: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                } ?: MfGateCreationContext(mfPlayer.id)
                val updatedCtx = gateService.save(ctx.copy(trigger = MfBlockPosition.fromBukkitBlock(block))).onFailure {
                    player.sendMessage("$RED${plugin.language["GateCreateSelectTriggerFailedToSaveGateCreationContext"]}")
                    plugin.logger.log(SEVERE, "Failed to save gate creation context: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                createGate(player, updatedCtx)
            }
        )
    }

    private fun createGate(player: Player, ctx: MfGateCreationContext) {
        plugin.server.scheduler.runTask(
            plugin,
            Runnable syncValidations@{
                val position1 = ctx.position1
                if (position1 == null) {
                    player.sendMessage("$RED${plugin.language["GateCreateFirstPositionNotSet"]}")
                    restartGateCreation(player, ctx)
                    return@syncValidations
                }
                val position2 = ctx.position2
                if (position2 == null) {
                    player.sendMessage("$RED${plugin.language["GateCreateSecondPositionNotSet"]}")
                    restartGateCreation(player, ctx)
                    return@syncValidations
                }
                val trigger = ctx.trigger
                if (trigger == null) {
                    player.sendMessage("$RED${plugin.language["GateCreateTriggerNotSet"]}")
                    restartGateCreation(player, ctx)
                    return@syncValidations
                }

                // Validate area dimensions
                val area = MfCuboidArea(position1, position2)
                if (area.width > 1 && area.depth > 1) {
                    player.sendMessage("$RED${plugin.language["GateCreateMustBeFlatPlane"]}")
                    restartGateCreation(player, ctx)
                    return@syncValidations
                }
                val minHeight = plugin.config.getInt("gates.minHeight")
                if (area.height < minHeight) {
                    player.sendMessage("$RED${plugin.language["GateCreateMinHeightNotMet", minHeight.toString()]}")
                    restartGateCreation(player, ctx)
                    return@syncValidations
                }

                val blocks = area.blocks

                // Validate restricted blocks within the area
                val restrictedBlock = blocks.firstOrNull { it.toBukkitBlock()?.type in plugin.services.gateService.restrictedBlockMaterials }
                if (restrictedBlock != null) {
                    player.sendMessage("$RED${plugin.language["GateCreateAreaRestrictedBlock"]}")
                    restartGateCreation(player, ctx)
                    return@syncValidations
                }

                val maxBlocks = plugin.config.getInt("gates.maxBlocks")
                if (blocks.size > maxBlocks) {
                    player.sendMessage("$RED${plugin.language["GateCreateAreaLimitExceeded", maxBlocks.toString()]}")
                    restartGateCreation(player, ctx)
                    return@syncValidations
                }

                val chunks = blocks.mapTo(mutableSetOf()) { it.toBukkitBlock()?.chunk }
                val triggerChunk = trigger.toBukkitBlock()?.chunk

                val materials = blocks.mapTo(mutableSetOf()) { it.toBukkitBlock()?.type }
                val material = materials.singleOrNull()
                if (material == null) {
                    player.sendMessage(
                        "$RED${plugin.language[
                            "GateCreateGateMustContainSingleBlockType",
                            materials.joinToString {
                                it?.toString()
                                    ?.lowercase()
                                    ?.replace('_', ' ')
                                    ?: plugin.language["UnrecognisedBlock"]
                            }
                        ]}"
                    )
                    restartGateCreation(player, ctx)
                    return@syncValidations
                }

                plugin.server.scheduler.runTaskAsynchronously(
                    plugin,
                    Runnable updateInteractionStatus@{
                        val interactionService = plugin.services.interactionService
                        val factionService = plugin.services.factionService
                        val faction = factionService.getFaction(ctx.playerId)
                        if (faction == null) {
                            player.sendMessage("$RED${plugin.language["GateCreateMustBeInAFaction"]}")
                            cancelGateCreation(player, ctx)
                            return@updateInteractionStatus
                        }

                        val claimService = plugin.services.claimService
                        val claims = chunks.map { chunk -> chunk?.let { claimService.getClaim(it) } }
                        if (claims.any { it == null || it.factionId != faction.id }) {
                            player.sendMessage("$RED${plugin.language["GateCreateGateCrossesUnclaimedTerritory"]}")
                            restartGateCreation(player, ctx)
                            return@updateInteractionStatus
                        }
                        val triggerClaim = triggerChunk?.let { claimService.getClaim(triggerChunk) }
                        if (triggerClaim == null || triggerClaim.factionId != faction.id) {
                            player.sendMessage("$RED${plugin.language["GateCreateTriggerInUnclaimedTerritory"]}")
                            restartGateCreation(player, ctx)
                            return@updateInteractionStatus
                        }
                        val gateService = plugin.services.gateService
                        val maxGates = plugin.config.getInt("gates.maxPerFaction")
                        if (gateService.getGatesByFaction(faction.id).size >= maxGates) {
                            player.sendMessage("$RED${plugin.language["GateCreateFactionMaxGatesReached", maxGates.toString()]}")
                            cancelGateCreation(player, ctx)
                            return@updateInteractionStatus
                        }
                        gateService.save(
                            MfGate(
                                plugin,
                                factionId = faction.id,
                                area = area,
                                trigger = ctx.trigger,
                                material = material
                            )
                        ).onFailure {
                            player.sendMessage("$RED${plugin.language["GateCreateFailedToSaveGate"]}")
                            plugin.logger.log(SEVERE, "Failed to save gate: ${it.reason.message}", it.reason.cause)
                            return@updateInteractionStatus
                        }
                        gateService.deleteGateCreationContext(ctx.playerId).onFailure {
                            player.sendMessage("$RED${plugin.language["GateCreateFailedToDeleteCreationContext"]}")
                            plugin.logger.log(SEVERE, "Failed to delete gate creation context: ${it.reason.message}", it.reason.cause)
                            return@updateInteractionStatus
                        }
                        interactionService.setInteractionStatus(ctx.playerId, null).onFailure {
                            player.sendMessage("$RED${plugin.language["GateCreateFailedToSetInteractionStatus"]}")
                            plugin.logger.log(SEVERE, "Failed to set interaction status: ${it.reason.message}", it.reason.cause)
                            return@updateInteractionStatus
                        }
                        player.sendMessage("$GREEN${plugin.language["GateCreated"]}")
                    }
                )
            }
        )
    }

    private fun restartGateCreation(player: Player, ctx: MfGateCreationContext) {
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val interactionService = plugin.services.interactionService
                interactionService.setInteractionStatus(ctx.playerId, SELECTING_GATE_POSITION_1).onFailure {
                    player.sendMessage("$RED${plugin.language["GateCreateFailedToSetInteractionStatus"]}")
                    plugin.logger.log(SEVERE, "Failed to set interaction status: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                player.sendMessage("$GREEN${plugin.language["GateCreateSelectFirstPosition"]}")
            }
        )
    }

    private fun cancelGateCreation(player: Player, ctx: MfGateCreationContext) {
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val interactionService = plugin.services.interactionService
                interactionService.setInteractionStatus(ctx.playerId, null).onFailure {
                    player.sendMessage("$RED${plugin.language["GateCreateFailedToSetInteractionStatus"]}")
                    plugin.logger.log(SEVERE, "Failed to set interaction status: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
            }
        )
    }
}
