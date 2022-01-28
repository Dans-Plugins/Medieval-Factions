/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.eventhandlers;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.domain.ClaimedChunk;
import dansplugins.factionsystem.objects.domain.LockedBlock;
import dansplugins.factionsystem.services.LocalGateService;
import dansplugins.factionsystem.services.LocalLocaleService;
import dansplugins.factionsystem.services.LocalLockService;
import dansplugins.factionsystem.utils.BlockChecker;
import dansplugins.factionsystem.utils.InteractionAccessChecker;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.InventoryHolder;
import preponderous.ponder.minecraft.spigot.tools.UUIDChecker;

import java.util.Objects;

/**
 * @author Daniel McCoy Stephenson
 */
public class InteractionHandler implements Listener {

    @EventHandler()
    public void handle(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        ClaimedChunk claimedChunk = PersistentData.getInstance().getChunkDataAccessor().getClaimedChunk(block.getLocation().getChunk());

        if (InteractionAccessChecker.getInstance().shouldEventBeCancelled(claimedChunk, player)) {
            event.setCancelled(true);
            return;
        }

        if (PersistentData.getInstance().isBlockInGate(block, player)) {
            event.setCancelled(true);
            return;
        }

        if (PersistentData.getInstance().isBlockLocked(block)) {
            boolean isOwner = PersistentData.getInstance().getLockedBlock(block).getOwner().equals(player.getUniqueId());
            if (!isOwner) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + LocalLocaleService.getInstance().getText("AlertNonOwnership"));
                return;
            }

            PersistentData.getInstance().removeLockedBlock(block);

