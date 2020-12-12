package factionsystem.EventHandlers;

import factionsystem.Data.EphemeralData;
import factionsystem.MedievalFactions;
import factionsystem.Objects.ClaimedChunk;
import factionsystem.Objects.Faction;
import factionsystem.Objects.Gate;
import factionsystem.Objects.LockedBlock;
import factionsystem.Data.PersistentData;
import factionsystem.Util.Utilities;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

import static factionsystem.Util.Utilities.*;
import static org.bukkit.Bukkit.getLogger;
import static org.bukkit.Material.LADDER;

public class BlockInteractionHandler implements Listener {

    @EventHandler()
    public void handle(BlockBreakEvent event) {
        // get player
        Player player = event.getPlayer();

        // get chunk
        ClaimedChunk chunk = getClaimedChunk(event.getBlock().getLocation().getChunk().getX(), event.getBlock().getLocation().getChunk().getZ(), event.getBlock().getWorld().getName(), PersistentData.getInstance().getClaimedChunks());

        // if chunk is claimed
        if (chunk != null) {

            // player not in a faction
            if (!isInFaction(event.getPlayer().getUniqueId(), PersistentData.getInstance().getFactions())) {
                event.setCancelled(true);
            }

            // if player is in faction
            for (Faction faction : PersistentData.getInstance().getFactions()) {
                if (faction.isMember(player.getUniqueId())) {

                    // if player's faction is not the same as the holder of the chunk and player isn't bypassing
                    if (!(faction.getName().equalsIgnoreCase(chunk.getHolder())) && !EphemeralData.getInstance().getAdminsBypassingProtections().contains(player.getUniqueId())) {
                        event.setCancelled(true);
                        return;
                    }

                    // if block is locked
                    if (MedievalFactions.getInstance().utilities.isBlockLocked(event.getBlock())) {

                        // if player is not the owner and isn't bypassing
                        if (!MedievalFactions.getInstance().utilities.getLockedBlock(event.getBlock()).getOwner().equals(player.getUniqueId())
                                && !EphemeralData.getInstance().getAdminsBypassingProtections().contains(event.getPlayer().getUniqueId())) {
                            event.setCancelled(true);
                            player.sendMessage(ChatColor.RED + "You don't own this!");
                            return;
                        }

                    	Utilities.removeLock(event.getBlock(), PersistentData.getInstance().getLockedBlocks());

                    }
                    
                    // if block is in a gate
                    for (Gate gate : faction.getGates())
                    {
//                    	System.out.println("Gate " + gate.getName() + "?");
                    	if (gate.hasBlock(event.getBlock()))
                    	{
                    		event.setCancelled(true);
                            player.sendMessage(ChatColor.RED + "This block is part of gate '" + gate.getName() + "'. You must remove the gate first.");
                            return;
                    	}
                    }
                }
            }
        }
    }

    @EventHandler()
    public void handle(BlockPlaceEvent event) {
        // get player
        Player player = event.getPlayer();

        // get chunk
        ClaimedChunk chunk = getClaimedChunk(event.getBlock().getLocation().getChunk().getX(), event.getBlock().getLocation().getChunk().getZ(),
                event.getBlock().getWorld().getName(), PersistentData.getInstance().getClaimedChunks());

        // if chunk is claimed
        if (chunk != null) {

            // player not in a faction
            if (!isInFaction(event.getPlayer().getUniqueId(), PersistentData.getInstance().getFactions()) && !EphemeralData.getInstance().getAdminsBypassingProtections().contains(event.getPlayer().getUniqueId())) {
                event.setCancelled(true);
            }

            // if player is in faction
            for (Faction faction : PersistentData.getInstance().getFactions()) {
                if (faction.isMember(player.getUniqueId())) {

                    // if player's faction is not the same as the holder of the chunk
                    if (!(faction.getName().equalsIgnoreCase(chunk.getHolder())) && !EphemeralData.getInstance().getAdminsBypassingProtections().contains(event.getPlayer().getUniqueId())) {

                        if (MedievalFactions.getInstance().getConfig().getBoolean("laddersPlaceableInEnemyFactionTerritory")) {
                            // if trying to place ladder on enemy territory
                            if (event.getBlockPlaced().getType() == LADDER && faction.isEnemy(chunk.getHolder())) {
                                return;
                            }
                        }

                        event.setCancelled(true);
                        return;
                    }

                    // if chest
                    if (MedievalFactions.getInstance().utilities.isChest(event.getBlock())) {
                        // if next to non-owned locked chest
                        if (isNextToNonOwnedLockedChest(event.getPlayer(), event.getBlock()) && !EphemeralData.getInstance().getAdminsBypassingProtections().contains(event.getPlayer().getUniqueId())) {
                            event.setCancelled(true);
                            player.sendMessage(ChatColor.RED + "You can't place chests next to locked chests you don't own.");
                            return;
                        }
                    }

                    // if hopper
                    if (event.getBlock().getType() == Material.HOPPER) {
                        // if next to or under/above non-owned locked chest
                        if (isNextToNonOwnedLockedChest(event.getPlayer(), event.getBlock()) || isUnderOrAboveNonOwnedLockedChest(event.getPlayer(), event.getBlock()) && !EphemeralData.getInstance().getAdminsBypassingProtections().contains(event.getPlayer().getUniqueId())) {
                            event.setCancelled(true);
                            player.sendMessage(ChatColor.RED + "You can't place hoppers next to, under or above locked chests you don't own.");
                            return;
                        }
                    }
                }
            }
        }
    }

