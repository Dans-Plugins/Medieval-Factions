package dansplugins.factionsystem.eventhandlers;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.managers.ChunkManager;
import dansplugins.factionsystem.managers.GateManager;
import dansplugins.factionsystem.managers.LocaleManager;
import dansplugins.factionsystem.managers.LockManager;
import dansplugins.factionsystem.objects.ClaimedChunk;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.objects.Gate;
import dansplugins.factionsystem.objects.LockedBlock;
import dansplugins.factionsystem.utils.BlockChecker;
import dansplugins.factionsystem.utils.InteractionAccessChecker;
import dansplugins.factionsystem.utils.UUIDChecker;
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
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.InventoryHolder;

public class InteractionHandler implements Listener {

    // EVENT HANDLER METHODS ------------------------------------------------------

    @EventHandler()
    public void handle(BlockBreakEvent event) {
        // get player
        Player player = event.getPlayer();

        // get chunk
        ClaimedChunk claimedChunk = ChunkManager.getInstance().getClaimedChunk(event.getBlock().getLocation().getChunk());

        if (InteractionAccessChecker.getInstance().shouldEventBeCancelled(claimedChunk, player)) {
            event.setCancelled(true);
            return;
        }

        // if block is in a gate
        for (Faction faction : PersistentData.getInstance().getFactions()) {
            for (Gate gate : faction.getGates()) {
                if (gate.hasBlock(event.getBlock())) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("BlockIsPartOfGateMustRemoveGate"), gate.getName()));
                    return;
                }
            }
        }
        
        // if block is not locked then return
        if (!PersistentData.getInstance().isBlockLocked(event.getBlock())) {
            return;
        }
        else {
            // block is locked
            boolean isOwner = PersistentData.getInstance().getLockedBlock(event.getBlock()).getOwner().equals(player.getUniqueId());
            if (!isOwner) {
                // player is not the owner and isn't bypassing
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertNonOwnership"));
                return;
            }

            LockManager.getInstance().removeLock(event.getBlock());
            // if block was a door
            if (BlockChecker.getInstance().isDoor(event.getBlock())) {
                // remove locks above and below the original block as well
                Block relativeUp = event.getBlock().getRelative(BlockFace.UP);
                Block relativeDown = event.getBlock().getRelative(BlockFace.DOWN);
                if (BlockChecker.getInstance().isDoor(relativeUp)) {
                    LockManager.getInstance().removeLock(relativeUp);
                }
                if (BlockChecker.getInstance().isDoor(relativeDown)) {
                    LockManager.getInstance().removeLock(relativeDown);
                }
                return;
            }
        }
    }

    @EventHandler()
    public void handle(BlockPlaceEvent event) {
        // get player
        Player player = event.getPlayer();

        // get chunk
        ClaimedChunk claimedChunk = ChunkManager.getInstance().getClaimedChunk(event.getBlock().getLocation().getChunk());

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
                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CannotPlaceChestsNextToUnownedLockedChests"));
                event.setCancelled(true);
                return;
            }

            int seconds = 2;
            MedievalFactions.getInstance().getServer().getScheduler().runTaskLater(MedievalFactions.getInstance(), new Runnable() {
                @Override
                public void run() {
                    Block block = player.getWorld().getBlockAt(event.getBlock().getLocation());

                    if (!BlockChecker.getInstance().isChest(block)) {
                        // There has been 2 seconds since we last confirmed this was a chest, double-checking isn't ever bad :)
                        return;
                    }

                    InventoryHolder holder = ((Chest) block.getState()).getInventory().getHolder();
                    if (holder instanceof DoubleChest) {
                        // make sure both sides are locked
                        DoubleChest doubleChest = (DoubleChest) holder;
                        Block leftChest = ((Chest) doubleChest.getLeftSide()).getBlock();
                        Block rightChest = ((Chest) doubleChest.getRightSide()).getBlock();

                        if (PersistentData.getInstance().isBlockLocked(leftChest)) {
                            // lock right chest
                            LockedBlock right = new LockedBlock(player.getUniqueId(), PersistentData.getInstance().getPlayersFaction(player.getUniqueId()).getName(), rightChest.getX(), rightChest.getY(), rightChest.getZ(), rightChest.getWorld().getName());
                            PersistentData.getInstance().getLockedBlocks().add(right);
                        }
                        else {
                            if (PersistentData.getInstance().isBlockLocked(rightChest)) {
                                // lock left chest
                                LockedBlock left = new LockedBlock(player.getUniqueId(), PersistentData.getInstance().getPlayersFaction(player.getUniqueId()).getName(), leftChest.getX(), leftChest.getY(), leftChest.getZ(), leftChest.getWorld().getName());
                                PersistentData.getInstance().getLockedBlocks().add(left);
                            }
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
                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CannotPlaceHoppersNextToUnownedLockedChests"));
                return;
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
            LockManager.getInstance().handleLockingBlock(event, player, clickedBlock);
        }

        // ---------------------------------------------------------------------------------------------------------------

        // if player is trying to unlock a block
        if (EphemeralData.getInstance().getUnlockingPlayers().contains(player.getUniqueId())) {
            LockManager.getInstance().handleUnlockingBlock(event, player, clickedBlock);
        }

        // ---------------------------------------------------------------------------------------------------------------
        
        LockedBlock lockedBlock = PersistentData.getInstance().getLockedBlock(clickedBlock);
        if (lockedBlock != null) {
            boolean playerHasAccess = lockedBlock.hasAccess(player.getUniqueId());
            boolean isPlayerBypassing = EphemeralData.getInstance().getAdminsBypassingProtections().contains(player.getUniqueId());
            if (!playerHasAccess && !isPlayerBypassing) {
                // player doesn't have access and isn't overriding
                String owner = UUIDChecker.getInstance().findPlayerNameBasedOnUUID(lockedBlock.getOwner());
                player.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("LockedBy"), owner));
                event.setCancelled(true);
                return;
            }
            
            if (EphemeralData.getInstance().getPlayersGrantingAccess().containsKey(player.getUniqueId())) {
                // player is trying to grant access
                LockManager.getInstance().handleGrantingAccess(event, clickedBlock, player);
            }
            
            if (EphemeralData.getInstance().getPlayersCheckingAccess().contains(player.getUniqueId())) {
                // player is trying to check access
                LockManager.getInstance().handleCheckingAccess(event, lockedBlock, player);
            }
            
            if (EphemeralData.getInstance().getPlayersRevokingAccess().containsKey(player.getUniqueId())) {
                // player is trying to revoke access
                LockManager.getInstance().handleRevokingAccess(event, clickedBlock, player);
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
                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("BlockIsNotLocked"));
            }
        }

        // Check if it's a lever, and if it is and it's connected to a gate in the faction
        // territory then open/close the gate.
        boolean playerClickedLever = clickedBlock.getType().equals(Material.LEVER);
        if (playerClickedLever) {
            GateManager.getInstance().handlePotentialGateInteraction(clickedBlock, player, event);
        }

        // ---------------------------------------------------------------------------------------------------------------

        // pgarner Sep 2, 2020: Moved this to after test to see if the block is locked because it could be a block they have been granted
        // access to (or in future, a 'public' locked block), so if they're not in the faction whose territory the block exists in we want that
        // check to be handled before the interaction is rejected for not being a faction member.
        // if chunk is claimed
        ClaimedChunk chunk = ChunkManager.getInstance().getClaimedChunk(event.getClickedBlock().getLocation().getChunk());
        if (chunk != null) {
            ChunkManager.getInstance().handleClaimedChunkInteraction(event, chunk);
        }

        // ---------------------------------------------------------------------------------------------------------------

        // get tool in player's hand, if it's the gate tool
        // then we want to let them create the gate.
        boolean playerCreatingGate = EphemeralData.getInstance().getCreatingGatePlayers().containsKey(event.getPlayer().getUniqueId());
        boolean playerHoldingGoldenHoe = player.getInventory().getItemInMainHand().getType().equals(Material.GOLDEN_HOE);
        if (playerCreatingGate && playerHoldingGoldenHoe) {
            GateManager.getInstance().handleCreatingGate(clickedBlock, player, event);
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
            ClaimedChunk claimedChunk = ChunkManager.getInstance().getClaimedChunk(chunk);

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
        ClaimedChunk claimedChunk = ChunkManager.getInstance().getClaimedChunk(entity.getLocation().getChunk());

        if (InteractionAccessChecker.getInstance().shouldEventBeCancelled(claimedChunk, player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler()
    public void handle(PlayerBucketFillEvent event) {
        if (MedievalFactions.getInstance().isDebugEnabled()) { System.out.println("DEBUG: A player is attempting to fill a bucket!"); }

        Player player = event.getPlayer();

        Block clickedBlock = event.getBlockClicked();

        ClaimedChunk claimedChunk = ChunkManager.getInstance().getClaimedChunk(clickedBlock.getChunk());

        if (InteractionAccessChecker.getInstance().shouldEventBeCancelled(claimedChunk, player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler()
    public void handle(PlayerBucketEmptyEvent event) {
        if (MedievalFactions.getInstance().isDebugEnabled()) { System.out.println("DEBUG: A player is attempting to empty a bucket!"); }

        Player player = event.getPlayer();

        Block clickedBlock = event.getBlockClicked();

        ClaimedChunk claimedChunk = ChunkManager.getInstance().getClaimedChunk(clickedBlock.getChunk());

        if (InteractionAccessChecker.getInstance().shouldEventBeCancelled(claimedChunk, player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler()
    public void handle(EntityPlaceEvent event) {
        if (MedievalFactions.getInstance().isDebugEnabled()) { System.out.println("DEBUG: A player is attempting to place an entity!"); }

        Player player = event.getPlayer();

        Block clickedBlock = event.getBlock();

        ClaimedChunk claimedChunk = ChunkManager.getInstance().getClaimedChunk(clickedBlock.getChunk());

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
            ClaimedChunk claimedChunk = ChunkManager.getInstance().getClaimedChunk(chunk);

            if (InteractionAccessChecker.getInstance().shouldEventBeCancelled(claimedChunk, player)) {
                event.setCancelled(true);
            }
        }
    }

    // END OF EVENT HANDLER METHODS ------------------------------------------------------

    // HELPER METHODS ------------------------------------------------------

    private boolean isPlayerUsingAnAccessCommand(Player player) {
        return EphemeralData.getInstance().getPlayersGrantingAccess().containsKey(player.getUniqueId()) ||
                EphemeralData.getInstance().getPlayersCheckingAccess().contains(player.getUniqueId()) ||
                EphemeralData.getInstance().getPlayersRevokingAccess().containsKey(player.getUniqueId());
    }

    // END OF HELPER METHODS ------------------------------------------------------

}