            if (BlockChecker.getInstance().isDoor(block)) {
                removeLocksAboveAndBelowTheOriginalBlockAsWell(block);
            }
        }
    }

    private void removeLocksAboveAndBelowTheOriginalBlockAsWell(Block block) {

        Block relativeUp = block.getRelative(BlockFace.UP);
        Block relativeDown = block.getRelative(BlockFace.DOWN);
        if (BlockChecker.getInstance().isDoor(relativeUp)) {
            PersistentData.getInstance().removeLockedBlock(relativeUp);
        }
        if (BlockChecker.getInstance().isDoor(relativeDown)) {
            PersistentData.getInstance().removeLockedBlock(relativeDown);
        }
    }

    @EventHandler()
    public void handle(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        ClaimedChunk claimedChunk = PersistentData.getInstance().getChunkDataAccessor().getClaimedChunk(event.getBlock().getLocation().getChunk());

        if (InteractionAccessChecker.getInstance().isPlayerAttemptingToPlaceLadderInEnemyTerritoryAndIsThisAllowed(event.getBlockPlaced(), player, claimedChunk)) {
            return;
        }

        if (InteractionAccessChecker.getInstance().shouldEventBeCancelled(claimedChunk, player)) {
            event.setCancelled(true);
            return;
        }

        if (BlockChecker.getInstance().isChest(event.getBlock())) {
            boolean isNextToNonOwnedLockedChest = BlockChecker.getInstance().isNextToNonOwnedLockedChest(event.getPlayer(), event.getBlock());
            if (isNextToNonOwnedLockedChest) {
                player.sendMessage(ChatColor.RED + LocalLocaleService.getInstance().getText("CannotPlaceChestsNextToUnownedLockedChests"));
                event.setCancelled(true);
                return;
            }

            int seconds = 2;
            MedievalFactions.getInstance().getServer().getScheduler().runTaskLater(MedievalFactions.getInstance(), () -> {
                Block block = player.getWorld().getBlockAt(event.getBlock().getLocation());

                if (!BlockChecker.getInstance().isChest(block)) {
                    // There has been 2 seconds since we last confirmed this was a chest, double-checking isn't ever bad :)
                    return;
                }

                InventoryHolder holder = ((Chest) block.getState()).getInventory().getHolder();
                if (holder instanceof DoubleChest) {
                    // make sure both sides are locked
                    DoubleChest doubleChest = (DoubleChest) holder;
                    Block leftChest = ((Chest) Objects.requireNonNull(doubleChest.getLeftSide())).getBlock();
                    Block rightChest = ((Chest) Objects.requireNonNull(doubleChest.getRightSide())).getBlock();

                    if (PersistentData.getInstance().isBlockLocked(leftChest)) {
                        // lock right chest
                        LockedBlock right = new LockedBlock(player.getUniqueId(), PersistentData.getInstance().getPlayersFaction(player.getUniqueId()).getName(), rightChest.getX(), rightChest.getY(), rightChest.getZ(), rightChest.getWorld().getName());
                        PersistentData.getInstance().addLockedBlock(right);
                    }
                    else {
                        if (PersistentData.getInstance().isBlockLocked(rightChest)) {
                            // lock left chest
                            LockedBlock left = new LockedBlock(player.getUniqueId(), PersistentData.getInstance().getPlayersFaction(player.getUniqueId()).getName(), leftChest.getX(), leftChest.getY(), leftChest.getZ(), leftChest.getWorld().getName());
                            PersistentData.getInstance().addLockedBlock(left);
                        }
                    }

                }
            }, seconds * 20);
        }

        // if hopper
        if (event.getBlock().getType() == Material.HOPPER) {
            boolean isNextToNonOwnedLockedChest = BlockChecker.getInstance().isNextToNonOwnedLockedChest(event.getPlayer(), event.getBlock());
            boolean isUnderOrAboveNonOwnedLockedChest = BlockChecker.getInstance().isUnderOrAboveNonOwnedLockedChest(event.getPlayer(), event.getBlock());
            if (isNextToNonOwnedLockedChest || isUnderOrAboveNonOwnedLockedChest) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + LocalLocaleService.getInstance().getText("CannotPlaceHoppersNextToUnownedLockedChests"));
            }
        }
    }

    @EventHandler()
    public void handle(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            // player has clicked air
            return;
        }

        if (clickedBlock instanceof ItemFrame) {
            if (MedievalFactions.getInstance().isDebugEnabled()) { System.out.println("DEBUG: ItemFrame interaction captured in PlayerInteractEvent!"); }
        }

        // ---------------------------------------------------------------------------------------------------------------

        // if player is attempting to lock a block
        if (EphemeralData.getInstance().getLockingPlayers().contains(player.getUniqueId())) {
            LocalLockService.getInstance().handleLockingBlock(event, player, clickedBlock);
        }

        // ---------------------------------------------------------------------------------------------------------------

        // if player is trying to unlock a block
        if (EphemeralData.getInstance().getUnlockingPlayers().contains(player.getUniqueId())) {
            LocalLockService.getInstance().handleUnlockingBlock(event, player, clickedBlock);
        }

        // ---------------------------------------------------------------------------------------------------------------
        
        LockedBlock lockedBlock = PersistentData.getInstance().getLockedBlock(clickedBlock);
        if (lockedBlock != null) {
            boolean playerHasAccess = lockedBlock.hasAccess(player.getUniqueId());
            boolean isPlayerBypassing = EphemeralData.getInstance().getAdminsBypassingProtections().contains(player.getUniqueId());
            if (!playerHasAccess && !isPlayerBypassing) {
                // player doesn't have access and isn't overriding
                UUIDChecker uuidChecker = new UUIDChecker();
                String owner = uuidChecker.findPlayerNameBasedOnUUID(lockedBlock.getOwner());
                player.sendMessage(ChatColor.RED + String.format(LocalLocaleService.getInstance().getText("LockedBy"), owner));
                event.setCancelled(true);
                return;
            }
            
            if (EphemeralData.getInstance().getPlayersGrantingAccess().containsKey(player.getUniqueId())) {
                // player is trying to grant access
                LocalLockService.getInstance().handleGrantingAccess(event, clickedBlock, player);
            }
            
            if (EphemeralData.getInstance().getPlayersCheckingAccess().contains(player.getUniqueId())) {
                // player is trying to check access
                LocalLockService.getInstance().handleCheckingAccess(event, lockedBlock, player);
            }
            
            if (EphemeralData.getInstance().getPlayersRevokingAccess().containsKey(player.getUniqueId())) {
                // player is trying to revoke access
                LocalLockService.getInstance().handleRevokingAccess(event, clickedBlock, player);
            }

            if (playerHasAccess) {
                /*
                Don't process any more checks so that the event is not cancelled
                when a player who is not part of the faction has access granted
                to a lock.
                */
                return;
            }

        }
        else {
            if (isPlayerUsingAnAccessCommand(player)) {
                player.sendMessage(ChatColor.RED + LocalLocaleService.getInstance().getText("BlockIsNotLocked"));
            }
        }

        // Check if it's a lever, and if it is and it's connected to a gate in the faction
        // territory then open/close the gate.
        boolean playerClickedLever = clickedBlock.getType().equals(Material.LEVER);
        if (playerClickedLever) {
            LocalGateService.getInstance().handlePotentialGateInteraction(clickedBlock, player, event);
        }

        // ---------------------------------------------------------------------------------------------------------------

        // pgarner Sep 2, 2020: Moved this to after test to see if the block is locked because it could be a block they have been granted
        // access to (or in future, a 'public' locked block), so if they're not in the faction whose territory the block exists in we want that
        // check to be handled before the interaction is rejected for not being a faction member.
        // if chunk is claimed
        ClaimedChunk chunk = PersistentData.getInstance().getChunkDataAccessor().getClaimedChunk(event.getClickedBlock().getLocation().getChunk());
        if (chunk != null) {
            PersistentData.getInstance().getChunkDataAccessor().handleClaimedChunkInteraction(event, chunk);
        }

        // ---------------------------------------------------------------------------------------------------------------

        // get tool in player's hand, if it's the gate tool
        // then we want to let them create the gate.
        boolean playerCreatingGate = EphemeralData.getInstance().getCreatingGatePlayers().containsKey(event.getPlayer().getUniqueId());
        boolean playerHoldingGoldenHoe = player.getInventory().getItemInMainHand().getType().equals(Material.GOLDEN_HOE);
        if (playerCreatingGate && playerHoldingGoldenHoe) {
            LocalGateService.getInstance().handleCreatingGate(clickedBlock, player, event);
        }

        // ---------------------------------------------------------------------------------------------------------------
    }

    @EventHandler()
    public void handle(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        Entity clickedEntity = event.getRightClicked();

        Location location = null;

        if (clickedEntity instanceof ArmorStand) {
            ArmorStand armorStand = (ArmorStand) clickedEntity;

            // get chunk that armor stand is in
            location = armorStand.getLocation();
        }
        else if (clickedEntity instanceof ItemFrame) {
            if (MedievalFactions.getInstance().isDebugEnabled()) {
                System.out.println("DEBUG: ItemFrame interaction captured in PlayerInteractAtEntityEvent!");
            }
            ItemFrame itemFrame = (ItemFrame) clickedEntity;

            // get chunk that armor stand is in
            location = itemFrame.getLocation();
        }

        if (location != null) {
            Chunk chunk = location.getChunk();
            ClaimedChunk claimedChunk = PersistentData.getInstance().getChunkDataAccessor().getClaimedChunk(chunk);

            if (InteractionAccessChecker.getInstance().shouldEventBeCancelled(claimedChunk, player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler()
    public void handle(HangingBreakByEntityEvent event) {
        if (!(event.getRemover() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getRemover();

        Entity entity = event.getEntity();

        // get chunk that entity is in
        ClaimedChunk claimedChunk = PersistentData.getInstance().getChunkDataAccessor().getClaimedChunk(entity.getLocation().getChunk());

        if (InteractionAccessChecker.getInstance().shouldEventBeCancelled(claimedChunk, player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler()
    public void handle(PlayerBucketFillEvent event) {
        if (MedievalFactions.getInstance().isDebugEnabled()) { System.out.println("DEBUG: A player is attempting to fill a bucket!"); }

        Player player = event.getPlayer();

        Block clickedBlock = event.getBlockClicked();

        ClaimedChunk claimedChunk = PersistentData.getInstance().getChunkDataAccessor().getClaimedChunk(clickedBlock.getChunk());

        if (InteractionAccessChecker.getInstance().shouldEventBeCancelled(claimedChunk, player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler()
    public void handle(PlayerBucketEmptyEvent event) {
        if (MedievalFactions.getInstance().isDebugEnabled()) { System.out.println("DEBUG: A player is attempting to empty a bucket!"); }

        Player player = event.getPlayer();

        Block clickedBlock = event.getBlockClicked();

        ClaimedChunk claimedChunk = PersistentData.getInstance().getChunkDataAccessor().getClaimedChunk(clickedBlock.getChunk());

        if (InteractionAccessChecker.getInstance().shouldEventBeCancelled(claimedChunk, player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler()
    public void handle(EntityPlaceEvent event) {
        if (MedievalFactions.getInstance().isDebugEnabled()) { System.out.println("DEBUG: A player is attempting to place an entity!"); }

        Player player = event.getPlayer();

        Block clickedBlock = event.getBlock();

        ClaimedChunk claimedChunk = PersistentData.getInstance().getChunkDataAccessor().getClaimedChunk(clickedBlock.getChunk());

        if (InteractionAccessChecker.getInstance().shouldEventBeCancelled(claimedChunk, player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler()
    public void handle(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity clickedEntity = event.getRightClicked();

        if (clickedEntity instanceof ItemFrame) {
            if (MedievalFactions.getInstance().isDebugEnabled()) {
                System.out.println("DEBUG: ItemFrame interaction captured in PlayerInteractEntityEvent!");
            }
            ItemFrame itemFrame = (ItemFrame) clickedEntity;

            // get chunk that armor stand is in
            Location location = itemFrame.getLocation();
            Chunk chunk = location.getChunk();
            ClaimedChunk claimedChunk = PersistentData.getInstance().getChunkDataAccessor().getClaimedChunk(chunk);

            if (InteractionAccessChecker.getInstance().shouldEventBeCancelled(claimedChunk, player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler()
    public void handle(BlockRedstoneEvent event) {
        Block block = event.getBlock();
        LocalGateService.getInstance().handlePotentialGateInteraction(block, event);
    }

    private boolean isPlayerUsingAnAccessCommand(Player player) {
        return EphemeralData.getInstance().getPlayersGrantingAccess().containsKey(player.getUniqueId()) ||
                EphemeralData.getInstance().getPlayersCheckingAccess().contains(player.getUniqueId()) ||
                EphemeralData.getInstance().getPlayersRevokingAccess().containsKey(player.getUniqueId());
    }
}