    private boolean isNextToNonOwnedLockedChest(Player player, Block block) {

        // define blocks
        Block neighbor1 = block.getWorld().getBlockAt(block.getX() + 1, block.getY(), block.getZ());
        Block neighbor2 = block.getWorld().getBlockAt(block.getX() - 1, block.getY(), block.getZ());
        Block neighbor3 = block.getWorld().getBlockAt(block.getX(), block.getY(), block.getZ() + 1);
        Block neighbor4 = block.getWorld().getBlockAt(block.getX(), block.getY(), block.getZ() - 1);

        if (MedievalFactions.getInstance().utilities.isChest(neighbor1)) {
            if (MedievalFactions.getInstance().utilities.isBlockLocked(neighbor1) && MedievalFactions.getInstance().utilities.getLockedBlock(neighbor1).getOwner() != player.getUniqueId()) {
                return true;
            }
        }

        if (MedievalFactions.getInstance().utilities.isChest(neighbor2)) {
            if (MedievalFactions.getInstance().utilities.isBlockLocked(neighbor2) && MedievalFactions.getInstance().utilities.getLockedBlock(neighbor2).getOwner() != player.getUniqueId()) {
                return true;
            }
        }

        if (MedievalFactions.getInstance().utilities.isChest(neighbor3)) {
            if (MedievalFactions.getInstance().utilities.isBlockLocked(neighbor3) && MedievalFactions.getInstance().utilities.getLockedBlock(neighbor3).getOwner() != player.getUniqueId()) {
                return true;
            }
        }

        if (MedievalFactions.getInstance().utilities.isChest(neighbor4)) {
            if (MedievalFactions.getInstance().utilities.isBlockLocked(neighbor4) && MedievalFactions.getInstance().utilities.getLockedBlock(neighbor4).getOwner() != player.getUniqueId()) {
                return true;
            }
        }

        return false;
    }

    private boolean isUnderOrAboveNonOwnedLockedChest(Player player, Block block) {
        // define blocks
        Block neighbor1 = block.getWorld().getBlockAt(block.getX(), block.getY() + 1, block.getZ());
        Block neighbor2 = block.getWorld().getBlockAt(block.getX(), block.getY() - 1, block.getZ());

        if (MedievalFactions.getInstance().utilities.isChest(neighbor1)) {
            if (MedievalFactions.getInstance().utilities.isBlockLocked(neighbor1) && MedievalFactions.getInstance().utilities.getLockedBlock(neighbor1).getOwner() != player.getUniqueId()) {
                return true;
            }
        }

        if (MedievalFactions.getInstance().utilities.isChest(neighbor2)) {
            if (MedievalFactions.getInstance().utilities.isBlockLocked(neighbor2) && MedievalFactions.getInstance().utilities.getLockedBlock(neighbor2).getOwner() != player.getUniqueId()) {
                return true;
            }
        }

        return false;
    }

    @EventHandler()
    public void handle(PlayerInteractEvent event) {
        // get player
        Player player = event.getPlayer();
        // get chunk
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock != null) {

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

            // if block is locked
            LockedBlock lockedBlock = MedievalFactions.getInstance().utilities.getLockedBlock(clickedBlock);
            if (lockedBlock != null) {

                // if player doesn't have access and isn't overriding
                if (!lockedBlock.hasAccess(player.getUniqueId()) && !EphemeralData.getInstance().getAdminsBypassingProtections().contains(player.getUniqueId())) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Locked by " + findPlayerNameBasedOnUUID(lockedBlock.getOwner()));
                    return;
                }

                // if player is trying to grant access
                if (EphemeralData.getInstance().getPlayersGrantingAccess().containsKey(player.getUniqueId())) {
                    handleGrantingAccess(event, clickedBlock, player);
                }

                // if player is trying to check access
                if (EphemeralData.getInstance().getPlayersCheckingAccess().contains(player.getUniqueId())) {
                    handleCheckingAccess(event, lockedBlock, player);
                }

                // if player is trying to revoke access
                if (EphemeralData.getInstance().getPlayersRevokingAccess().containsKey(player.getUniqueId())) {
                    handleRevokingAccess(event, clickedBlock, player);
                }

                if (lockedBlock.hasAccess(player.getUniqueId()))
                {
                    // Don't process any more checks so that the event is not cancelled
                    // when a player who is not part of the faction has access granted
                    // to a lock.
                    return;
                }

            }
            else {
                // if player is using an access command
                if (EphemeralData.getInstance().getPlayersGrantingAccess().containsKey(player.getUniqueId()) ||
                        EphemeralData.getInstance().getPlayersCheckingAccess().contains(player.getUniqueId()) ||
                        EphemeralData.getInstance().getPlayersRevokingAccess().containsKey(player.getUniqueId())) {

                    player.sendMessage(ChatColor.RED + "That block isn't locked!");
                }
            }

