package dansplugins.factionsystem.eventhandlers;

import dansplugins.factionsystem.*;
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
import dansplugins.factionsystem.utils.UUIDChecker;
import org.bukkit.*;
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
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;

import static org.bukkit.Material.LADDER;

public class InteractionHandler implements Listener {

    private final boolean debug = true;

    // EVENT HANDLER METHODS ------------------------------------------------------

    @EventHandler()
    public void handle(BlockBreakEvent event) {
        // get player
        Player player = event.getPlayer();

        // get chunk
        ClaimedChunk chunk = ChunkManager.getInstance().getClaimedChunk(event.getBlock().getLocation().getChunk().getX(), event.getBlock().getLocation().getChunk().getZ(), event.getBlock().getWorld().getName(), PersistentData.getInstance().getClaimedChunks());

        // if chunk is not claimed then return
        if (chunk == null) {
            return;
        }

        boolean isPlayerBypassing = EphemeralData.getInstance().getAdminsBypassingProtections().contains(event.getPlayer().getUniqueId());
        
        Faction faction = PersistentData.getInstance().getPlayersFaction(event.getPlayer().getUniqueId());
        if (faction == null && !isPlayerBypassing) {
            // player not in a faction
            event.setCancelled(true);
            return;
        }
        else {
            // player is in faction
            boolean isLandClaimedByPlayersFaction = faction.getName().equalsIgnoreCase(chunk.getHolder());
            if (!isLandClaimedByPlayersFaction) {
                // player's faction is not the same as the holder of the chunk and player isn't bypassing
                if (!isOutsiderInteractionAllowed(player, chunk, faction) && !isPlayerBypassing) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
        
        // if block is not locked then return
        if (PersistentData.getInstance().isBlockLocked(event.getBlock())) {
            return;
        }
        else {
            // block is locked
            boolean isOwner = PersistentData.getInstance().getLockedBlock(event.getBlock()).getOwner().equals(player.getUniqueId());
            if (!isOwner && !isPlayerBypassing) {
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

        // if block is in a gate
        for (Gate gate : faction.getGates()) {
            if (gate.hasBlock(event.getBlock())) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("BlockIsPartOfGateMustRemoveGate"), gate.getName()));
                return;
            }
        }
    }

    @EventHandler()
    public void handle(BlockPlaceEvent event) {
        // get player
        Player player = event.getPlayer();

        // get chunk
        ClaimedChunk chunk = ChunkManager.getInstance().getClaimedChunk(event.getBlock().getLocation().getChunk());

        // if chunk is not claimed then return
        if (chunk == null) {
            return;
        }

        // if player not in a faction and isn't bypassing then cancel event and return
        boolean isPlayerInFaction = PersistentData.getInstance().isInFaction(player.getUniqueId());
        boolean isPlayerBypassing = EphemeralData.getInstance().getAdminsBypassingProtections().contains(player.getUniqueId());
        if (!isPlayerInFaction && !isPlayerBypassing) {
            event.setCancelled(true);
            return;
        }
        
        // player is in faction
        // player is not bypassing

        Faction faction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());

        boolean isLandClaimedByPlayersFaction = faction.getName().equalsIgnoreCase(chunk.getHolder());
        if (!isLandClaimedByPlayersFaction) {
            boolean laddersArePlaceableInEnemyTerritory = MedievalFactions.getInstance().getConfig().getBoolean("laddersPlaceableInEnemyFactionTerritory");
            boolean playerIsTryingToPlaceLadderInEnemyTerritory = event.getBlockPlaced().getType() == LADDER && faction.isEnemy(chunk.getHolder());
            if (laddersArePlaceableInEnemyTerritory && playerIsTryingToPlaceLadderInEnemyTerritory) {
                // allow interaction
                return;
            }

            if (!isOutsiderInteractionAllowed(player, chunk, faction) && !isPlayerBypassing) {
                event.setCancelled(true);
                return;
            }
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
            if (debug) { System.out.println("DEBUG: ItemFrame interaction captured in PlayerInteractEvent!"); }
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
        ClaimedChunk chunk = ChunkManager.getInstance().getClaimedChunk(event.getClickedBlock().getLocation().getChunk().getX(), event.getClickedBlock().getLocation().getChunk().getZ(), event.getClickedBlock().getWorld().getName(), PersistentData.getInstance().getClaimedChunks());
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

        World world = null;
        Location location = null;

        if (clickedEntity instanceof ArmorStand) {
            ArmorStand armorStand = (ArmorStand) clickedEntity;

            // get chunk that armor stand is in
            world = armorStand.getWorld();
            location = armorStand.getLocation();
        }
        else if (clickedEntity instanceof ItemFrame) {
            if (debug) {
                System.out.println("DEBUG: ItemFrame interaction captured in PlayerInteractAtEntityEvent!");
            }
            ItemFrame itemFrame = (ItemFrame) clickedEntity;

            // get chunk that armor stand is in
            world = itemFrame.getWorld();
            location = itemFrame.getLocation();
        }

        if (location != null && world != null) {
            Chunk chunk = location.getChunk();
            ClaimedChunk claimedChunk = ChunkManager.getInstance().getClaimedChunk(chunk.getX(), chunk.getZ(), world.getName(), PersistentData.getInstance().getClaimedChunks());

            if (shouldEventBeCancelled(claimedChunk, player)) {
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

        if (shouldEventBeCancelled(claimedChunk, player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler()
    public void handle(PlayerBucketFillEvent event) {
        if (debug) { System.out.println("DEBUG: A player is attempting to fill a bucket!"); }

        Player player = event.getPlayer();

        Block clickedBlock = event.getBlockClicked();

        ClaimedChunk claimedChunk = ChunkManager.getInstance().getClaimedChunk(clickedBlock.getChunk());

        if (shouldEventBeCancelled(claimedChunk, player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler()
    public void handle(PlayerBucketEmptyEvent event) {
        if (debug) { System.out.println("DEBUG: A player is attempting to empty a bucket!"); }

        Player player = event.getPlayer();

        Block clickedBlock = event.getBlockClicked();

        ClaimedChunk claimedChunk = ChunkManager.getInstance().getClaimedChunk(clickedBlock.getChunk());

        if (shouldEventBeCancelled(claimedChunk, player)) {
            event.setCancelled(true);
        }
    }

    // END OF EVENT HANDLER METHODS ------------------------------------------------------

    // HELPER METHODS ------------------------------------------------------

    private boolean isPlayerUsingAnAccessCommand(Player player) {
        return EphemeralData.getInstance().getPlayersGrantingAccess().containsKey(player.getUniqueId()) ||
                EphemeralData.getInstance().getPlayersCheckingAccess().contains(player.getUniqueId()) ||
                EphemeralData.getInstance().getPlayersRevokingAccess().containsKey(player.getUniqueId());
    }

    private boolean shouldEventBeCancelled(ClaimedChunk claimedChunk, Player player) {
        if (claimedChunk == null) {
            return false;
        }

        Faction playersFaction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());

        boolean isPlayerBypassing = EphemeralData.getInstance().getAdminsBypassingProtections().contains(player.getUniqueId());

        if (playersFaction == null && !isPlayerBypassing) {
            return true;
        }

        boolean isLandClaimedByPlayersFaction = playersFaction.getName().equalsIgnoreCase(claimedChunk.getHolder());
        if (!isLandClaimedByPlayersFaction) {
            if (!isOutsiderInteractionAllowed(player, claimedChunk, playersFaction) && !isPlayerBypassing) {
                return true;
            }
        }

        return false;
    }

    private boolean isOutsiderInteractionAllowed(Player player, ClaimedChunk chunk, Faction faction) {
        boolean inVassalageTree = PersistentData.getInstance().isPlayerInFactionInVassalageTree(player, PersistentData.getInstance().getFaction(chunk.getHolder()));
        boolean isAlly = faction.isAlly(chunk.getHolder());
        boolean allyInteractionAllowed = MedievalFactions.getInstance().getConfig().getBoolean("allowAllyInteraction");
        boolean vassalageTreeInteractionAllowed = MedievalFactions.getInstance().getConfig().getBoolean("allowVassalageTreeInteraction");

        boolean allowed = false;

        if (allyInteractionAllowed && isAlly) {
            allowed = true;
        }

        if (vassalageTreeInteractionAllowed && inVassalageTree) {
            allowed = true;
        }

        return allowed;
    }

    // END OF HELPER METHODS ------------------------------------------------------

}