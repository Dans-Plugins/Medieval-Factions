package factionsystem.EventHandlers;

import factionsystem.MedievalFactions;
import factionsystem.Objects.ClaimedChunk;
import factionsystem.Objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import static factionsystem.Subsystems.UtilitySubsystem.getClaimedChunk;
import static factionsystem.Subsystems.UtilitySubsystem.isInFaction;
import static org.bukkit.Material.LADDER;

public class BlockPlaceEventHandler implements Listener {

    @EventHandler()
    public void handle(BlockPlaceEvent event) {
        // get player
        Player player = event.getPlayer();

        // get chunk
        ClaimedChunk chunk = getClaimedChunk(event.getBlock().getLocation().getChunk().getX(), event.getBlock().getLocation().getChunk().getZ(),
        		event.getBlock().getWorld().getName(), MedievalFactions.getInstance().claimedChunks);

        // if chunk is claimed
        if (chunk != null) {

            // player not in a faction
            if (!isInFaction(event.getPlayer().getUniqueId(), MedievalFactions.getInstance().factions) && !MedievalFactions.getInstance().adminsBypassingProtections.contains(event.getPlayer().getUniqueId())) {
                event.setCancelled(true);
            }

            // if player is in faction
            for (Faction faction : MedievalFactions.getInstance().factions) {
                if (faction.isMember(player.getUniqueId())) {

                    // if player's faction is not the same as the holder of the chunk
                    if (!(faction.getName().equalsIgnoreCase(chunk.getHolder())) && !MedievalFactions.getInstance().adminsBypassingProtections.contains(event.getPlayer().getUniqueId())) {

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
                        if (isNextToNonOwnedLockedChest(event.getPlayer(), event.getBlock()) && !MedievalFactions.getInstance().adminsBypassingProtections.contains(event.getPlayer().getUniqueId())) {
                            event.setCancelled(true);
                            player.sendMessage(ChatColor.RED + "You can't place chests next to locked chests you don't own.");
                            return;
                        }
                    }

                    // if hopper
                    if (event.getBlock().getType() == Material.HOPPER) {
                        // if next to or under/above non-owned locked chest
                        if (isNextToNonOwnedLockedChest(event.getPlayer(), event.getBlock()) || isUnderOrAboveNonOwnedLockedChest(event.getPlayer(), event.getBlock()) && !MedievalFactions.getInstance().adminsBypassingProtections.contains(event.getPlayer().getUniqueId())) {
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
}
