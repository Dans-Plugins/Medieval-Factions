package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.area.MfCuboidArea
import com.dansplugins.factionsystem.claim.MfClaimedChunk
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
import org.bukkit.Material
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
import org.bukkit.event.Event.Result.DENY
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action.LEFT_CLICK_BLOCK
import org.bukkit.event.block.Action.PHYSICAL
import org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot.HAND
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level.SEVERE
import org.bukkit.block.data.type.Gate as FenceGateData

class PlayerInteractListener(private val plugin: MedievalFactions) : Listener {

    private companion object {
        // Hand-used items whose right-click use acts on the player rather than on the world - drinking,
        // throwing, drawing or raising - and which therefore have no block-targeted behaviour at all.
        // Material.isEdible already covers food, so only the non-edible cases are listed here.
        // Items that do act on the clicked block (a bucket, flint and steel, a hoe, an axe, a shovel,
        // bone meal, an eye of ender, a firework rocket, a wind charge, anything placeable) are
        // deliberately absent, because releasing the item half of a protected interaction for those
        // would let a non-member alter a claim. The list is therefore fail-closed: an item missing from
        // it is merely restricted, never a hole in protection.
        private val WORLD_NEUTRAL_MATERIALS = setOf(
            Material.POTION,
            Material.SPLASH_POTION,
            Material.LINGERING_POTION,
            Material.MILK_BUCKET,
            Material.HONEY_BOTTLE,
            Material.OMINOUS_BOTTLE,
            Material.EXPERIENCE_BOTTLE,
            Material.BOW,
            Material.CROSSBOW,
            Material.SNOWBALL,
            Material.EGG,
            Material.ENDER_PEARL,
            Material.FISHING_ROD,
            Material.TRIDENT,
            Material.SHIELD,
            Material.SPYGLASS,
            Material.GOAT_HORN
        )
    }

    // Tracks players whose MfPlayer record is currently being created asynchronously, so that
    // Action.PHYSICAL (which fires once per tick while the player stands on the block) doesn't
    // queue a duplicate save for every tick until the first one completes.
    private val playersWithPendingSave: MutableSet<MfPlayerId> = ConcurrentHashMap.newKeySet()

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

    /**
     * Checks if a player has the faction permission to bypass locks.
     * * @param mfPlayer The player to check
     * @return true if the player's faction role has the BYPASS_LOCKS permission, false otherwise
     *         (including when the player has no faction or no role)
     */
    private fun hasLockBypassPermission(mfPlayer: MfPlayer): Boolean {
        val factionService = plugin.services.factionService
        val playerFaction = factionService.getFaction(mfPlayer.id) ?: return false
        val role = playerFaction.getRole(mfPlayer.id) ?: return false
        return role.hasPermission(playerFaction, plugin.factionPermissions.bypassLocks)
    }

