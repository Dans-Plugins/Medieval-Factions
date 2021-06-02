package dansplugins.factionsystem.managers;

import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.ClaimedChunk;
import dansplugins.factionsystem.objects.LockedBlock;
import dansplugins.factionsystem.utils.BlockChecker;
import dansplugins.factionsystem.utils.UUIDChecker;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class LockManager {
    private static LockManager instance;

    private LockManager() {

    }

    public static LockManager getInstance() {
        if (instance == null) {
            instance = new LockManager();
        }
        return instance;
    }

    public void handleLockingBlock(PlayerInteractEvent event, Player player, Block clickedBlock) {
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

    public void handleUnlockingBlock(PlayerInteractEvent event, Player player, Block clickedBlock) {
        // if locked
        if (PersistentData.getInstance().isBlockLocked(clickedBlock)) {
            if (PersistentData.getInstance().getLockedBlock(clickedBlock).getOwner().equals(player.getUniqueId())
                || EphemeralData.getInstance().getForcefullyUnlockingPlayers().contains(player.getUniqueId())) {

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

                // remove player from forcefully unlocking players list if they are in it
                EphemeralData.getInstance().getForcefullyUnlockingPlayers().remove(player.getUniqueId());

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

    public void removeLock(Block block) {
        for (LockedBlock b : PersistentData.getInstance().getLockedBlocks()) {
            if (b.getX() == block.getX() && b.getY() == block.getY() && b.getZ() == block.getZ() && block.getWorld().getName().equalsIgnoreCase(b.getWorld())) {
                PersistentData.getInstance().getLockedBlocks().remove(b);
                return;
            }
        }
    }

    public void handleGrantingAccess(PlayerInteractEvent event, Block clickedBlock, Player player) {

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

    public void handleCheckingAccess(PlayerInteractEvent event, LockedBlock lockedBlock, Player player) {
        player.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("FollowingPlayersHaveAccess"));
        for (UUID playerUUID : lockedBlock.getAccessList()) {
            player.sendMessage(ChatColor.AQUA + " - " + UUIDChecker.getInstance().findPlayerNameBasedOnUUID(playerUUID));
        }
        EphemeralData.getInstance().getPlayersCheckingAccess().remove(player.getUniqueId());
        event.setCancelled(true);
    }

    public void handleRevokingAccess(PlayerInteractEvent event, Block clickedBlock, Player player) {

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

}
