package factionsystem.EventHandlers;

import factionsystem.Main;
import factionsystem.Objects.ClaimedChunk;
import factionsystem.Objects.Faction;
import factionsystem.Objects.LockedBlock;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

import static factionsystem.Subsystems.UtilitySubsystem.getClaimedChunk;
import static factionsystem.Subsystems.UtilitySubsystem.isInFaction;

public class BlockBreakEventHandler {

    Main main = null;

    public BlockBreakEventHandler(Main plugin) {
        main = plugin;
    }

    public void handle(BlockBreakEvent event) {
        // get player
        Player player = event.getPlayer();

        // get chunk
        ClaimedChunk chunk = getClaimedChunk(event.getBlock().getLocation().getChunk().getX(), event.getBlock().getLocation().getChunk().getZ(), main.claimedChunks);

        // if chunk is claimed
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

                    // if block is locked
                    if (main.utilities.isBlockLocked(event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ())) {

                        // if player is not the owner
                        if (!main.utilities.getLockedBlock(event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ()).getOwner().equalsIgnoreCase(player.getName())) {
                            event.setCancelled(true);
                            player.sendMessage(ChatColor.RED + "You don't own this!");
                            return;
                        }

                        for (LockedBlock block : main.lockedBlocks) {
                            if (block.getX() == event.getBlock().getX() && block.getY() == event.getBlock().getY() && block.getZ() == event.getBlock().getZ()) {
                                main.lockedBlocks.remove(block);
                                break;
                            }
                        }

                    }
                }
            }
        }
    }

}
