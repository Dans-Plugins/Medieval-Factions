package factionsystem.EventHandlers;

import factionsystem.Main;
import factionsystem.Objects.ClaimedChunk;
import factionsystem.Objects.Faction;
import factionsystem.Objects.LockedBlock;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

import static factionsystem.Subsystems.UtilitySubsystem.*;
import static org.bukkit.Material.LADDER;

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

            // if chunk is claimed
            ClaimedChunk chunk = getClaimedChunk(event.getClickedBlock().getLocation().getChunk().getX(), event.getClickedBlock().getLocation().getChunk().getZ(), main.claimedChunks);
            if (chunk != null) {
                handleClaimedChunk(event, chunk);
            }

            // ---------------------------------------------------------------------------------------------------------------

            // if block is locked
            LockedBlock lockedBlock = main.utilities.getLockedBlock(clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ());
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

            }
            else {
                // if player is using an access command
                if (main.playersGrantingAccess.containsKey(player.getUniqueId()) ||
                    main.playersCheckingAccess.contains(player.getUniqueId()) ||
                    main.playersRevokingAccess.containsKey(player.getUniqueId())) {

                    player.sendMessage(ChatColor.RED + "That block isn't locked!");
                }
            }
        }
    }

    private void handleLockingBlock(PlayerInteractEvent event, Player player, Block clickedBlock) {
        // if chunk is claimed
        ClaimedChunk chunk = getClaimedChunk(event.getClickedBlock().getLocation().getChunk().getX(), event.getClickedBlock().getLocation().getChunk().getZ(), main.claimedChunks);
        if (chunk != null) {

            // if claimed by other faction
            if (!chunk.getHolder().equalsIgnoreCase(getPlayersFaction(player.getUniqueId(), main.factions).getName())) {
                player.sendMessage(ChatColor.RED + "You can only lock things in your faction's territory!");
                event.setCancelled(true);
                return;
            }

            // if already locked
            if (main.utilities.isBlockLocked(clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ())) {
                player.sendMessage(ChatColor.RED + "This block is already locked!");
                event.setCancelled(true);
                return;
            }

            // block type check
            if (main.utilities.isDoor(clickedBlock) || main.utilities.isChest(clickedBlock)) {

                if (main.utilities.isChest(clickedBlock)) {
                    InventoryHolder holder = ((Chest) clickedBlock.getState()).getInventory().getHolder();
                    if (holder instanceof DoubleChest) {
                        // chest multi-lock
                        DoubleChest doubleChest = (DoubleChest) holder;
                        Block leftChest = ((Chest) doubleChest.getLeftSide()).getBlock();
                        Block rightChest = ((Chest) doubleChest.getRightSide()).getBlock();

                        LockedBlock left = new LockedBlock(player.getUniqueId(), getPlayersFaction(player.getUniqueId(), main.factions).getName(), leftChest.getX(), leftChest.getY(), leftChest.getZ());
                        main.lockedBlocks.add(left);

                        LockedBlock right = new LockedBlock(player.getUniqueId(), getPlayersFaction(player.getUniqueId(), main.factions).getName(), rightChest.getX(), rightChest.getY(), rightChest.getZ());
                        main.lockedBlocks.add(right);

                        player.sendMessage(ChatColor.GREEN + "Locked!");
                        main.lockingPlayers.remove(player.getUniqueId());
                    }
                    else {
                        // lock single chest
                        LockedBlock single = new LockedBlock(player.getUniqueId(), getPlayersFaction(player.getUniqueId(), main.factions).getName(), clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ());
                        main.lockedBlocks.add(single);

                        player.sendMessage(ChatColor.GREEN + "Locked!");
                        main.lockingPlayers.remove(player.getUniqueId());
                    }
                }

                // door multi-lock
                if (main.utilities.isDoor(clickedBlock)) {
                    // lock initial block
                    LockedBlock initial = new LockedBlock(player.getUniqueId(), getPlayersFaction(player.getUniqueId(), main.factions).getName(), clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ());
                    main.lockedBlocks.add(initial);
                    // check block above
                    if (main.utilities.isDoor(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() + 1, clickedBlock.getZ()))) {
                        LockedBlock newLockedBlock2 = new LockedBlock(player.getUniqueId(), getPlayersFaction(player.getUniqueId(), main.factions).getName(), clickedBlock.getX(), clickedBlock.getY() + 1, clickedBlock.getZ());
                        main.lockedBlocks.add(newLockedBlock2);
                    }
                    // check block below
                    if (main.utilities.isDoor(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() - 1, clickedBlock.getZ()))) {
                        LockedBlock newLockedBlock2 = new LockedBlock(player.getUniqueId(), getPlayersFaction(player.getUniqueId(), main.factions).getName(), clickedBlock.getX(), clickedBlock.getY() - 1, clickedBlock.getZ());
                        main.lockedBlocks.add(newLockedBlock2);
                    }

                    player.sendMessage(ChatColor.GREEN + "Locked!");
                    main.lockingPlayers.remove(player.getUniqueId());
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

    private void handleUnlockingBlock(PlayerInteractEvent event, Player player, Block clickedBlock) {
        // if locked
        if (main.utilities.isBlockLocked(clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ())) {
            if (main.utilities.getLockedBlock(clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ()).getOwner().equals(player.getUniqueId())) {

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

                    if (main.getConfig().getBoolean("laddersPlaceableInEnemyFactionTerritory")) {
                        // if trying to place ladder on enemy territory
                        if (event.getMaterial() == LADDER && faction.isEnemy(chunk.getHolder())) {
                            return;
                        }
                    }

                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    private void handleGrantingAccess(PlayerInteractEvent event, Block clickedBlock, Player player) {

        // if not owner
        if (main.utilities.getLockedBlock(clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ()).getOwner() != player.getUniqueId()) {
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

                main.utilities.getLockedBlock(leftChest.getX(), leftChest.getY(), leftChest.getZ()).addToAccessList(main.playersGrantingAccess.get(player.getUniqueId()));
                main.utilities.getLockedBlock(rightChest.getX(), rightChest.getY(), rightChest.getZ()).addToAccessList(main.playersGrantingAccess.get(player.getUniqueId()));

                player.sendMessage(ChatColor.GREEN + "Access granted to " + findPlayerNameBasedOnUUID(main.playersGrantingAccess.get(player.getUniqueId())));
                main.playersGrantingAccess.remove(player.getUniqueId());
            }
            else { // if single chest
                // grant access to single chest
                main.utilities.getLockedBlock(clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ()).addToAccessList(main.playersGrantingAccess.get(player.getUniqueId()));
                player.sendMessage(ChatColor.GREEN + "Access granted to " + findPlayerNameBasedOnUUID(main.playersGrantingAccess.get(player.getUniqueId())));
                main.playersGrantingAccess.remove(player.getUniqueId());
            }

        }

        // if door
        if (main.utilities.isDoor(clickedBlock)) {
            // grant access to initial block
            main.utilities.getLockedBlock(clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ()).addToAccessList(main.playersGrantingAccess.get(player.getUniqueId()));
            // check block above
            if (main.utilities.isDoor(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() + 1, clickedBlock.getZ()))) {
                main.utilities.getLockedBlock(clickedBlock.getX(), clickedBlock.getY() + 1, clickedBlock.getZ()).addToAccessList(main.playersGrantingAccess.get(player.getUniqueId()));
            }
            // check block below
            if (main.utilities.isDoor(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() - 1, clickedBlock.getZ()))) {
                main.utilities.getLockedBlock(clickedBlock.getX(), clickedBlock.getY() - 1, clickedBlock.getZ()).addToAccessList(main.playersGrantingAccess.get(player.getUniqueId()));
            }

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
        if (main.utilities.getLockedBlock(clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ()).getOwner() != player.getUniqueId()) {
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

                main.utilities.getLockedBlock(leftChest.getX(), leftChest.getY(), leftChest.getZ()).removeFromAccessList(main.playersRevokingAccess.get(player.getUniqueId()));
                main.utilities.getLockedBlock(rightChest.getX(), rightChest.getY(), rightChest.getZ()).removeFromAccessList(main.playersRevokingAccess.get(player.getUniqueId()));

                player.sendMessage(ChatColor.GREEN + "Access revoked for " + findPlayerNameBasedOnUUID(main.playersRevokingAccess.get(player.getUniqueId())));
                main.playersRevokingAccess.remove(player.getUniqueId());
            }
            else { // if single chest
                // revoke access to single chest
                main.utilities.getLockedBlock(clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ()).removeFromAccessList(main.playersRevokingAccess.get(player.getUniqueId()));
                player.sendMessage(ChatColor.GREEN + "Access revoked for " + findPlayerNameBasedOnUUID(main.playersRevokingAccess.get(player.getUniqueId())));
                main.playersRevokingAccess.remove(player.getUniqueId());
            }

        }

        // if door
        if (main.utilities.isDoor(clickedBlock)) {
            // revoke access to initial block
            main.utilities.getLockedBlock(clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ()).removeFromAccessList(main.playersRevokingAccess.get(player.getUniqueId()));
            // check block above
            if (main.utilities.isDoor(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() + 1, clickedBlock.getZ()))) {
                main.utilities.getLockedBlock(clickedBlock.getX(), clickedBlock.getY() + 1, clickedBlock.getZ()).removeFromAccessList(main.playersRevokingAccess.get(player.getUniqueId()));
            }
            // check block below
            if (main.utilities.isDoor(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() - 1, clickedBlock.getZ()))) {
                main.utilities.getLockedBlock(clickedBlock.getX(), clickedBlock.getY() - 1, clickedBlock.getZ()).removeFromAccessList(main.playersRevokingAccess.get(player.getUniqueId()));
            }

            player.sendMessage(ChatColor.GREEN + "Access revoked for " + findPlayerNameBasedOnUUID(main.playersRevokingAccess.get(player.getUniqueId())));
            main.playersRevokingAccess.remove(player.getUniqueId());
        }
        event.setCancelled(true);

    }

}