    private fun applyProtections(event: PlayerInteractEvent) {
        val clickedBlock = event.clickedBlock ?: return
        val playerService = plugin.services.playerService
        val claimService = plugin.services.claimService
        val mfPlayer = playerService.getPlayer(event.player)

        // Physical interactions (pressure plates, tripwire, farmland) fire once per tick for as long as
        // the player is stood on the block, so notifying on each one floods the player's chat. The
        // interaction is still cancelled - only the message is suppressed.
        val suppressProtectionMessages = event.action == PHYSICAL

        if (mfPlayer == null) {
            event.isCancelled = true
            val playerId = MfPlayerId(event.player.uniqueId.toString())
            if (playersWithPendingSave.add(playerId)) {
                plugin.server.scheduler.runTaskAsynchronously(
                    plugin,
                    Runnable {
                        try {
                            playerService.save(MfPlayer(plugin, event.player)).onFailure {
                                event.player.sendMessage("$RED${plugin.language["BlockInteractFailedToSavePlayer"]}")
                                plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                                return@Runnable
                            }
                        } finally {
                            playersWithPendingSave.remove(playerId)
                        }
                    }
                )
            }
            return
        }

        // Handle locks first
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
                // Check if player has bypass permission from mf.bypass or faction permission
                if ((mfPlayer.isBypassEnabled && event.player.hasPermission("mf.bypass")) || hasLockBypassPermission(mfPlayer)) {
                    // The whole task is skipped when suppressing, not just the message: each one performs
                    // an owner lookup, so scheduling it per tick is the expensive half of the problem.
                    if (!suppressProtectionMessages) {
                        plugin.server.scheduler.runTaskAsynchronously(
                            plugin,
                            Runnable {
                                val owner = playerService.getPlayer(lockedBlock.playerId)
                                event.player.sendMessage("$RED${plugin.language["LockProtectionBypassed", owner?.toBukkit()?.name ?: plugin.language["UnknownPlayer"]]}")
                            }
                        )
                    }
                } else {
                    // A lock is a separate protection mechanism from territory, but the two-result
                    // distinction applies to it identically: the locked block must stay shut, while an
                    // item that cannot act on it has no reason to be suppressed.
                    val deniedInFull = denyInteraction(event)
                    if (deniedInFull && !suppressProtectionMessages) {
                        plugin.server.scheduler.runTaskAsynchronously(
                            plugin,
                            Runnable {
                                val owner = playerService.getPlayer(lockedBlock.playerId)
                                event.player.sendMessage("$RED${plugin.language["BlockLocked", owner?.toBukkit()?.name ?: plugin.language["UnknownPlayer"]]}")
                            }
                        )
                    }
                }
                return
            } else {
                return // bypass claim protection to allow access to locks where a player is an accessor
            }
        }

        // Handle door/trapdoor/fence-gate special case
        if (plugin.config.getBoolean("factions.nonMembersCanInteractWithDoors")) {
            if (blockData is Door || blockData is TrapDoor || blockData is FenceGateData) {
                return
            }
        }

        // Apply territory protection
        val claim = claimService.getClaim(clickedBlock.chunk)

        if (claim == null) {
            if (plugin.config.getBoolean("wilderness.interaction.prevent", false)) {
                // The option protects blocks, not the player's own food, so denyInteraction keeps
                // eating and drinking working here exactly as it does inside a claim - see #1747.
                val deniedInFull = denyInteraction(event)
                if (deniedInFull && plugin.config.getBoolean("wilderness.interaction.alert", true) && !suppressProtectionMessages) {
                    event.player.sendMessage("$RED${plugin.language["CannotInteractBlockInWilderness"]}")
                }
            }
            return
        }

        val factionService = plugin.services.factionService
        val claimFaction = factionService.getFaction(claim.factionId) ?: return

        // Check if player is allowed to interact based on faction relationships
        if (!claimService.isInteractionAllowed(mfPlayer.id, claim)) {
            if (mfPlayer.isBypassEnabled && event.player.hasPermission("mf.bypass")) {
                if (!suppressProtectionMessages) {
                    event.player.sendMessage("$RED${plugin.language["FactionTerritoryProtectionBypassed"]}")
                }
            } else {
                // Check if player is at war and trying to place a ladder
                // Only allow if they're right-clicking with a ladder on a solid, non-interactive block
                val isPlacingLadder = event.action == RIGHT_CLICK_BLOCK &&
                    event.hasItem() && event.item?.type == Material.LADDER && clickedBlock.type.isSolid && !isInteractiveBlock(clickedBlock.type)
                if (claimService.isWartimeLadderPlacementAllowed(
                        mfPlayer.id,
                        claim,
                        isPlacingLadder
                    )
                ) {
                    // Allow ladder placement in enemy territory during wartime
                    return
                }
                if (isWartimeActionAllowed(event, clickedBlock, mfPlayer, claim)) {
                    return
                }
                val deniedInFull = denyInteraction(event)
                if (deniedInFull && !suppressProtectionMessages) {
                    event.player.sendMessage("$RED${plugin.language["CannotInteractWithBlockInFactionTerritory", claimFaction.name]}")
                }
            }
        }
    }

    /**
     * Named predicate for "interactive block" (chest, lever, door, etc. - anything that responds
     * to a right-click independently of what the player is holding). Kept as a thin wrapper around
     * Bukkit's [Material.isInteractable] - already the definition used elsewhere in this listener
     * (the ladder-placement and wartime checks) - so the definition lives in one named place per
     * #1970, rather than being re-derived inline at each call site.
     */
    private fun isInteractiveBlock(material: Material): Boolean = material.isInteractable

    /**
     * True when this interaction is the player using what they are holding on themselves - eating,
     * drinking, throwing, drawing a bow, raising a shield - rather than acting on the block. The
     * action type is checked first and strictly: a left-click never uses an item this way, and a
     * physical interaction has no item at all, so neither may be exempted. See [WORLD_NEUTRAL_MATERIALS]
     * for why the set of materials is a fail-closed allowance rather than a list of what to block.
     */
    private fun isWorldNeutralItemUse(event: PlayerInteractEvent): Boolean {
        if (event.action != RIGHT_CLICK_BLOCK) return false
        val item = event.item ?: return false
        return item.type.isEdible || item.type in WORLD_NEUTRAL_MATERIALS
    }

    /**
     * Refuses a protected interaction, and reports whether it was refused in full.
     *
     * A [PlayerInteractEvent] carries two independent results - the interacted block and the item in
     * hand - and `isCancelled = true` denies both at once. Denying the item in hand also suppresses
     * the follow-up use-item packet the client sends when a block-targeted use does nothing, which is
     * why cancelling outright stopped a player eating, drinking, shooting a bow, throwing a snowball
     * or an ender pearl, casting a fishing rod or throwing a trident while merely looking at a
     * protected block (#1747, #1995).
     *
     * So when the held item cannot act on the block, only the block result is denied and the item is
     * left to run. Everything else - every left-click, every physical interaction, and every
     * right-click with an item that could place, break or alter a block - is still cancelled in full,
     * because a left-click and a physical interaction are gated on `isCancelled` alone, and because
     * an item's block-targeted use is gated on the item result rather than the block result.
     *
     * @return true when both results were denied, false when only the block was protected. The
     *         protection message is sent only in the former case: the player who was eating never
     *         attempted to use the block, and #1747's per-interaction chat noise must not return.
     */
    private fun denyInteraction(event: PlayerInteractEvent): Boolean {
        if (isWorldNeutralItemUse(event)) {
            event.setUseInteractedBlock(DENY)
            return false
        }
        event.isCancelled = true
        return true
    }

    /**
     * Strictly gates which wartime permission check (if any) is consulted, based solely on the
     * Bukkit [org.bukkit.event.block.Action] and whether the clicked block is interactive. The
     * action type is resolved first and is the sole determinant of which branch runs; the held
     * item's material plays no role in branch selection (only in the value passed to the placeable
     * check once that branch is already chosen). This keeps action-type detection and wartime
     * config evaluation as two separate steps with no overlap - see #1968, #1970.
     */
    private fun isWartimeActionAllowed(
        event: PlayerInteractEvent,
        clickedBlock: Block,
        mfPlayer: MfPlayer,
        claim: MfClaimedChunk
    ): Boolean {
        val claimService = plugin.services.claimService
        return when (event.action) {
            LEFT_CLICK_BLOCK ->
                // Block is in the wartime breakable list; allow the left-click so BlockBreakEvent can fire
                claimService.isWartimeBreakableBlock(mfPlayer.id, claim, clickedBlock.type)
            RIGHT_CLICK_BLOCK -> if (isInteractiveBlock(clickedBlock.type)) {
                // Block is in the wartime interactable list; allow the interaction
                claimService.isWartimeInteractableBlock(mfPlayer.id, claim, clickedBlock.type)
            } else if (event.hasItem()) {
                // Item in hand is in the wartime placeable list; allow the right-click so BlockPlaceEvent can fire
                val itemType = event.item?.type
                itemType != null && claimService.isWartimePlaceableBlock(mfPlayer.id, claim, itemType)
            } else {
                false
            }
            else -> false
        }
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

            // Check if player has bypass permission from mf.force.unlock or faction permission
            val canBypass = player.hasPermission("mf.force.unlock") || hasLockBypassPermission(mfPlayer)
            if (!canBypass) {
                player.sendMessage("$RED${plugin.language["BlockUnlockOwnedByOtherPlayer", ownerName]}")
                return
            }
            player.sendMessage("$RED${plugin.language["BlockUnlockProtectionBypassed", ownerName]}")
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