            // Check if it's a lever, and if it is and it's connected to a gate in the faction
            // territory then open/close the gate.
            if (clickedBlock.getType().equals(Material.LEVER))
            {
                if (Utilities.isClaimed(clickedBlock.getChunk(), PersistentData.getInstance().getClaimedChunks()))
                {
                    ClaimedChunk claim = Utilities.getClaimedChunk(clickedBlock.getChunk().getX(), clickedBlock.getChunk().getZ(),
                            clickedBlock.getChunk().getWorld().getName(), PersistentData.getInstance().getClaimedChunks());
                    Faction faction = Utilities.getFaction(claim.getHolder(), PersistentData.getInstance().getFactions());

                    if (faction.hasGateTrigger(clickedBlock))
                    {
                        for (Gate g : faction.getGatesForTrigger(clickedBlock))
                        {
                            BlockData blockData = clickedBlock.getBlockData();
                            Powerable powerable = (Powerable) blockData;
                            if (powerable.isPowered())
                            {
                                if (faction.getGatesForTrigger(clickedBlock).get(0).isReady())
                                {
                                    g.openGate();
                                }
                                else
                                {
                                    event.setCancelled(true);
                                    player.sendMessage(ChatColor.RED + "This gate is " + g.getStatus() + ", please wait.");
                                    return;
                                }
                            }
                            else
                            {
                                if (faction.getGatesForTrigger(clickedBlock).get(0).isReady())
                                {
                                    g.closeGate();
                                }
                                else
                                {
                                    event.setCancelled(true);
                                    player.sendMessage(ChatColor.RED + "This gate is " + g.getStatus() + ", please wait.");
                                    return;
                                }
                            }
                        }
                        return;
                    }
                }
            }

            // ---------------------------------------------------------------------------------------------------------------

            // pgarner Sep 2, 2020: Moved this to after test to see if the block is locked because it could be a block they have been granted
            // access to (or in future, a 'public' locked block), so if they're not in the faction whose territory the block exists in we want that
            // check to be handled before the interaction is rejected for not being a faction member.
            // if chunk is claimed
            ClaimedChunk chunk = getClaimedChunk(event.getClickedBlock().getLocation().getChunk().getX(), event.getClickedBlock().getLocation().getChunk().getZ(), event.getClickedBlock().getWorld().getName(), PersistentData.getInstance().getClaimedChunks());
            if (chunk != null) {
                handleClaimedChunk(event, chunk);
            }

            // ---------------------------------------------------------------------------------------------------------------

