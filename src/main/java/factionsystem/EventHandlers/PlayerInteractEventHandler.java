package factionsystem.EventHandlers;

import factionsystem.Main;
import factionsystem.Objects.ClaimedChunk;
import factionsystem.Objects.Faction;
import factionsystem.Objects.LockedBlock;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;

import static factionsystem.Utility.UtilityFunctions.*;

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
            if (main.lockingPlayers.contains(player.getName())) {
                // if chunk is claimed
                ClaimedChunk chunk = getClaimedChunk(event.getClickedBlock().getLocation().getChunk().getX(), event.getClickedBlock().getLocation().getChunk().getZ(), main.claimedChunks);
                if (chunk != null) {

                    // if claimed by other faction
                    if (!chunk.getHolder().equalsIgnoreCase(getPlayersFaction(player.getName(), main.factions).getName())) {
                        player.sendMessage(ChatColor.RED + "You can only lock things in your faction's territory!");
                        event.setCancelled(true);
                        return;
                    }

                    // if already locked
                    if (main.isBlockLocked(clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ())) {
                        player.sendMessage(ChatColor.RED + "This block is already locked!");
                        event.setCancelled(true);
                        return;
                    }

                    // block type check
                    if (main.isDoor(clickedBlock) || main.isChest(clickedBlock)) {

                        if (main.isChest(clickedBlock)) {
                            InventoryHolder holder = ((Chest) clickedBlock.getState()).getInventory().getHolder();
                            if (holder instanceof DoubleChest) {
                                // chest multi-lock
                                DoubleChest doubleChest = (DoubleChest) holder;
                                Block leftChest = ((Chest) doubleChest.getLeftSide()).getBlock();
                                Block rightChest = ((Chest) doubleChest.getRightSide()).getBlock();

                                LockedBlock left = new LockedBlock(player.getName(), getPlayersFaction(player.getName(), main.factions).getName(), leftChest.getX(), leftChest.getY(), leftChest.getZ());
                                main.lockedBlocks.add(left);

                                LockedBlock right = new LockedBlock(player.getName(), getPlayersFaction(player.getName(), main.factions).getName(), rightChest.getX(), rightChest.getY(), rightChest.getZ());
                                main.lockedBlocks.add(right);

                                player.sendMessage(ChatColor.GREEN + "Locked!");
                                main.lockingPlayers.remove(player.getName());
                            }
                            else {
                                // lock single chest
                                LockedBlock single = new LockedBlock(player.getName(), getPlayersFaction(player.getName(), main.factions).getName(), clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ());
                                main.lockedBlocks.add(single);
                            }
                        }

                        // door multi-lock
                        if (main.isDoor(clickedBlock)) {
                            // lock initial block
                            LockedBlock initial = new LockedBlock(player.getName(), getPlayersFaction(player.getName(), main.factions).getName(), clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ());
                            main.lockedBlocks.add(initial);
                            // check block above
                            if (main.isDoor(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() + 1, clickedBlock.getZ()))) {
                                LockedBlock newLockedBlock2 = new LockedBlock(player.getName(), getPlayersFaction(player.getName(), main.factions).getName(), clickedBlock.getX(), clickedBlock.getY() + 1, clickedBlock.getZ());
                                main.lockedBlocks.add(newLockedBlock2);
                            }
                            // check block below
                            if (main.isDoor(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() - 1, clickedBlock.getZ()))) {
                                LockedBlock newLockedBlock2 = new LockedBlock(player.getName(), getPlayersFaction(player.getName(), main.factions).getName(), clickedBlock.getX(), clickedBlock.getY() - 1, clickedBlock.getZ());
                                main.lockedBlocks.add(newLockedBlock2);
                            }

                            player.sendMessage(ChatColor.GREEN + "Locked!");
                            main.lockingPlayers.remove(player.getName());
                        }

                        event.setCancelled(true);
                        return;
                    }
                    else {
                        player.sendMessage(ChatColor.RED + "You can only lock chests or doors.");
                        return;
                    }

                }
                else {
                    player.sendMessage(ChatColor.RED + "You can only lock blocks on land claimed by your faction!");
                    event.setCancelled(true);
                    return;
                }
            }

            // if player is trying to unlock a block
            if (main.unlockingPlayers.contains(player.getName())) {
                // if locked
                if (main.isBlockLocked(clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ())) {
                    if (main.getLockedBlock(clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ()).getOwner().equalsIgnoreCase(player.getName())) {

                        if (main.isChest(clickedBlock)) {
                            InventoryHolder holder = ((Chest) clickedBlock.getState()).getInventory().getHolder();
                            if (holder instanceof DoubleChest) {
                                // chest multi-unlock
                                DoubleChest doubleChest = (DoubleChest) holder;
                                Block leftChest = ((Chest) doubleChest.getLeftSide()).getBlock();
                                Block rightChest = ((Chest) doubleChest.getRightSide()).getBlock();

                                // unlock leftChest and rightChest
                                main.removeLock(leftChest);
                                main.removeLock(rightChest);

                                player.sendMessage(ChatColor.GREEN + "Unlocked!");
                                main.unlockingPlayers.remove(player.getName());
                            }
                            else {
                                // unlock single chest
                                main.removeLock(clickedBlock);
                                player.sendMessage(ChatColor.GREEN + "Unlocked!");
                                main.unlockingPlayers.remove(player.getName());
                            }
                        }

                        // door multi-unlock
                        if (main.isDoor(clickedBlock)) {
                            // unlock initial block
                            main.removeLock(clickedBlock);
                            // check block above
                            if (main.isDoor(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() + 1, clickedBlock.getZ()))) {
                                main.removeLock(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() + 1, clickedBlock.getZ()));
                            }
                            // check block below
                            if (main.isDoor(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() - 1, clickedBlock.getZ()))) {
                                main.removeLock(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() - 1, clickedBlock.getZ()));
                            }

                            player.sendMessage(ChatColor.GREEN + "Unlocked!");
                            main.unlockingPlayers.remove(player.getName());
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

            // if chunk is claimed
            ClaimedChunk chunk = getClaimedChunk(event.getClickedBlock().getLocation().getChunk().getX(), event.getClickedBlock().getLocation().getChunk().getZ(), main.claimedChunks);
            if (chunk != null) {

                // player not in a faction
                if (!isInFaction(event.getPlayer().getName(), main.factions)) {
                    event.setCancelled(true);
                }

                // if player is in faction
                for (Faction faction : main.factions) {
                    if (faction.isMember(player.getName())) {

                        // if player's faction is not the same as the holder of the chunk
                        if (!(faction.getName().equalsIgnoreCase(chunk.getHolder()))) {
                            event.setCancelled(true);
                            return;
                        }
                    }
                }
            }

            // if block is locked
            LockedBlock lockedBlock = main.getLockedBlock(clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ());
            if (lockedBlock != null) {

                // if player doesn't have access
                if (!lockedBlock.hasAccess(player.getName())) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Locked by " + lockedBlock.getOwner());
                    return;
                }

            }
        }
    }

}
