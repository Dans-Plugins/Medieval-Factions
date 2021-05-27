package dansplugins.factionsystem.eventhandlers;

import dansplugins.factionsystem.ChunkManager;
import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
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
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.UUID;

import static org.bukkit.Material.LADDER;

public class InteractionHandler implements Listener {

    private final boolean debug = false;

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
        if (faction == null) {
            // player not in a faction
            event.setCancelled(true);
            return;
        }
        else {
            // player is in faction
            boolean isLandClaimedByPlayersFaction = faction.getName().equalsIgnoreCase(chunk.getHolder());
            if (!isLandClaimedByPlayersFaction && !isPlayerBypassing) {
                // player's faction is not the same as the holder of the chunk and player isn't bypassing
                if (!isInteractionAllowed(player, chunk, faction)) {
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

            removeLock(event.getBlock(), PersistentData.getInstance().getLockedBlocks());
            // if block was a door
            if (BlockChecker.getInstance().isDoor(event.getBlock())) {
                // remove locks above and below the original block as well
                Block relativeUp = event.getBlock().getRelative(BlockFace.UP);
                Block relativeDown = event.getBlock().getRelative(BlockFace.DOWN);
                if (BlockChecker.getInstance().isDoor(relativeUp)) {
                    removeLock(relativeUp);
                }
                if (BlockChecker.getInstance().isDoor(relativeDown)) {
                    removeLock(relativeDown);
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

            if (!isInteractionAllowed(player, chunk, faction)) {
                event.setCancelled(true);
                return;
            }
        }

        if (BlockChecker.getInstance().isChest(event.getBlock())) {
            boolean isNextToNonOwnedLockedChest = isNextToNonOwnedLockedChest(event.getPlayer(), event.getBlock());
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
            boolean isNextToNonOwnedLockedChest = isNextToNonOwnedLockedChest(event.getPlayer(), event.getBlock());
            boolean isUnderOrAboveNonOwnedLockedChest = isUnderOrAboveNonOwnedLockedChest(event.getPlayer(), event.getBlock());
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
            handleLockingBlock(event, player, clickedBlock);
        }

        // ---------------------------------------------------------------------------------------------------------------

        // if player is trying to unlock a block
        if (EphemeralData.getInstance().getUnlockingPlayers().contains(player.getUniqueId())) {
            handleUnlockingBlock(event, player, clickedBlock);
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
                handleGrantingAccess(event, clickedBlock, player);
            }
            
            if (EphemeralData.getInstance().getPlayersCheckingAccess().contains(player.getUniqueId())) {
                // player is trying to check access
                handleCheckingAccess(event, lockedBlock, player);
            }
            
            if (EphemeralData.getInstance().getPlayersRevokingAccess().containsKey(player.getUniqueId())) {
                // player is trying to revoke access
                handleRevokingAccess(event, clickedBlock, player);
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
            handlePotentialGateInteraction(clickedBlock, player, event);
        }

        // ---------------------------------------------------------------------------------------------------------------

        // pgarner Sep 2, 2020: Moved this to after test to see if the block is locked because it could be a block they have been granted
        // access to (or in future, a 'public' locked block), so if they're not in the faction whose territory the block exists in we want that
        // check to be handled before the interaction is rejected for not being a faction member.
        // if chunk is claimed
        ClaimedChunk chunk = ChunkManager.getInstance().getClaimedChunk(event.getClickedBlock().getLocation().getChunk().getX(), event.getClickedBlock().getLocation().getChunk().getZ(), event.getClickedBlock().getWorld().getName(), PersistentData.getInstance().getClaimedChunks());
        if (chunk != null) {
            handleClaimedChunk(event, chunk);
        }

        // ---------------------------------------------------------------------------------------------------------------

        // get tool in player's hand, if it's the gate tool
        // then we want to let them create the gate.
        boolean playerCreatingGate = EphemeralData.getInstance().getCreatingGatePlayers().containsKey(event.getPlayer().getUniqueId());
        boolean playerHoldingGoldenHoe = player.getInventory().getItemInMainHand().getType().equals(Material.GOLDEN_HOE);
        if (playerCreatingGate && playerHoldingGoldenHoe) {
            handleCreatingGate(clickedBlock, player, event);
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

            // if chunk is not claimed, return
            if (claimedChunk == null) {
                return;
            }

            String holderFactionName = claimedChunk.getHolder();

            Faction playersFaction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());

            if (playersFaction == null) {
                return;
            }

            String playersFactionName = playersFaction.getName();

            // if holder is not the same as player's faction
            if (!holderFactionName.equalsIgnoreCase(playersFactionName)) {
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
        World world = entity.getWorld();
        Location location = entity.getLocation();
        Chunk chunk = location.getChunk();
        ClaimedChunk claimedChunk = ChunkManager.getInstance().getClaimedChunk(chunk.getX(), chunk.getZ(), world.getName(), PersistentData.getInstance().getClaimedChunks());

        // if chunk is not claimed, return
        if (claimedChunk == null) {
            return;
        }

        String holderFactionName = claimedChunk.getHolder();

        Faction playersFaction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());

        if (playersFaction == null) {
            return;
        }

        String playersFactionName = playersFaction.getName();

        // if holder is not the same as player's faction
        if (!holderFactionName.equalsIgnoreCase(playersFactionName)) {
            event.setCancelled(true);
        }
    }

    // END OF EVENT HANDLER METHODS ------------------------------------------------------

    // HELPER METHODS ------------------------------------------------------

    private void handlePotentialGateInteraction(Block clickedBlock, Player player, PlayerInteractEvent event) {
        if (ChunkManager.getInstance().isClaimed(clickedBlock.getChunk(), PersistentData.getInstance().getClaimedChunks())) {
            ClaimedChunk claim = ChunkManager.getInstance().getClaimedChunk(clickedBlock.getChunk().getX(), clickedBlock.getChunk().getZ(),
                    clickedBlock.getChunk().getWorld().getName(), PersistentData.getInstance().getClaimedChunks());
            Faction faction = PersistentData.getInstance().getFaction(claim.getHolder());

            if (faction.hasGateTrigger(clickedBlock)) {
                for (Gate g : faction.getGatesForTrigger(clickedBlock)) {
                    BlockData blockData = clickedBlock.getBlockData();
                    Powerable powerable = (Powerable) blockData;
                    if (powerable.isPowered()) {
                        if (faction.getGatesForTrigger(clickedBlock).get(0).isReady()) {
                            g.openGate();
                        }
                        else {
                            event.setCancelled(true);
                            player.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PleaseWaitGate"), g.getStatus()));
                            return;
                        }
                    }
                    else {
                        if (faction.getGatesForTrigger(clickedBlock).get(0).isReady()) {
                            g.closeGate();
                        }
                        else {
                            event.setCancelled(true);
                            player.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PleaseWaitGate"), g.getStatus()));
                            return;
                        }
                    }
                }
                return;
            }
        }
    }

    private void handleCreatingGate(Block clickedBlock, Player player, PlayerInteractEvent event) {
        if (!ChunkManager.getInstance().isClaimed(clickedBlock.getChunk(), PersistentData.getInstance().getClaimedChunks())) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CanOnlyCreateGatesInClaimedTerritory"));
            return;
        }
        else {
            ClaimedChunk claimedChunk = ChunkManager.getInstance().getClaimedChunk(clickedBlock.getChunk().getX(), clickedBlock.getChunk().getZ(), clickedBlock.getWorld().getName(), PersistentData.getInstance().getClaimedChunks());
            if (claimedChunk != null) {
                if (!PersistentData.getInstance().getFaction(claimedChunk.getHolder()).isMember(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeMemberToCreateGate"));
                    return;
                }
                else {
                    if (!PersistentData.getInstance().getFaction(claimedChunk.getHolder()).isOwner(player.getUniqueId())
                            && !PersistentData.getInstance().getFaction(claimedChunk.getHolder()).isOfficer(player.getUniqueId())) {
                        player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeOwnerOrOfficerToCreateGate"));
                        return;
                    }
                }
            }
        }

        if (player.hasPermission("mf.gate")) {
            // TODO: Check if a gate already exists here, and if it does, print out some info
            // of that existing gate instead of trying to create a new one.
            if (EphemeralData.getInstance().getCreatingGatePlayers().containsKey(event.getPlayer().getUniqueId()) && EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getCoord1() == null) {
                Gate.ErrorCodeAddCoord e = EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).addCoord(clickedBlock);
                if (e.equals(Gate.ErrorCodeAddCoord.None)) {
                    event.getPlayer().sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("Point1PlacementSuccessful"));
                    event.getPlayer().sendMessage(ChatColor.YELLOW + LocaleManager.getInstance().getText("ClickToPlaceSecondCorner"));
                    return;
                }
                else if (e.equals(Gate.ErrorCodeAddCoord.MaterialMismatch)) {
                    event.getPlayer().sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("MaterialsMismatch1"));
                    EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                    return;
                }
                else if (e.equals(Gate.ErrorCodeAddCoord.WorldMismatch)) {
                    event.getPlayer().sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("WorldsMismatch1"));
                    EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                    return;
                }
                else if (e.equals(Gate.ErrorCodeAddCoord.NoCuboids)) {
                    event.getPlayer().sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CuboidDisallowed1"));
                    EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                    return;
                }
                else {
                    event.getPlayer().sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CancelledGatePlacement1"));
                    EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                    return;
                }
            }
            else if (EphemeralData.getInstance().getCreatingGatePlayers().containsKey(event.getPlayer().getUniqueId()) && EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getCoord1() != null
                    && EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getCoord2() == null
                    && EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getTrigger() == null) {
                if (!EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getCoord1().equals(clickedBlock)) {
                    Gate.ErrorCodeAddCoord e = EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).addCoord(clickedBlock);
                    if (e.equals(Gate.ErrorCodeAddCoord.None)) {
                        event.getPlayer().sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("Point2PlacedSuccessfully"));
                        event.getPlayer().sendMessage(ChatColor.YELLOW + LocaleManager.getInstance().getText("ClickOnTriggerLever"));
                        return;
                    }
                    else if (e.equals(Gate.ErrorCodeAddCoord.MaterialMismatch)) {
                        event.getPlayer().sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("MaterialsMismatch2"));
                        EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                        return;
                    }
                    else if (e.equals(Gate.ErrorCodeAddCoord.WorldMismatch)) {
                        event.getPlayer().sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("WorldsMismatch2"));
                        EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                        return;
                    }
                    else if (e.equals(Gate.ErrorCodeAddCoord.NoCuboids)) {
                        event.getPlayer().sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CuboidDisallowed2"));
                        EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                        return;
                    }
                    else if (e.equals(Gate.ErrorCodeAddCoord.LessThanThreeHigh)) {
                        event.getPlayer().sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("ThreeBlockRequirement"));
                        EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                        return;
                    }
                    else {
                        event.getPlayer().sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CancelledGatePlacement2"));
                        EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                        return;
                    }
                }
            }
            else if (EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getCoord2() != null
                    && EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getTrigger() == null
                    && !EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getCoord2().equals(clickedBlock)) {
                if (clickedBlock.getType().equals(Material.LEVER)) {
                    if (ChunkManager.getInstance().isClaimed(clickedBlock.getChunk(), PersistentData.getInstance().getClaimedChunks())) {
                        Gate.ErrorCodeAddCoord e = EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).addCoord(clickedBlock);
                        if (e.equals(Gate.ErrorCodeAddCoord.None)) {
                            ClaimedChunk claim = ChunkManager.getInstance().getClaimedChunk(clickedBlock.getChunk().getX(), clickedBlock.getChunk().getZ(),
                                    clickedBlock.getChunk().getWorld().getName(), PersistentData.getInstance().getClaimedChunks());
                            Faction faction = PersistentData.getInstance().getFaction(claim.getHolder());
                            faction.addGate(EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()));
                            EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                            event.getPlayer().sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("LeverSuccessfullyLinked"));
                            event.getPlayer().sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("GateCreated"));
                            return;
                        }
                        else {
                            event.getPlayer().sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CancelledGatePlacementErrorLinking"));
                            EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                            return;
                        }
                    }
                    else {
                        event.getPlayer().sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("OnlyUseLeversInClaimedTerritory"));
                        EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                        return;
                    }
                }
                else {
                    event.getPlayer().sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("TriggerBlockNotLever"));
                    EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                    return;
                }
            }
        }
        else {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("PermissionGate"));
        }
    }

    private void removeLock(Block block, ArrayList<LockedBlock> lockedBlocks) {
        for (LockedBlock b : lockedBlocks) {
            if (b.getX() == block.getX() && b.getY() == block.getY() && b.getZ() == block.getZ() && block.getWorld().getName().equalsIgnoreCase(b.getWorld())) {
                lockedBlocks.remove(b);
                return;
            }
        }
    }

    private boolean isNextToNonOwnedLockedChest(Player player, Block block) {
        // define blocks
        Block neighbor1 = block.getWorld().getBlockAt(block.getX() + 1, block.getY(), block.getZ());
        Block neighbor2 = block.getWorld().getBlockAt(block.getX() - 1, block.getY(), block.getZ());
        Block neighbor3 = block.getWorld().getBlockAt(block.getX(), block.getY(), block.getZ() + 1);
        Block neighbor4 = block.getWorld().getBlockAt(block.getX(), block.getY(), block.getZ() - 1);

        if (BlockChecker.getInstance().isChest(neighbor1)) {
            if (PersistentData.getInstance().isBlockLocked(neighbor1) && PersistentData.getInstance().getLockedBlock(neighbor1).getOwner() != player.getUniqueId()) {
                return true;
            }
        }

        if (BlockChecker.getInstance().isChest(neighbor2)) {
            if (PersistentData.getInstance().isBlockLocked(neighbor2) && PersistentData.getInstance().getLockedBlock(neighbor2).getOwner() != player.getUniqueId()) {
                return true;
            }
        }

        if (BlockChecker.getInstance().isChest(neighbor3)) {
            if (PersistentData.getInstance().isBlockLocked(neighbor3) && PersistentData.getInstance().getLockedBlock(neighbor3).getOwner() != player.getUniqueId()) {
                return true;
            }
        }

        if (BlockChecker.getInstance().isChest(neighbor4)) {
            if (PersistentData.getInstance().isBlockLocked(neighbor4) && PersistentData.getInstance().getLockedBlock(neighbor4).getOwner() != player.getUniqueId()) {
                return true;
            }
        }

        return false;
    }

    private boolean isUnderOrAboveNonOwnedLockedChest(Player player, Block block) {
        // define blocks
        Block neighbor1 = block.getWorld().getBlockAt(block.getX(), block.getY() + 1, block.getZ());
        Block neighbor2 = block.getWorld().getBlockAt(block.getX(), block.getY() - 1, block.getZ());

        if (BlockChecker.getInstance().isChest(neighbor1)) {
            if (PersistentData.getInstance().isBlockLocked(neighbor1) && PersistentData.getInstance().getLockedBlock(neighbor1).getOwner() != player.getUniqueId()) {
                return true;
            }
        }

        if (BlockChecker.getInstance().isChest(neighbor2)) {
            if (PersistentData.getInstance().isBlockLocked(neighbor2) && PersistentData.getInstance().getLockedBlock(neighbor2).getOwner() != player.getUniqueId()) {
                return true;
            }
        }

        return false;
    }

    private void handleLockingBlock(PlayerInteractEvent event, Player player, Block clickedBlock) {
        // if chunk is claimed
        ClaimedChunk chunk = ChunkManager.getInstance().getClaimedChunk(event.getClickedBlock().getLocation().getChunk());
        if (chunk != null) {

            // if claimed by other faction
            if (!chunk.getHolder().equalsIgnoreCase(PersistentData.getInstance().getPlayersFaction(player.getUniqueId()).getName())) {
                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CanOnlyLockInFactionTerritory"));
                event.setCancelled(true);
                return;
            }

            // if already locked
            if (PersistentData.getInstance().isBlockLocked(clickedBlock)) {
                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("BlockAlreadyLocked"));
                event.setCancelled(true);
                return;
            }

            // block type check
            if (BlockChecker.getInstance().isDoor(clickedBlock) || BlockChecker.getInstance().isChest(clickedBlock) || BlockChecker.getInstance().isGate(clickedBlock) || BlockChecker.getInstance().isBarrel(clickedBlock) || BlockChecker.getInstance().isTrapdoor(clickedBlock) || BlockChecker.getInstance().isFurnace(clickedBlock) || BlockChecker.getInstance().isAnvil(clickedBlock)) {

                // specific to chests because they can be single or double.
                if (BlockChecker.getInstance().isChest(clickedBlock)) {
                    InventoryHolder holder = ((Chest) clickedBlock.getState()).getInventory().getHolder();
                    if (holder instanceof DoubleChest) {
                        // chest multi-lock
                        DoubleChest doubleChest = (DoubleChest) holder;
                        Block leftChest = ((Chest) doubleChest.getLeftSide()).getBlock();
                        Block rightChest = ((Chest) doubleChest.getRightSide()).getBlock();

                        LockedBlock left = new LockedBlock(player.getUniqueId(), PersistentData.getInstance().getPlayersFaction(player.getUniqueId()).getName(), leftChest.getX(), leftChest.getY(), leftChest.getZ(), leftChest.getWorld().getName());
                        PersistentData.getInstance().getLockedBlocks().add(left);

                        lock1x1Block(player, rightChest);
                    }
                    else {
                        // lock single chest
                        lock1x1Block(player, clickedBlock);
                    }
                }

                // door multi-lock (specific to doors because they have two block heights but you could have clicked either block).
                if (BlockChecker.getInstance().isDoor(clickedBlock)) {
                    // lock initial block
                    LockedBlock initial = new LockedBlock(player.getUniqueId(), PersistentData.getInstance().getPlayersFaction(player.getUniqueId()).getName(), clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ(), clickedBlock.getWorld().getName());
                    PersistentData.getInstance().getLockedBlocks().add(initial);
                    // check block above
                    if (BlockChecker.getInstance().isDoor(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() + 1, clickedBlock.getZ()))) {
                        LockedBlock newLockedBlock2 = new LockedBlock(player.getUniqueId(), PersistentData.getInstance().getPlayersFaction(player.getUniqueId()).getName(), clickedBlock.getX(), clickedBlock.getY() + 1, clickedBlock.getZ(), clickedBlock.getWorld().getName());
                        PersistentData.getInstance().getLockedBlocks().add(newLockedBlock2);
                    }
                    // check block below
                    if (BlockChecker.getInstance().isDoor(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() - 1, clickedBlock.getZ()))) {
                        LockedBlock newLockedBlock2 = new LockedBlock(player.getUniqueId(), PersistentData.getInstance().getPlayersFaction(player.getUniqueId()).getName(), clickedBlock.getX(), clickedBlock.getY() - 1, clickedBlock.getZ(), clickedBlock.getWorld().getName());
                        PersistentData.getInstance().getLockedBlocks().add(newLockedBlock2);
                    }

                    player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("Locked"));
                    EphemeralData.getInstance().getLockingPlayers().remove(player.getUniqueId());
                }

                // Remainder of lockable blocks are only 1x1 so generic code will suffice.
                if (BlockChecker.getInstance().isGate(clickedBlock) || BlockChecker.getInstance().isBarrel(clickedBlock) || BlockChecker.getInstance().isTrapdoor(clickedBlock) || BlockChecker.getInstance().isFurnace(clickedBlock) || BlockChecker.getInstance().isAnvil(clickedBlock)) {
                    lock1x1Block(player, clickedBlock);
                }

                event.setCancelled(true);
                return;
            }
            else {
                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CanOnlyLockSpecificBlocks"));
                return;
            }

        }
        else {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CanOnlyLockBlocksInClaimedTerritory"));
            event.setCancelled(true);
            return;
        }
    }

    private void lock1x1Block(Player player, Block clickedBlock) {
        LockedBlock block = new LockedBlock(player.getUniqueId(), PersistentData.getInstance().getPlayersFaction(player.getUniqueId()).getName(),
                clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ(), clickedBlock.getWorld().getName());
        PersistentData.getInstance().getLockedBlocks().add(block);
        player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("Locked"));
        EphemeralData.getInstance().getLockingPlayers().remove(player.getUniqueId());
    }

    private void handleUnlockingBlock(PlayerInteractEvent event, Player player, Block clickedBlock) {
        // if locked
        if (PersistentData.getInstance().isBlockLocked(clickedBlock)) {
            if (PersistentData.getInstance().getLockedBlock(clickedBlock).getOwner().equals(player.getUniqueId())) {

                if (BlockChecker.getInstance().isChest(clickedBlock)) {
                    InventoryHolder holder = ((Chest) clickedBlock.getState()).getInventory().getHolder();
                    if (holder instanceof DoubleChest) {
                        // chest multi-unlock
                        DoubleChest doubleChest = (DoubleChest) holder;
                        Block leftChest = ((Chest) doubleChest.getLeftSide()).getBlock();
                        Block rightChest = ((Chest) doubleChest.getRightSide()).getBlock();

                        // unlock leftChest and rightChest
                        removeLock(leftChest);
                        removeLock(rightChest);

                        player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("AlertUnlocked"));
                        EphemeralData.getInstance().getUnlockingPlayers().remove(player.getUniqueId());
                    }
                    else {
                        // unlock single chest
                        removeLock(clickedBlock);
                        player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("AlertUnlocked"));
                        EphemeralData.getInstance().getUnlockingPlayers().remove(player.getUniqueId());
                    }
                }

                // door multi-unlock
                if (BlockChecker.getInstance().isDoor(clickedBlock)) {
                    // unlock initial block
                    removeLock(clickedBlock);
                    // check block above
                    if (BlockChecker.getInstance().isDoor(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() + 1, clickedBlock.getZ()))) {
                        removeLock(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() + 1, clickedBlock.getZ()));
                    }
                    // check block below
                    if (BlockChecker.getInstance().isDoor(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() - 1, clickedBlock.getZ()))) {
                        removeLock(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() - 1, clickedBlock.getZ()));
                    }

                    player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("AlertUnlocked"));
                    EphemeralData.getInstance().getUnlockingPlayers().remove(player.getUniqueId());
                }

                // single block size lock logic.
                if (BlockChecker.getInstance().isGate(clickedBlock) || BlockChecker.getInstance().isBarrel(clickedBlock) || BlockChecker.getInstance().isTrapdoor(clickedBlock) || BlockChecker.getInstance().isFurnace(clickedBlock)) {
                    removeLock(clickedBlock);

                    player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("AlertUnlocked"));
                    EphemeralData.getInstance().getUnlockingPlayers().remove(player.getUniqueId());
                }

                event.setCancelled(true);
                return;
            }
        }
        else {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("BlockIsNotLocked"));
            event.setCancelled(true);
            return;
        }
    }

    private void removeLock(Block block) {
        for (LockedBlock b : PersistentData.getInstance().getLockedBlocks()) {
            if (b.getX() == block.getX() && b.getY() == block.getY() && b.getZ() == block.getZ() && block.getWorld().getName().equalsIgnoreCase(b.getWorld())) {
                PersistentData.getInstance().getLockedBlocks().remove(b);
                return;
            }
        }
    }

    private void handleClaimedChunk(PlayerInteractEvent event, ClaimedChunk chunk) {
        // player not in a faction and isn't overriding
        if (!PersistentData.getInstance().isInFaction(event.getPlayer().getUniqueId()) && !EphemeralData.getInstance().getAdminsBypassingProtections().contains(event.getPlayer().getUniqueId())) {

            Block block = event.getClickedBlock();
            if (MedievalFactions.getInstance().getConfig().getBoolean("nonMembersCanInteractWithDoors") && block != null && BlockChecker.getInstance().isDoor(block)) {
                // allow non-faction members to interact with doors
                return;
            }

            event.setCancelled(true);
        }

        // if player is in faction
        for (Faction faction : PersistentData.getInstance().getFactions()) {
            if (faction.isMember(event.getPlayer().getUniqueId())) {

                // if player's faction is not the same as the holder of the chunk and player isn't overriding
                if (!(faction.getName().equalsIgnoreCase(chunk.getHolder())) && !EphemeralData.getInstance().getAdminsBypassingProtections().contains(event.getPlayer().getUniqueId())) {

                    Block block = event.getClickedBlock();
                    if (MedievalFactions.getInstance().getConfig().getBoolean("nonMembersCanInteractWithDoors") && block != null && BlockChecker.getInstance().isDoor(block)) {
                        // allow non-faction members to interact with doors
                        return;
                    }

                    // if enemy territory
                    if (faction.isEnemy(chunk.getHolder())) {
                        // if not interacting with chest
                        if (isBlockInteractable(event)) {
                            // allow placing ladders
                            if (MedievalFactions.getInstance().getConfig().getBoolean("laddersPlaceableInEnemyFactionTerritory")) {
                                if (event.getMaterial() == LADDER) {
                                    return;
                                }
                            }
                            // allow eating
                            if (materialAllowed(event.getMaterial())) {
                                return;
                            }
                            // allow blocking
                            if (event.getPlayer().getInventory().getItemInOffHand().getType() == Material.SHIELD) {
                                return;
                            }
                        }
                    }
                    boolean inVassalageTree = PersistentData.getInstance().isPlayerInFactionInVassalageTree(event.getPlayer(), PersistentData.getInstance().getFaction(chunk.getHolder()));
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

                    if (!allowed) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    private boolean isBlockInteractable(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null) {
            // CHEST
            if (BlockChecker.getInstance().isChest(event.getClickedBlock())) {
                return false;
            }
            switch(event.getClickedBlock().getType()) {
                case ACACIA_DOOR:
                case BIRCH_DOOR:
                case DARK_OAK_DOOR:
                case IRON_DOOR:
                case JUNGLE_DOOR:
                case OAK_DOOR:
                case SPRUCE_DOOR:
                case ACACIA_TRAPDOOR:
                case BIRCH_TRAPDOOR:
                case DARK_OAK_TRAPDOOR:
                case IRON_TRAPDOOR:
                case JUNGLE_TRAPDOOR:
                case OAK_TRAPDOOR:
                case SPRUCE_TRAPDOOR:
                case ACACIA_FENCE_GATE:
                case BIRCH_FENCE_GATE:
                case DARK_OAK_FENCE_GATE:
                case JUNGLE_FENCE_GATE:
                case OAK_FENCE_GATE:
                case SPRUCE_FENCE_GATE:
                case BARREL:
                case LEVER:
                case ACACIA_BUTTON:
                case BIRCH_BUTTON:
                case DARK_OAK_BUTTON:
                case JUNGLE_BUTTON:
                case OAK_BUTTON:
                case SPRUCE_BUTTON:
                case STONE_BUTTON:
                case LECTERN:
                    return false;
            }
        }
        return true;
    }

    private boolean materialAllowed(Material material) {
        switch(material) {
            case BREAD:
            case POTATO:
            case CARROT:
            case BEETROOT:
            case BEEF:
            case PORKCHOP:
            case CHICKEN:
            case COD:
            case SALMON:
            case MUTTON:
            case RABBIT:
            case TROPICAL_FISH:
            case PUFFERFISH:
            case MUSHROOM_STEW:
            case RABBIT_STEW:
            case BEETROOT_SOUP:
            case COOKED_BEEF:
            case COOKED_PORKCHOP:
            case COOKED_CHICKEN:
            case COOKED_SALMON:
            case COOKED_MUTTON:
            case COOKED_COD:
            case MELON:
            case PUMPKIN:
            case MELON_SLICE:
            case CAKE:
            case PUMPKIN_PIE:
            case APPLE:
            case COOKIE:
            case POISONOUS_POTATO:
            case CHORUS_FRUIT:
            case DRIED_KELP:
            case BAKED_POTATO:
                return true;
        }
        return false;
    }

    private void handleGrantingAccess(PlayerInteractEvent event, Block clickedBlock, Player player) {

        // if not owner
        if (PersistentData.getInstance().getLockedBlock(clickedBlock).getOwner() != player.getUniqueId()) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("NotTheOwnerOfThisBlock"));
            return;
        }

        // if chest
        if (BlockChecker.getInstance().isChest(clickedBlock)) {
            InventoryHolder holder = ((Chest) clickedBlock.getState()).getInventory().getHolder();
            if (holder instanceof DoubleChest) { // if double chest
                // grant access to both chests
                DoubleChest doubleChest = (DoubleChest) holder;
                Block leftChest = ((Chest) doubleChest.getLeftSide()).getBlock();
                Block rightChest = ((Chest) doubleChest.getRightSide()).getBlock();

                PersistentData.getInstance().getLockedBlock(leftChest).addToAccessList(EphemeralData.getInstance().getPlayersGrantingAccess().get(player.getUniqueId()));
                PersistentData.getInstance().getLockedBlock(rightChest).addToAccessList(EphemeralData.getInstance().getPlayersGrantingAccess().get(player.getUniqueId()));

                player.sendMessage(ChatColor.GREEN + String.format(LocaleManager.getInstance().getText("AlertAccessGrantedTo"), UUIDChecker.getInstance().findPlayerNameBasedOnUUID(EphemeralData.getInstance().getPlayersGrantingAccess().get(player.getUniqueId()))));
                EphemeralData.getInstance().getPlayersGrantingAccess().remove(player.getUniqueId());
            }
            else { // if single chest
                // grant access to single chest
                PersistentData.getInstance().getLockedBlock(clickedBlock).addToAccessList(EphemeralData.getInstance().getPlayersGrantingAccess().get(player.getUniqueId()));
                player.sendMessage(ChatColor.GREEN + String.format(LocaleManager.getInstance().getText("AlertAccessGrantedTo"), UUIDChecker.getInstance().findPlayerNameBasedOnUUID(EphemeralData.getInstance().getPlayersGrantingAccess().get(player.getUniqueId()))));
                EphemeralData.getInstance().getPlayersGrantingAccess().remove(player.getUniqueId());
            }

        }

        // if door
        if (BlockChecker.getInstance().isDoor(clickedBlock)) {
            // grant access to initial block
            PersistentData.getInstance().getLockedBlock(clickedBlock).addToAccessList(EphemeralData.getInstance().getPlayersGrantingAccess().get(player.getUniqueId()));
            // check block above
            if (BlockChecker.getInstance().isDoor(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() + 1, clickedBlock.getZ()))) {
                PersistentData.getInstance().getLockedBlock(clickedBlock).addToAccessList(EphemeralData.getInstance().getPlayersGrantingAccess().get(player.getUniqueId()));
            }
            // check block below
            if (BlockChecker.getInstance().isDoor(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() - 1, clickedBlock.getZ()))) {
                PersistentData.getInstance().getLockedBlock(clickedBlock).addToAccessList(EphemeralData.getInstance().getPlayersGrantingAccess().get(player.getUniqueId()));
            }

            player.sendMessage(ChatColor.GREEN + String.format(LocaleManager.getInstance().getText("AlertAccessGrantedTo"), UUIDChecker.getInstance().findPlayerNameBasedOnUUID(EphemeralData.getInstance().getPlayersGrantingAccess().get(player.getUniqueId()))));
            EphemeralData.getInstance().getPlayersGrantingAccess().remove(player.getUniqueId());
        }

        // if gate (or single-block sized lock)
        if (BlockChecker.getInstance().isGate(clickedBlock) || BlockChecker.getInstance().isBarrel(clickedBlock) || BlockChecker.getInstance().isTrapdoor(clickedBlock) || BlockChecker.getInstance().isFurnace(clickedBlock)) {
            PersistentData.getInstance().getLockedBlock(clickedBlock).addToAccessList(EphemeralData.getInstance().getPlayersGrantingAccess().get(player.getUniqueId()));

            player.sendMessage(ChatColor.GREEN + String.format(LocaleManager.getInstance().getText("AlertAccessGrantedTo"), UUIDChecker.getInstance().findPlayerNameBasedOnUUID(EphemeralData.getInstance().getPlayersGrantingAccess().get(player.getUniqueId()))));
            EphemeralData.getInstance().getPlayersGrantingAccess().remove(player.getUniqueId());
        }

        event.setCancelled(true);
    }

    private void handleCheckingAccess(PlayerInteractEvent event, LockedBlock lockedBlock, Player player) {
        player.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("FollowingPlayersHaveAccess"));
        for (UUID playerUUID : lockedBlock.getAccessList()) {
            player.sendMessage(ChatColor.AQUA + " - " + UUIDChecker.getInstance().findPlayerNameBasedOnUUID(playerUUID));
        }
        EphemeralData.getInstance().getPlayersCheckingAccess().remove(player.getUniqueId());
        event.setCancelled(true);
    }

    private void handleRevokingAccess(PlayerInteractEvent event, Block clickedBlock, Player player) {

        // if not owner
        if (PersistentData.getInstance().getLockedBlock(clickedBlock).getOwner() != player.getUniqueId()) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("NotTheOwnerOfThisBlock"));
            return;
        }

        // if chest
        if (BlockChecker.getInstance().isChest(clickedBlock)) {
            InventoryHolder holder = ((Chest) clickedBlock.getState()).getInventory().getHolder();
            if (holder instanceof DoubleChest) { // if double chest
                // revoke access to both chests
                DoubleChest doubleChest = (DoubleChest) holder;
                Block leftChest = ((Chest) doubleChest.getLeftSide()).getBlock();
                Block rightChest = ((Chest) doubleChest.getRightSide()).getBlock();

                PersistentData.getInstance().getLockedBlock(leftChest).removeFromAccessList(EphemeralData.getInstance().getPlayersRevokingAccess().get(player.getUniqueId()));
                PersistentData.getInstance().getLockedBlock(rightChest).removeFromAccessList(EphemeralData.getInstance().getPlayersRevokingAccess().get(player.getUniqueId()));

                player.sendMessage(ChatColor.GREEN + String.format(LocaleManager.getInstance().getText("AlertAccessRevokedFor"), UUIDChecker.getInstance().findPlayerNameBasedOnUUID(EphemeralData.getInstance().getPlayersRevokingAccess().get(player.getUniqueId()))));
                EphemeralData.getInstance().getPlayersRevokingAccess().remove(player.getUniqueId());
            }
            else { // if single chest
                // revoke access to single chest
                PersistentData.getInstance().getLockedBlock(clickedBlock).removeFromAccessList(EphemeralData.getInstance().getPlayersRevokingAccess().get(player.getUniqueId()));
                player.sendMessage(ChatColor.GREEN + String.format(LocaleManager.getInstance().getText("AlertAccessRevokedFor"), UUIDChecker.getInstance().findPlayerNameBasedOnUUID(EphemeralData.getInstance().getPlayersRevokingAccess().get(player.getUniqueId()))));
                EphemeralData.getInstance().getPlayersRevokingAccess().remove(player.getUniqueId());
            }

        }

        // if door
        if (BlockChecker.getInstance().isDoor(clickedBlock)) {
            // revoke access to initial block
            PersistentData.getInstance().getLockedBlock(clickedBlock).removeFromAccessList(EphemeralData.getInstance().getPlayersRevokingAccess().get(player.getUniqueId()));
            // check block above
            if (BlockChecker.getInstance().isDoor(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() + 1, clickedBlock.getZ()))) {
                PersistentData.getInstance().getLockedBlock(clickedBlock).removeFromAccessList(EphemeralData.getInstance().getPlayersRevokingAccess().get(player.getUniqueId()));
            }
            // check block below
            if (BlockChecker.getInstance().isDoor(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() - 1, clickedBlock.getZ()))) {
                PersistentData.getInstance().getLockedBlock(clickedBlock).removeFromAccessList(EphemeralData.getInstance().getPlayersRevokingAccess().get(player.getUniqueId()));
            }

            player.sendMessage(ChatColor.GREEN + String.format(LocaleManager.getInstance().getText("AlertAccessRevokedFor"), UUIDChecker.getInstance().findPlayerNameBasedOnUUID(EphemeralData.getInstance().getPlayersRevokingAccess().get(player.getUniqueId()))));
            EphemeralData.getInstance().getPlayersRevokingAccess().remove(player.getUniqueId());
        }

        // if gate or other single-block sized lock
        if (BlockChecker.getInstance().isGate(clickedBlock) || BlockChecker.getInstance().isBarrel(clickedBlock) || BlockChecker.getInstance().isTrapdoor(clickedBlock) || BlockChecker.getInstance().isFurnace(clickedBlock)) {
            PersistentData.getInstance().getLockedBlock(clickedBlock).removeFromAccessList(EphemeralData.getInstance().getPlayersRevokingAccess().get(player.getUniqueId()));

            player.sendMessage(ChatColor.GREEN + String.format(LocaleManager.getInstance().getText("AlertAccessRevokedFor"), UUIDChecker.getInstance().findPlayerNameBasedOnUUID(EphemeralData.getInstance().getPlayersRevokingAccess().get(player.getUniqueId()))));
            EphemeralData.getInstance().getPlayersRevokingAccess().remove(player.getUniqueId());
        }

        event.setCancelled(true);

    }

    private boolean isInteractionAllowed(Player player, ClaimedChunk chunk, Faction faction) {
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

    private boolean isPlayerUsingAnAccessCommand(Player player) {
        return EphemeralData.getInstance().getPlayersGrantingAccess().containsKey(player.getUniqueId()) ||
                EphemeralData.getInstance().getPlayersCheckingAccess().contains(player.getUniqueId()) ||
                EphemeralData.getInstance().getPlayersRevokingAccess().containsKey(player.getUniqueId());
    }

    // END OF HELPER METHODS ------------------------------------------------------

}