            // get tool in player's hand, if it's the gate tool
            // then we want to let them create the gate.
            if (EphemeralData.getInstance().getCreatingGatePlayers().containsKey(event.getPlayer().getUniqueId())
                    && player.getInventory().getItemInMainHand().getType().equals(Material.GOLDEN_HOE))
            {
                if (!Utilities.isClaimed(clickedBlock.getChunk(), PersistentData.getInstance().getClaimedChunks()))
                {
                    player.sendMessage(ChatColor.RED + "You can only create gates in claimed territory.");
                    return;
                }
                else
                {
                    ClaimedChunk claimedChunk = Utilities.getClaimedChunk(clickedBlock.getChunk().getX(), clickedBlock.getChunk().getZ(), clickedBlock.getWorld().getName(), PersistentData.getInstance().getClaimedChunks());
                    if (claimedChunk != null)
                    {
                        if (!Utilities.getFaction(claimedChunk.getHolder(), PersistentData.getInstance().getFactions()).isMember(player.getUniqueId()))
                        {
                            player.sendMessage(ChatColor.RED + "You must be a member of this faction to create a gate.");
                            return;
                        }
                        else
                        {
                            if (!Utilities.getFaction(claimedChunk.getHolder(), PersistentData.getInstance().getFactions()).isOwner(player.getUniqueId())
                                    && !Utilities.getFaction(claimedChunk.getHolder(), PersistentData.getInstance().getFactions()).isOfficer(player.getUniqueId()))
                            {
                                player.sendMessage(ChatColor.RED + "You must be a faction owner or officer to create a gate.");
                                return;
                            }
                        }
                    }
                }

                if (player.hasPermission("mf.gate") || player.hasPermission("mf.default"))
                {
                    // TODO: Check if a gate already exists here, and if it does, print out some info
                    // of that existing gate instead of trying to create a new one.
                    if (EphemeralData.getInstance().getCreatingGatePlayers().containsKey(event.getPlayer().getUniqueId()) && EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getCoord1() == null)
                    {
                        Gate.ErrorCodeAddCoord e = EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).addCoord(clickedBlock);
                        if (e.equals(Gate.ErrorCodeAddCoord.None))
                        {
                            event.getPlayer().sendMessage(ChatColor.GREEN + "Creating Gate 1/4: Point 1 placed successfully.");
                            event.getPlayer().sendMessage(ChatColor.YELLOW + "Click to place the second corner...");
                            return;
                        }
                        else if (e.equals(Gate.ErrorCodeAddCoord.MaterialMismatch))
                        {
                            event.getPlayer().sendMessage(ChatColor.RED + "Error placing point 1: Materials mismatch.");
                            EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                            return;
                        }
                        else if (e.equals(Gate.ErrorCodeAddCoord.WorldMismatch))
                        {
                            event.getPlayer().sendMessage(ChatColor.RED + "Error placing point 1: Worlds mismatch.");
                            EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                            return;
                        }
                        else if (e.equals(Gate.ErrorCodeAddCoord.NoCuboids))
                        {
                            event.getPlayer().sendMessage(ChatColor.RED + "Error placing point 1: You cannot place a cuboid.");
                            EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                            return;
                        }
                        else
                        {
                            event.getPlayer().sendMessage(ChatColor.RED + "Error placing point 1. Cancelled gate placement.");
                            EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                            return;
                        }
                    }
                    else if (EphemeralData.getInstance().getCreatingGatePlayers().containsKey(event.getPlayer().getUniqueId()) && EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getCoord1() != null
                            && EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getCoord2() == null
                            && EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getTrigger() == null)
                    {
                        if (!EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getCoord1().equals(clickedBlock))
                        {
                            Gate.ErrorCodeAddCoord e = EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).addCoord(clickedBlock);
                            if (e.equals(Gate.ErrorCodeAddCoord.None))
                            {
                                event.getPlayer().sendMessage(ChatColor.GREEN + "Creating Gate 2/4: Point 2 placed successfully.");
                                event.getPlayer().sendMessage(ChatColor.YELLOW + "Click on the trigger lever...");
                                return;
                            }
                            else if (e.equals(Gate.ErrorCodeAddCoord.MaterialMismatch))
                            {
                                event.getPlayer().sendMessage(ChatColor.RED + "Error placing point 2: Materials mismatch.");
                                EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                                return;
                            }
                            else if (e.equals(Gate.ErrorCodeAddCoord.WorldMismatch))
                            {
                                event.getPlayer().sendMessage(ChatColor.RED + "Error placing point 2: Worlds mismatch.");
                                EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                                return;
                            }
                            else if (e.equals(Gate.ErrorCodeAddCoord.NoCuboids))
                            {
                                event.getPlayer().sendMessage(ChatColor.RED + "Error placing point 2: You cannot place a cuboid.");
                                EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                                return;
                            }
                            else if (e.equals(Gate.ErrorCodeAddCoord.LessThanThreeHigh))
                            {
                                event.getPlayer().sendMessage(ChatColor.RED + "Error placing point 2: Gate must be 3 blocks or taller.");
                                EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                                return;
                            }
                            else
                            {
                                event.getPlayer().sendMessage(ChatColor.RED + "Error placing point 2. Cancelled gate placement.");
                                EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                                return;
                            }
                        }
                    }
                    else if (EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getCoord2() != null
                            && EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getTrigger() == null
                            && !EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getCoord2().equals(clickedBlock))
                    {
                        if (clickedBlock.getType().equals(Material.LEVER))
                        {
                            if (Utilities.isClaimed(clickedBlock.getChunk(), PersistentData.getInstance().getClaimedChunks()))
                            {
                                Gate.ErrorCodeAddCoord e = EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).addCoord(clickedBlock);
                                if (e.equals(Gate.ErrorCodeAddCoord.None))
                                {
                                    ClaimedChunk claim = Utilities.getClaimedChunk(clickedBlock.getChunk().getX(), clickedBlock.getChunk().getZ(),
                                            clickedBlock.getChunk().getWorld().getName(), PersistentData.getInstance().getClaimedChunks());
                                    Faction faction = Utilities.getFaction(claim.getHolder(), PersistentData.getInstance().getFactions());
                                    faction.addGate(EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()));
                                    EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                                    event.getPlayer().sendMessage(ChatColor.GREEN + "Creating Gate 4/4: Lever successfully linked.");
                                    event.getPlayer().sendMessage(ChatColor.GREEN + "Gate successfully created.");
                                    return;
                                }
                                else
                                {
                                    event.getPlayer().sendMessage(ChatColor.RED + "Error linking to lever. Cancelled gate placement.");
                                    EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                                    return;
                                }
                            }
                            else
                            {
                                event.getPlayer().sendMessage(ChatColor.RED + "Error: Can only use levers in claimed territory.");
                                EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                                return;
                            }
                        }
                        else
                        {
                            event.getPlayer().sendMessage(ChatColor.RED + "Trigger block was not a lever. Cancelled gate placement.");
                            EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                            return;
                        }
                    }
                }
                else
                {
                    player.sendMessage(ChatColor.RED + "Sorry! In order to create a gate you need the following permission: 'mf.gate'");
                }
            }

        }
    }

    private void handleLockingBlock(PlayerInteractEvent event, Player player, Block clickedBlock) {
        // if chunk is claimed
        ClaimedChunk chunk = getClaimedChunk(event.getClickedBlock().getLocation().getChunk().getX(), event.getClickedBlock().getLocation().getChunk().getZ(),
                event.getClickedBlock().getWorld().getName(), PersistentData.getInstance().getClaimedChunks());
        if (chunk != null) {

            // if claimed by other faction
            if (!chunk.getHolder().equalsIgnoreCase(getPlayersFaction(player.getUniqueId(), PersistentData.getInstance().getFactions()).getName())) {
                player.sendMessage(ChatColor.RED + "You can only lock things in your faction's territory!");
                event.setCancelled(true);
                return;
            }

            // if already locked
            if (MedievalFactions.getInstance().utilities.isBlockLocked(clickedBlock)) {
                player.sendMessage(ChatColor.RED + "This block is already locked!");
                event.setCancelled(true);
                return;
            }

            // block type check
            if (isDoor(clickedBlock) || MedievalFactions.getInstance().utilities.isChest(clickedBlock) || isGate(clickedBlock) || isBarrel(clickedBlock) || isTrapdoor(clickedBlock) || isFurnace(clickedBlock) || isAnvil(clickedBlock)) {

                // specific to chests because they can be single or double.
                if (MedievalFactions.getInstance().utilities.isChest(clickedBlock)) {
                    InventoryHolder holder = ((Chest) clickedBlock.getState()).getInventory().getHolder();
                    if (holder instanceof DoubleChest) {
                        // chest multi-lock
                        DoubleChest doubleChest = (DoubleChest) holder;
                        Block leftChest = ((Chest) doubleChest.getLeftSide()).getBlock();
                        Block rightChest = ((Chest) doubleChest.getRightSide()).getBlock();

                        LockedBlock left = new LockedBlock(player.getUniqueId(), getPlayersFaction(player.getUniqueId(), PersistentData.getInstance().getFactions()).getName(), leftChest.getX(), leftChest.getY(), leftChest.getZ(), leftChest.getWorld().getName());
                        PersistentData.getInstance().getLockedBlocks().add(left);

                        LockedBlock right = new LockedBlock(player.getUniqueId(), getPlayersFaction(player.getUniqueId(), PersistentData.getInstance().getFactions()).getName(), rightChest.getX(), rightChest.getY(), rightChest.getZ(), rightChest.getWorld().getName());
                        PersistentData.getInstance().getLockedBlocks().add(right);

                        player.sendMessage(ChatColor.GREEN + "Locked!");
                        EphemeralData.getInstance().getLockingPlayers().remove(player.getUniqueId());
                    }
                    else {
                        // lock single chest
                        LockedBlock single = new LockedBlock(player.getUniqueId(), getPlayersFaction(player.getUniqueId(), PersistentData.getInstance().getFactions()).getName(), clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ(), clickedBlock.getWorld().getName());
                        PersistentData.getInstance().getLockedBlocks().add(single);

                        player.sendMessage(ChatColor.GREEN + "Locked!");
                        EphemeralData.getInstance().getLockingPlayers().remove(player.getUniqueId());
                    }
                }

                // door multi-lock (specific to doors because they have two block heights but you could have clicked either block).
                if (isDoor(clickedBlock)) {
                    // lock initial block
                    LockedBlock initial = new LockedBlock(player.getUniqueId(), getPlayersFaction(player.getUniqueId(), PersistentData.getInstance().getFactions()).getName(), clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ(), clickedBlock.getWorld().getName());
                    PersistentData.getInstance().getLockedBlocks().add(initial);
                    // check block above
                    if (isDoor(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() + 1, clickedBlock.getZ()))) {
                        LockedBlock newLockedBlock2 = new LockedBlock(player.getUniqueId(), getPlayersFaction(player.getUniqueId(), PersistentData.getInstance().getFactions()).getName(), clickedBlock.getX(), clickedBlock.getY() + 1, clickedBlock.getZ(), clickedBlock.getWorld().getName());
                        PersistentData.getInstance().getLockedBlocks().add(newLockedBlock2);
                    }
                    // check block below
                    if (isDoor(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() - 1, clickedBlock.getZ()))) {
                        LockedBlock newLockedBlock2 = new LockedBlock(player.getUniqueId(), getPlayersFaction(player.getUniqueId(), PersistentData.getInstance().getFactions()).getName(), clickedBlock.getX(), clickedBlock.getY() - 1, clickedBlock.getZ(), clickedBlock.getWorld().getName());
                        PersistentData.getInstance().getLockedBlocks().add(newLockedBlock2);
                    }

                    player.sendMessage(ChatColor.GREEN + "Locked!");
                    EphemeralData.getInstance().getLockingPlayers().remove(player.getUniqueId());
                }

                // Remainder of lockable blocks are only 1x1 so generic code will suffice.
                if (isGate(clickedBlock) || isBarrel(clickedBlock) || isTrapdoor(clickedBlock) || isFurnace(clickedBlock)) {
                    LockedBlock block = new LockedBlock(player.getUniqueId(), getPlayersFaction(player.getUniqueId(), PersistentData.getInstance().getFactions()).getName(),
                            clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ(), clickedBlock.getWorld().getName());
                    PersistentData.getInstance().getLockedBlocks().add(block);
                    player.sendMessage(ChatColor.GREEN + "Locked!");
                    EphemeralData.getInstance().getLockingPlayers().remove(player.getUniqueId());
                }

                event.setCancelled(true);
                return;
            }
            else {
                player.sendMessage(ChatColor.RED + "You can only lock chests, doors, barrels, trapdoors, furnaces, anvils or gates.");
                return;
            }

        }
        else {
            player.sendMessage(ChatColor.RED + "You can only lock blocks on land claimed by your faction!");
            event.setCancelled(true);
            return;
        }
    }

    private Material compatMaterial(String materialName)
    {
        Material mat = Material.getMaterial(materialName);
        if (mat == null)
        {
            // Find compatible substitute.
            switch(materialName)
            {
                case "CRIMSON_FENCE_GATE":
                    return Material.OAK_FENCE_GATE;
                case "WARPED_FENCE_GATE":
                    return Material.OAK_FENCE_GATE;
                case "CRIMSON_DOOR":
                    return Material.OAK_DOOR;
                case "WARPED_DOOR":
                    return Material.OAK_DOOR;
                case "CRIMSON_TRAPDOOR":
                    return Material.OAK_TRAPDOOR;
                case "WARPED_TRAPDOOR":
                    return Material.OAK_TRAPDOOR;
                default:
                    getLogger().info("ERROR: Could not locate a compatable material matching '" + materialName + "'.");
                    return null;
            }
        }
        else
        {
            return mat;
        }
    }

    private boolean isDoor(Block block) {
        if (block.getType() == Material.ACACIA_DOOR ||
                block.getType() == Material.BIRCH_DOOR ||
                block.getType() == Material.DARK_OAK_DOOR ||
                block.getType() == Material.IRON_DOOR ||
                block.getType() == Material.JUNGLE_DOOR ||
                block.getType() == Material.OAK_DOOR ||
                block.getType() == Material.SPRUCE_DOOR ||
                block.getType() == compatMaterial("CRIMSON_DOOR") ||
                block.getType() == compatMaterial("WARPED_DOOR")) {

            return true;

        }
        return false;
    }

    private boolean isTrapdoor(Block block)
    {
        if (block.getType() == Material.IRON_TRAPDOOR ||
                block.getType() == Material.OAK_TRAPDOOR ||
                block.getType() == Material.SPRUCE_TRAPDOOR ||
                block.getType() == Material.BIRCH_TRAPDOOR ||
                block.getType() == Material.JUNGLE_TRAPDOOR ||
                block.getType() == Material.ACACIA_TRAPDOOR ||
                block.getType() == Material.DARK_OAK_TRAPDOOR ||
                block.getType() == compatMaterial("CRIMSON_TRAPDOOR") ||
                block.getType() == compatMaterial("WARPED_TRAPDOOR"))
        {
            return true;
        }
        return false;
    }

    private boolean isFurnace(Block block)
    {
        if (block.getType() == Material.FURNACE ||
                block.getType() == Material.BLAST_FURNACE)
        {
            return true;
        }
        return false;
    }

    private boolean isAnvil(Block block)
    {
        if (block.getType() == Material.ANVIL ||
                block.getType() == Material.CHIPPED_ANVIL ||
                block.getType() == Material.DAMAGED_ANVIL)
        {
            return true;
        }
        return false;
    }

    private boolean isGate(Block block)
    {
        if (block.getType() == Material.OAK_FENCE_GATE ||
                block.getType() == Material.SPRUCE_FENCE_GATE ||
                block.getType() == Material.BIRCH_FENCE_GATE ||
                block.getType() == Material.JUNGLE_FENCE_GATE ||
                block.getType() == Material.ACACIA_FENCE_GATE ||
                block.getType() == Material.DARK_OAK_FENCE_GATE ||
                block.getType() == compatMaterial("CRIMSON_FENCE_GATE") ||
                block.getType() == compatMaterial("WARPED_FENCE_GATE"))
        {
            return true;
        }
        return false;
    }

    private boolean isBarrel(Block block)
    {
        if (block.getType() == Material.BARREL)
        {
            return true;
        }
        return false;
    }

    private void handleUnlockingBlock(PlayerInteractEvent event, Player player, Block clickedBlock) {
        // if locked
        if (MedievalFactions.getInstance().utilities.isBlockLocked(clickedBlock)) {
            if (MedievalFactions.getInstance().utilities.getLockedBlock(clickedBlock).getOwner().equals(player.getUniqueId())) {

                if (MedievalFactions.getInstance().utilities.isChest(clickedBlock)) {
                    InventoryHolder holder = ((Chest) clickedBlock.getState()).getInventory().getHolder();
                    if (holder instanceof DoubleChest) {
                        // chest multi-unlock
                        DoubleChest doubleChest = (DoubleChest) holder;
                        Block leftChest = ((Chest) doubleChest.getLeftSide()).getBlock();
                        Block rightChest = ((Chest) doubleChest.getRightSide()).getBlock();

                        // unlock leftChest and rightChest
                        removeLock(leftChest);
                        removeLock(rightChest);

                        player.sendMessage(ChatColor.GREEN + "Unlocked!");
                        EphemeralData.getInstance().getUnlockingPlayers().remove(player.getUniqueId());
                    }
                    else {
                        // unlock single chest
                        removeLock(clickedBlock);
                        player.sendMessage(ChatColor.GREEN + "Unlocked!");
                        EphemeralData.getInstance().getUnlockingPlayers().remove(player.getUniqueId());
                    }
                }

                // door multi-unlock
                if (isDoor(clickedBlock)) {
                    // unlock initial block
                    removeLock(clickedBlock);
                    // check block above
                    if (isDoor(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() + 1, clickedBlock.getZ()))) {
                        removeLock(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() + 1, clickedBlock.getZ()));
                    }
                    // check block below
                    if (isDoor(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() - 1, clickedBlock.getZ()))) {
                        removeLock(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() - 1, clickedBlock.getZ()));
                    }

                    player.sendMessage(ChatColor.GREEN + "Unlocked!");
                    EphemeralData.getInstance().getUnlockingPlayers().remove(player.getUniqueId());
                }

                // single block size lock logic.
                if (isGate(clickedBlock) || isBarrel(clickedBlock) || isTrapdoor(clickedBlock) || isFurnace(clickedBlock)) {
                    removeLock(clickedBlock);

                    player.sendMessage(ChatColor.GREEN + "Unlocked!");
                    EphemeralData.getInstance().getUnlockingPlayers().remove(player.getUniqueId());
                }

                event.setCancelled(true);
                return;
            }
        }
        else {
            player.sendMessage(ChatColor.RED + "That block isn't locked!");
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
        if (!isInFaction(event.getPlayer().getUniqueId(), PersistentData.getInstance().getFactions()) && !EphemeralData.getInstance().getAdminsBypassingProtections().contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }

        // if player is in faction
        for (Faction faction : PersistentData.getInstance().getFactions()) {
            if (faction.isMember(event.getPlayer().getUniqueId())) {

                // if player's faction is not the same as the holder of the chunk and player isn't overriding
                if (!(faction.getName().equalsIgnoreCase(chunk.getHolder())) && !EphemeralData.getInstance().getAdminsBypassingProtections().contains(event.getPlayer().getUniqueId())) {

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

                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    private boolean isBlockInteractable(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null) {
            // CHEST
            if (MedievalFactions.getInstance().utilities.isChest(event.getClickedBlock())) {
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
                    return false;
            }
        }
        return true;
    }

    public boolean materialAllowed(Material material) {
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
        if (MedievalFactions.getInstance().utilities.getLockedBlock(clickedBlock, PersistentData.getInstance().getLockedBlocks()).getOwner() != player.getUniqueId()) {
            player.sendMessage(ChatColor.RED + "You are not the owner of this block!");
            return;
        }

        // if chest
        if (MedievalFactions.getInstance().utilities.isChest(clickedBlock)) {
            InventoryHolder holder = ((Chest) clickedBlock.getState()).getInventory().getHolder();
            if (holder instanceof DoubleChest) { // if double chest
                // grant access to both chests
                DoubleChest doubleChest = (DoubleChest) holder;
                Block leftChest = ((Chest) doubleChest.getLeftSide()).getBlock();
                Block rightChest = ((Chest) doubleChest.getRightSide()).getBlock();

                MedievalFactions.getInstance().utilities.getLockedBlock(leftChest, PersistentData.getInstance().getLockedBlocks()).addToAccessList(EphemeralData.getInstance().getPlayersGrantingAccess().get(player.getUniqueId()));
                MedievalFactions.getInstance().utilities.getLockedBlock(rightChest, PersistentData.getInstance().getLockedBlocks()).addToAccessList(EphemeralData.getInstance().getPlayersGrantingAccess().get(player.getUniqueId()));

                player.sendMessage(ChatColor.GREEN + "Access granted to " + findPlayerNameBasedOnUUID(EphemeralData.getInstance().getPlayersGrantingAccess().get(player.getUniqueId())));
                EphemeralData.getInstance().getPlayersGrantingAccess().remove(player.getUniqueId());
            }
            else { // if single chest
                // grant access to single chest
                MedievalFactions.getInstance().utilities.getLockedBlock(clickedBlock, PersistentData.getInstance().getLockedBlocks()).addToAccessList(EphemeralData.getInstance().getPlayersGrantingAccess().get(player.getUniqueId()));
                player.sendMessage(ChatColor.GREEN + "Access granted to " + findPlayerNameBasedOnUUID(EphemeralData.getInstance().getPlayersGrantingAccess().get(player.getUniqueId())));
                EphemeralData.getInstance().getPlayersGrantingAccess().remove(player.getUniqueId());
            }

        }

        // if door
        if (isDoor(clickedBlock)) {
            // grant access to initial block
            MedievalFactions.getInstance().utilities.getLockedBlock(clickedBlock, PersistentData.getInstance().getLockedBlocks()).addToAccessList(EphemeralData.getInstance().getPlayersGrantingAccess().get(player.getUniqueId()));
            // check block above
            if (isDoor(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() + 1, clickedBlock.getZ()))) {
                MedievalFactions.getInstance().utilities.getLockedBlock(clickedBlock, PersistentData.getInstance().getLockedBlocks()).addToAccessList(EphemeralData.getInstance().getPlayersGrantingAccess().get(player.getUniqueId()));
            }
            // check block below
            if (isDoor(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() - 1, clickedBlock.getZ()))) {
                MedievalFactions.getInstance().utilities.getLockedBlock(clickedBlock, PersistentData.getInstance().getLockedBlocks()).addToAccessList(EphemeralData.getInstance().getPlayersGrantingAccess().get(player.getUniqueId()));
            }

            player.sendMessage(ChatColor.GREEN + "Access granted to " + findPlayerNameBasedOnUUID(EphemeralData.getInstance().getPlayersGrantingAccess().get(player.getUniqueId())));
            EphemeralData.getInstance().getPlayersGrantingAccess().remove(player.getUniqueId());
        }

        // if gate (or single-block sized lock)
        if (isGate(clickedBlock) || isBarrel(clickedBlock) || isTrapdoor(clickedBlock) || isFurnace(clickedBlock)) {
            MedievalFactions.getInstance().utilities.getLockedBlock(clickedBlock, PersistentData.getInstance().getLockedBlocks()).addToAccessList(EphemeralData.getInstance().getPlayersGrantingAccess().get(player.getUniqueId()));

            player.sendMessage(ChatColor.GREEN + "Access granted to " + findPlayerNameBasedOnUUID(EphemeralData.getInstance().getPlayersGrantingAccess().get(player.getUniqueId())));
            EphemeralData.getInstance().getPlayersGrantingAccess().remove(player.getUniqueId());
        }

        event.setCancelled(true);
    }

    private void handleCheckingAccess(PlayerInteractEvent event, LockedBlock lockedBlock, Player player) {
        player.sendMessage(ChatColor.AQUA + "The following players have access to this block:");
        for (UUID playerUUID : lockedBlock.getAccessList()) {
            player.sendMessage(ChatColor.AQUA + " - " + findPlayerNameBasedOnUUID(playerUUID));
        }
        EphemeralData.getInstance().getPlayersCheckingAccess().remove(player.getUniqueId());
        event.setCancelled(true);
    }

    private void handleRevokingAccess(PlayerInteractEvent event, Block clickedBlock, Player player) {

        // if not owner
        if (MedievalFactions.getInstance().utilities.getLockedBlock(clickedBlock, PersistentData.getInstance().getLockedBlocks()).getOwner() != player.getUniqueId()) {
            player.sendMessage(ChatColor.RED + "You are not the owner of this block!");
            return;
        }

        // if chest
        if (MedievalFactions.getInstance().utilities.isChest(clickedBlock)) {
            InventoryHolder holder = ((Chest) clickedBlock.getState()).getInventory().getHolder();
            if (holder instanceof DoubleChest) { // if double chest
                // revoke access to both chests
                DoubleChest doubleChest = (DoubleChest) holder;
                Block leftChest = ((Chest) doubleChest.getLeftSide()).getBlock();
                Block rightChest = ((Chest) doubleChest.getRightSide()).getBlock();

                MedievalFactions.getInstance().utilities.getLockedBlock(leftChest, PersistentData.getInstance().getLockedBlocks()).removeFromAccessList(EphemeralData.getInstance().getPlayersRevokingAccess().get(player.getUniqueId()));
                MedievalFactions.getInstance().utilities.getLockedBlock(rightChest, PersistentData.getInstance().getLockedBlocks()).removeFromAccessList(EphemeralData.getInstance().getPlayersRevokingAccess().get(player.getUniqueId()));

                player.sendMessage(ChatColor.GREEN + "Access revoked for " + findPlayerNameBasedOnUUID(EphemeralData.getInstance().getPlayersRevokingAccess().get(player.getUniqueId())));
                EphemeralData.getInstance().getPlayersRevokingAccess().remove(player.getUniqueId());
            }
            else { // if single chest
                // revoke access to single chest
                MedievalFactions.getInstance().utilities.getLockedBlock(clickedBlock, PersistentData.getInstance().getLockedBlocks()).removeFromAccessList(EphemeralData.getInstance().getPlayersRevokingAccess().get(player.getUniqueId()));
                player.sendMessage(ChatColor.GREEN + "Access revoked for " + findPlayerNameBasedOnUUID(EphemeralData.getInstance().getPlayersRevokingAccess().get(player.getUniqueId())));
                EphemeralData.getInstance().getPlayersRevokingAccess().remove(player.getUniqueId());
            }

        }

        // if door
        if (isDoor(clickedBlock)) {
            // revoke access to initial block
            MedievalFactions.getInstance().utilities.getLockedBlock(clickedBlock, PersistentData.getInstance().getLockedBlocks()).removeFromAccessList(EphemeralData.getInstance().getPlayersRevokingAccess().get(player.getUniqueId()));
            // check block above
            if (isDoor(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() + 1, clickedBlock.getZ()))) {
                MedievalFactions.getInstance().utilities.getLockedBlock(clickedBlock, PersistentData.getInstance().getLockedBlocks()).removeFromAccessList(EphemeralData.getInstance().getPlayersRevokingAccess().get(player.getUniqueId()));
            }
            // check block below
            if (isDoor(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() - 1, clickedBlock.getZ()))) {
                MedievalFactions.getInstance().utilities.getLockedBlock(clickedBlock, PersistentData.getInstance().getLockedBlocks()).removeFromAccessList(EphemeralData.getInstance().getPlayersRevokingAccess().get(player.getUniqueId()));
            }

            player.sendMessage(ChatColor.GREEN + "Access revoked for " + findPlayerNameBasedOnUUID(EphemeralData.getInstance().getPlayersRevokingAccess().get(player.getUniqueId())));
            EphemeralData.getInstance().getPlayersRevokingAccess().remove(player.getUniqueId());
        }

        // if gate or other single-block sized lock
        if (isGate(clickedBlock) || isBarrel(clickedBlock) || isTrapdoor(clickedBlock) || isFurnace(clickedBlock)) {
            MedievalFactions.getInstance().utilities.getLockedBlock(clickedBlock, PersistentData.getInstance().getLockedBlocks()).removeFromAccessList(EphemeralData.getInstance().getPlayersRevokingAccess().get(player.getUniqueId()));

            player.sendMessage(ChatColor.GREEN + "Access revoked for " + findPlayerNameBasedOnUUID(EphemeralData.getInstance().getPlayersRevokingAccess().get(player.getUniqueId())));
            EphemeralData.getInstance().getPlayersRevokingAccess().remove(player.getUniqueId());
        }

        event.setCancelled(true);

    }

}
