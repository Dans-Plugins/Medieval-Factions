package factionsystem.EventHandlers;

import factionsystem.Main;
import factionsystem.Objects.ClaimedChunk;
import factionsystem.Objects.Faction;
import factionsystem.Objects.Gate;
import factionsystem.Objects.LockedBlock;
import factionsystem.Subsystems.UtilitySubsystem;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static factionsystem.Subsystems.UtilitySubsystem.*;
import static org.bukkit.Material.*;

public class PlayerInteractEventHandler {

    Main main = null;

    public PlayerInteractEventHandler(Main plugin) {
        main = plugin;
    }

    public void handle(PlayerInteractEvent event) {
        // get player
        Player player = event.getPlayer();
        // get chunk
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock != null) {

            // if player is attempting to lock a block
            if (main.lockingPlayers.contains(player.getUniqueId())) {
                handleLockingBlock(event, player, clickedBlock);
            }

            // ---------------------------------------------------------------------------------------------------------------

            // if player is trying to unlock a block
            if (main.unlockingPlayers.contains(player.getUniqueId())) {
                handleUnlockingBlock(event, player, clickedBlock);
            }

            // ---------------------------------------------------------------------------------------------------------------

            // if block is locked
            LockedBlock lockedBlock = main.utilities.getLockedBlock(clickedBlock);
            if (lockedBlock != null) {

                // if player doesn't have access and isn't overriding
                if (!lockedBlock.hasAccess(player.getUniqueId()) && !main.adminsBypassingProtections.contains(player.getUniqueId())) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Locked by " + findPlayerNameBasedOnUUID(lockedBlock.getOwner()));
                    return;
                }

                // if player is trying to grant access
                if (main.playersGrantingAccess.containsKey(player.getUniqueId())) {
                    handleGrantingAccess(event, clickedBlock, player);
                }

                // if player is trying to check access
                if (main.playersCheckingAccess.contains(player.getUniqueId())) {
                    handleCheckingAccess(event, lockedBlock, player);
                }

                // if player is trying to revoke access
                if (main.playersRevokingAccess.containsKey(player.getUniqueId())) {
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
                if (main.playersGrantingAccess.containsKey(player.getUniqueId()) ||
                    main.playersCheckingAccess.contains(player.getUniqueId()) ||
                    main.playersRevokingAccess.containsKey(player.getUniqueId())) {

                    player.sendMessage(ChatColor.RED + "That block isn't locked!");
                }
            }
            
        	// Check if it's a lever, and if it is and it's connected to a gate in the faction
        	// territory then open/close the gate.
        	if (clickedBlock.getType().equals(Material.LEVER))
        	{
    			if (UtilitySubsystem.isClaimed(clickedBlock.getChunk(), main.claimedChunks))
    			{
        			ClaimedChunk claim = UtilitySubsystem.getClaimedChunk(clickedBlock.getChunk().getX(), clickedBlock.getChunk().getZ(),
        					clickedBlock.getChunk().getWorld().getName(), main.claimedChunks);
        			Faction faction = UtilitySubsystem.getFaction(claim.getHolder(), main.factions);
        			System.out.println("Lever check for faction " + faction.getName());
        			if (faction.hasGateTrigger(clickedBlock))
        			{
        				System.out.println("Has gate check for faction " + faction.getName());
        				for (Gate g : faction.getGatesForTrigger(clickedBlock))
        				{
        					System.out.println("Gate for trigger" + g.getName());
        					BlockData blockData = clickedBlock.getBlockData();
        					Powerable powerable = (Powerable) blockData;
        					if (powerable.isPowered())
        					{
        						g.openGate();
        					}
        					else
        					{
        						g.closeGate();
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
            ClaimedChunk chunk = getClaimedChunk(event.getClickedBlock().getLocation().getChunk().getX(), event.getClickedBlock().getLocation().getChunk().getZ(), event.getClickedBlock().getWorld().getName(), main.claimedChunks);
            if (chunk != null) {
                handleClaimedChunk(event, chunk);
            }

            // ---------------------------------------------------------------------------------------------------------------

            // get tool in player's hand, if it's the gate tool
            // then we want to let them create the gate.
        	if (player.getInventory().getItemInMainHand().getType().equals(Material.GOLDEN_HOE))
        	{
        		if (!UtilitySubsystem.isClaimed(clickedBlock.getChunk(), main.claimedChunks))
        		{
        			player.sendMessage(ChatColor.RED + "You can only create gates in claimed territory.");
        			return;
        		}
        		else
        		{
        			ClaimedChunk claimedChunk = UtilitySubsystem.getClaimedChunk(clickedBlock.getChunk().getX(), clickedBlock.getChunk().getZ(), clickedBlock.getWorld().getName(), main.claimedChunks);
        			if (claimedChunk != null)
        			{
        				if (!UtilitySubsystem.getFaction(claimedChunk.getHolder(), main.factions).isMember(player.getUniqueId()))
        				{
        					player.sendMessage(ChatColor.RED + "You must be a member of this faction to create a gate.");
        					return;
        				}
        				else
        				{
        					if (!UtilitySubsystem.getFaction(claimedChunk.getHolder(), main.factions).isOwner(player.getUniqueId())
        							&& !UtilitySubsystem.getFaction(claimedChunk.getHolder(), main.factions).isOfficer(player.getUniqueId()))
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
	        		if (!main.creatingGatePlayers.containsKey(event.getPlayer().getUniqueId()))
	        		{
	        			UtilitySubsystem.startCreatingGate(main, event.getPlayer(), clickedBlock);
	        			event.getPlayer().sendMessage(ChatColor.GREEN + "Creating Gate 1/4: Point 1 placed successfully.");
	        			event.getPlayer().sendMessage(ChatColor.YELLOW + "Click to place the second point...");
	        			return;
	        		}
	        		else
	        		{
	        			if (main.creatingGatePlayers.get(event.getPlayer().getUniqueId()).getCoord2() == null
	        					&& !main.creatingGatePlayers.get(event.getPlayer().getUniqueId()).getCoord1().equals(clickedBlock))
	        			{
	        				Gate.ErrorCodeAddCoord e = main.creatingGatePlayers.get(event.getPlayer().getUniqueId()).addCoord(clickedBlock);
		        			if (e.equals(Gate.ErrorCodeAddCoord.None))
		        			{
			        			event.getPlayer().sendMessage(ChatColor.GREEN + "Creating Gate 2/4: Point 2 placed successfully.");
			        			event.getPlayer().sendMessage(ChatColor.YELLOW + "Click on the trigger lever...");
			        			return;
		        			}
		        			else if (e.equals(Gate.ErrorCodeAddCoord.MaterialMismatch))
		        			{
		        				event.getPlayer().sendMessage(ChatColor.RED + "Error placing point 2: Materials mismatch.");
		        				main.creatingGatePlayers.remove(event.getPlayer().getUniqueId());
		        				return;
		        			}
		        			else if (e.equals(Gate.ErrorCodeAddCoord.WorldMismatch))
		        			{
		        				event.getPlayer().sendMessage(ChatColor.RED + "Error placing point 2: Worlds mismatch.");
		        				main.creatingGatePlayers.remove(event.getPlayer().getUniqueId());
		        				return;
		        			}
		        			else if (e.equals(Gate.ErrorCodeAddCoord.NoCuboids))
		        			{
		        				event.getPlayer().sendMessage(ChatColor.RED + "Error placing point 2: You cannot place a cuboid.");
		        				main.creatingGatePlayers.remove(event.getPlayer().getUniqueId());
		        				return;
		        			}
		        			else
		        			{
		        				event.getPlayer().sendMessage(ChatColor.RED + "Error placing point 2. Cancelled gate placement.");
		        				main.creatingGatePlayers.remove(event.getPlayer().getUniqueId());
		        				return;
		        			}
	        			}
	        			else if (main.creatingGatePlayers.get(event.getPlayer().getUniqueId()).getCoord2() != null
	        					&& main.creatingGatePlayers.get(event.getPlayer().getUniqueId()).getTrigger() == null
	        					&& !main.creatingGatePlayers.get(event.getPlayer().getUniqueId()).getCoord2().equals(clickedBlock))
	        			{
	        				if (clickedBlock.getType().equals(Material.LEVER))
	        				{
			        			if (UtilitySubsystem.isClaimed(clickedBlock.getChunk(), main.claimedChunks))
			        			{
			        				Gate.ErrorCodeAddCoord e = main.creatingGatePlayers.get(event.getPlayer().getUniqueId()).addCoord(clickedBlock);
			        				if (e.equals(Gate.ErrorCodeAddCoord.None))
			        				{
					        			ClaimedChunk claim = UtilitySubsystem.getClaimedChunk(clickedBlock.getChunk().getX(), clickedBlock.getChunk().getZ(),
					        					clickedBlock.getChunk().getWorld().getName(), main.claimedChunks);
					        			Faction faction = UtilitySubsystem.getFaction(claim.getHolder(), main.factions);
				        				faction.addGate(main.creatingGatePlayers.get(event.getPlayer().getUniqueId()));
					        			main.creatingGatePlayers.remove(event.getPlayer().getUniqueId());     	
					        			event.getPlayer().sendMessage(ChatColor.GREEN + "Creating Gate 4/4: Lever successfully linked.");
					        			event.getPlayer().sendMessage(ChatColor.GREEN + "Gate successfully created.");
					        			return;
			        				}
			        				else
			        				{
				        				event.getPlayer().sendMessage(ChatColor.RED + "Error linking to lever. Cancelled gate placement.");
				        				main.creatingGatePlayers.remove(event.getPlayer().getUniqueId());
				        				return;
			        				}
			        			}
			        			else
			        			{
			        				event.getPlayer().sendMessage(ChatColor.RED + "Error: Can only use levers in claimed territory.");
			        				main.creatingGatePlayers.remove(event.getPlayer().getUniqueId());
			        				return;
			        			}
	        				}
	        				else
	        				{
		        				event.getPlayer().sendMessage(ChatColor.RED + "Trigger block was not a lever. Cancelled gate placement.");
		        				main.creatingGatePlayers.remove(event.getPlayer().getUniqueId());
		        				return;
	        				}
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
        		event.getClickedBlock().getWorld().getName(), main.claimedChunks);
        if (chunk != null) {

            // if claimed by other faction
            if (!chunk.getHolder().equalsIgnoreCase(getPlayersFaction(player.getUniqueId(), main.factions).getName())) {
                player.sendMessage(ChatColor.RED + "You can only lock things in your faction's territory!");
                event.setCancelled(true);
                return;
            }

            // if already locked
            if (main.utilities.isBlockLocked(clickedBlock)) {
                player.sendMessage(ChatColor.RED + "This block is already locked!");
                event.setCancelled(true);
                return;
            }

            // block type check
            if (main.utilities.isDoor(clickedBlock) || main.utilities.isChest(clickedBlock) || main.utilities.isGate(clickedBlock) || main.utilities.isBarrel(clickedBlock) || main.utilities.isTrapdoor(clickedBlock) || main.utilities.isFurnace(clickedBlock) || main.utilities.isAnvil(clickedBlock)) {

            	// specific to chests because they can be single or double.
                if (main.utilities.isChest(clickedBlock)) {
                    InventoryHolder holder = ((Chest) clickedBlock.getState()).getInventory().getHolder();
                    if (holder instanceof DoubleChest) {
                        // chest multi-lock
                        DoubleChest doubleChest = (DoubleChest) holder;
                        Block leftChest = ((Chest) doubleChest.getLeftSide()).getBlock();
                        Block rightChest = ((Chest) doubleChest.getRightSide()).getBlock();

                        LockedBlock left = new LockedBlock(player.getUniqueId(), getPlayersFaction(player.getUniqueId(), main.factions).getName(), leftChest.getX(), leftChest.getY(), leftChest.getZ(), leftChest.getWorld().getName());
                        main.lockedBlocks.add(left);

                        LockedBlock right = new LockedBlock(player.getUniqueId(), getPlayersFaction(player.getUniqueId(), main.factions).getName(), rightChest.getX(), rightChest.getY(), rightChest.getZ(), rightChest.getWorld().getName());
                        main.lockedBlocks.add(right);

                        player.sendMessage(ChatColor.GREEN + "Locked!");
                        main.lockingPlayers.remove(player.getUniqueId());
                    }
                    else {
                        // lock single chest
                        LockedBlock single = new LockedBlock(player.getUniqueId(), getPlayersFaction(player.getUniqueId(), main.factions).getName(), clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ(), clickedBlock.getWorld().getName());
                        main.lockedBlocks.add(single);

                        player.sendMessage(ChatColor.GREEN + "Locked!");
                        main.lockingPlayers.remove(player.getUniqueId());
                    }
                }

                // door multi-lock (specific to doors because they have two block heights but you could have clicked either block).
                if (main.utilities.isDoor(clickedBlock)) {
                    // lock initial block
                    LockedBlock initial = new LockedBlock(player.getUniqueId(), getPlayersFaction(player.getUniqueId(), main.factions).getName(), clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ(), clickedBlock.getWorld().getName());
                    main.lockedBlocks.add(initial);
                    // check block above
                    if (main.utilities.isDoor(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() + 1, clickedBlock.getZ()))) {
                        LockedBlock newLockedBlock2 = new LockedBlock(player.getUniqueId(), getPlayersFaction(player.getUniqueId(), main.factions).getName(), clickedBlock.getX(), clickedBlock.getY() + 1, clickedBlock.getZ(), clickedBlock.getWorld().getName());
                        main.lockedBlocks.add(newLockedBlock2);
                    }
                    // check block below
                    if (main.utilities.isDoor(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() - 1, clickedBlock.getZ()))) {
                        LockedBlock newLockedBlock2 = new LockedBlock(player.getUniqueId(), getPlayersFaction(player.getUniqueId(), main.factions).getName(), clickedBlock.getX(), clickedBlock.getY() - 1, clickedBlock.getZ(), clickedBlock.getWorld().getName());
                        main.lockedBlocks.add(newLockedBlock2);
                    }

                    player.sendMessage(ChatColor.GREEN + "Locked!");
                    main.lockingPlayers.remove(player.getUniqueId());
                }
                
                // Remainder of lockable blocks are only 1x1 so generic code will suffice.
                if (main.utilities.isGate(clickedBlock) || main.utilities.isBarrel(clickedBlock) || main.utilities.isTrapdoor(clickedBlock) || main.utilities.isFurnace(clickedBlock)) {
                	LockedBlock block = new LockedBlock(player.getUniqueId(), getPlayersFaction(player.getUniqueId(), main.factions).getName(), 
                			clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ(), clickedBlock.getWorld().getName());
                	main.lockedBlocks.add(block);
                	player.sendMessage(ChatColor.GREEN + "Locked!");
                	main.lockingPlayers.remove(player.getUniqueId());
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

    private void handleUnlockingBlock(PlayerInteractEvent event, Player player, Block clickedBlock) {
        // if locked
        if (main.utilities.isBlockLocked(clickedBlock)) {
            if (main.utilities.getLockedBlock(clickedBlock).getOwner().equals(player.getUniqueId())) {

                if (main.utilities.isChest(clickedBlock)) {
                    InventoryHolder holder = ((Chest) clickedBlock.getState()).getInventory().getHolder();
                    if (holder instanceof DoubleChest) {
                        // chest multi-unlock
                        DoubleChest doubleChest = (DoubleChest) holder;
                        Block leftChest = ((Chest) doubleChest.getLeftSide()).getBlock();
                        Block rightChest = ((Chest) doubleChest.getRightSide()).getBlock();

                        // unlock leftChest and rightChest
                        main.utilities.removeLock(leftChest);
                        main.utilities.removeLock(rightChest);

                        player.sendMessage(ChatColor.GREEN + "Unlocked!");
                        main.unlockingPlayers.remove(player.getUniqueId());
                    }
                    else {
                        // unlock single chest
                        main.utilities.removeLock(clickedBlock);
                        player.sendMessage(ChatColor.GREEN + "Unlocked!");
                        main.unlockingPlayers.remove(player.getUniqueId());
                    }
                }

                // door multi-unlock
                if (main.utilities.isDoor(clickedBlock)) {
                    // unlock initial block
                    main.utilities.removeLock(clickedBlock);
                    // check block above
                    if (main.utilities.isDoor(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() + 1, clickedBlock.getZ()))) {
                        main.utilities.removeLock(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() + 1, clickedBlock.getZ()));
                    }
                    // check block below
                    if (main.utilities.isDoor(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() - 1, clickedBlock.getZ()))) {
                        main.utilities.removeLock(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() - 1, clickedBlock.getZ()));
                    }

                    player.sendMessage(ChatColor.GREEN + "Unlocked!");
                    main.unlockingPlayers.remove(player.getUniqueId());
                }
                
                // single block size lock logic.
                if (main.utilities.isGate(clickedBlock) || main.utilities.isBarrel(clickedBlock) || main.utilities.isTrapdoor(clickedBlock) || main.utilities.isFurnace(clickedBlock)) {
                	main.utilities.removeLock(clickedBlock);

                	player.sendMessage(ChatColor.GREEN + "Unlocked!");
                    main.unlockingPlayers.remove(player.getUniqueId());
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

    private void handleClaimedChunk(PlayerInteractEvent event, ClaimedChunk chunk) {
        // player not in a faction and isn't overriding
        if (!isInFaction(event.getPlayer().getUniqueId(), main.factions) && !main.adminsBypassingProtections.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }

        // if player is in faction
        for (Faction faction : main.factions) {
            if (faction.isMember(event.getPlayer().getUniqueId())) {

                // if player's faction is not the same as the holder of the chunk and player isn't overriding
                if (!(faction.getName().equalsIgnoreCase(chunk.getHolder())) && !main.adminsBypassingProtections.contains(event.getPlayer().getUniqueId())) {

                    // if enemy territory
                    if (faction.isEnemy(chunk.getHolder())) {
                        // if not interacting with chest
                        if (isBlockInteractable(event)) {
                            // allow placing ladders
                            if (main.getConfig().getBoolean("laddersPlaceableInEnemyFactionTerritory")) {
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

    public boolean isBlockInteractable(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null) {
            // CHEST
            if (main.utilities.isChest(event.getClickedBlock())) {
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
        if (main.utilities.getLockedBlock(clickedBlock, main.lockedBlocks).getOwner() != player.getUniqueId()) {
            player.sendMessage(ChatColor.RED + "You are not the owner of this block!");
            return;
        }

        // if chest
        if (main.utilities.isChest(clickedBlock)) {
            InventoryHolder holder = ((Chest) clickedBlock.getState()).getInventory().getHolder();
            if (holder instanceof DoubleChest) { // if double chest
                // grant access to both chests
                DoubleChest doubleChest = (DoubleChest) holder;
                Block leftChest = ((Chest) doubleChest.getLeftSide()).getBlock();
                Block rightChest = ((Chest) doubleChest.getRightSide()).getBlock();

                main.utilities.getLockedBlock(leftChest, main.lockedBlocks).addToAccessList(main.playersGrantingAccess.get(player.getUniqueId()));
                main.utilities.getLockedBlock(rightChest, main.lockedBlocks).addToAccessList(main.playersGrantingAccess.get(player.getUniqueId()));

                player.sendMessage(ChatColor.GREEN + "Access granted to " + findPlayerNameBasedOnUUID(main.playersGrantingAccess.get(player.getUniqueId())));
                main.playersGrantingAccess.remove(player.getUniqueId());
            }
            else { // if single chest
                // grant access to single chest
                main.utilities.getLockedBlock(clickedBlock, main.lockedBlocks).addToAccessList(main.playersGrantingAccess.get(player.getUniqueId()));
                player.sendMessage(ChatColor.GREEN + "Access granted to " + findPlayerNameBasedOnUUID(main.playersGrantingAccess.get(player.getUniqueId())));
                main.playersGrantingAccess.remove(player.getUniqueId());
            }

        }

        // if door
        if (main.utilities.isDoor(clickedBlock)) {
            // grant access to initial block
            main.utilities.getLockedBlock(clickedBlock, main.lockedBlocks).addToAccessList(main.playersGrantingAccess.get(player.getUniqueId()));
            // check block above
            if (main.utilities.isDoor(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() + 1, clickedBlock.getZ()))) {
                main.utilities.getLockedBlock(clickedBlock, main.lockedBlocks).addToAccessList(main.playersGrantingAccess.get(player.getUniqueId()));
            }
            // check block below
            if (main.utilities.isDoor(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() - 1, clickedBlock.getZ()))) {
                main.utilities.getLockedBlock(clickedBlock, main.lockedBlocks).addToAccessList(main.playersGrantingAccess.get(player.getUniqueId()));
            }

            player.sendMessage(ChatColor.GREEN + "Access granted to " + findPlayerNameBasedOnUUID(main.playersGrantingAccess.get(player.getUniqueId())));
            main.playersGrantingAccess.remove(player.getUniqueId());
        }
        
        // if gate (or single-block sized lock)
        if (main.utilities.isGate(clickedBlock) || main.utilities.isBarrel(clickedBlock) || main.utilities.isTrapdoor(clickedBlock) || main.utilities.isFurnace(clickedBlock)) {
        	main.utilities.getLockedBlock(clickedBlock, main.lockedBlocks).addToAccessList(main.playersGrantingAccess.get(player.getUniqueId()));
        	
            player.sendMessage(ChatColor.GREEN + "Access granted to " + findPlayerNameBasedOnUUID(main.playersGrantingAccess.get(player.getUniqueId())));
            main.playersGrantingAccess.remove(player.getUniqueId());
        }
        
        event.setCancelled(true);
    }

    private void handleCheckingAccess(PlayerInteractEvent event, LockedBlock lockedBlock, Player player) {
        player.sendMessage(ChatColor.AQUA + "The following players have access to this block:");
        for (UUID playerUUID : lockedBlock.getAccessList()) {
            player.sendMessage(ChatColor.AQUA + " - " + findPlayerNameBasedOnUUID(playerUUID));
        }
        main.playersCheckingAccess.remove(player.getUniqueId());
        event.setCancelled(true);
    }

    private void handleRevokingAccess(PlayerInteractEvent event, Block clickedBlock, Player player) {

        // if not owner
        if (main.utilities.getLockedBlock(clickedBlock, main.lockedBlocks).getOwner() != player.getUniqueId()) {
            player.sendMessage(ChatColor.RED + "You are not the owner of this block!");
            return;
        }

        // if chest
        if (main.utilities.isChest(clickedBlock)) {
            InventoryHolder holder = ((Chest) clickedBlock.getState()).getInventory().getHolder();
            if (holder instanceof DoubleChest) { // if double chest
                // revoke access to both chests
                DoubleChest doubleChest = (DoubleChest) holder;
                Block leftChest = ((Chest) doubleChest.getLeftSide()).getBlock();
                Block rightChest = ((Chest) doubleChest.getRightSide()).getBlock();

                main.utilities.getLockedBlock(leftChest, main.lockedBlocks).removeFromAccessList(main.playersRevokingAccess.get(player.getUniqueId()));
                main.utilities.getLockedBlock(rightChest, main.lockedBlocks).removeFromAccessList(main.playersRevokingAccess.get(player.getUniqueId()));

                player.sendMessage(ChatColor.GREEN + "Access revoked for " + findPlayerNameBasedOnUUID(main.playersRevokingAccess.get(player.getUniqueId())));
                main.playersRevokingAccess.remove(player.getUniqueId());
            }
            else { // if single chest
                // revoke access to single chest
                main.utilities.getLockedBlock(clickedBlock, main.lockedBlocks).removeFromAccessList(main.playersRevokingAccess.get(player.getUniqueId()));
                player.sendMessage(ChatColor.GREEN + "Access revoked for " + findPlayerNameBasedOnUUID(main.playersRevokingAccess.get(player.getUniqueId())));
                main.playersRevokingAccess.remove(player.getUniqueId());
            }

        }

        // if door
        if (main.utilities.isDoor(clickedBlock)) {
            // revoke access to initial block
            main.utilities.getLockedBlock(clickedBlock, main.lockedBlocks).removeFromAccessList(main.playersRevokingAccess.get(player.getUniqueId()));
            // check block above
            if (main.utilities.isDoor(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() + 1, clickedBlock.getZ()))) {
                main.utilities.getLockedBlock(clickedBlock, main.lockedBlocks).removeFromAccessList(main.playersRevokingAccess.get(player.getUniqueId()));
            }
            // check block below
            if (main.utilities.isDoor(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() - 1, clickedBlock.getZ()))) {
                main.utilities.getLockedBlock(clickedBlock, main.lockedBlocks).removeFromAccessList(main.playersRevokingAccess.get(player.getUniqueId()));
            }

            player.sendMessage(ChatColor.GREEN + "Access revoked for " + findPlayerNameBasedOnUUID(main.playersRevokingAccess.get(player.getUniqueId())));
            main.playersRevokingAccess.remove(player.getUniqueId());
        }
        
        // if gate or other single-block sized lock
        if (main.utilities.isGate(clickedBlock) || main.utilities.isBarrel(clickedBlock) || main.utilities.isTrapdoor(clickedBlock) || main.utilities.isFurnace(clickedBlock)) {
        	main.utilities.getLockedBlock(clickedBlock, main.lockedBlocks).removeFromAccessList(main.playersRevokingAccess.get(player.getUniqueId()));

            player.sendMessage(ChatColor.GREEN + "Access revoked for " + findPlayerNameBasedOnUUID(main.playersRevokingAccess.get(player.getUniqueId())));
            main.playersRevokingAccess.remove(player.getUniqueId());
        }
        
        event.setCancelled(true);

    }

}
