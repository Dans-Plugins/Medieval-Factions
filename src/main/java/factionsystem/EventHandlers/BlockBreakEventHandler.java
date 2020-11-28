package factionsystem.EventHandlers;

import factionsystem.Main;
import factionsystem.Objects.ClaimedChunk;
import factionsystem.Objects.Faction;
import factionsystem.Objects.LockedBlock;
import factionsystem.Objects.Gate;
import factionsystem.Subsystems.UtilitySubsystem;

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
        ClaimedChunk chunk = getClaimedChunk(event.getBlock().getLocation().getChunk().getX(), event.getBlock().getLocation().getChunk().getZ(), event.getBlock().getWorld().getName(), main.claimedChunks);

        // if chunk is claimed
        if (chunk != null) {

            // player not in a faction
            if (!isInFaction(event.getPlayer().getUniqueId(), main.factions)) {
                event.setCancelled(true);
            }

            // if player is in faction
            for (Faction faction : main.factions) {
                if (faction.isMember(player.getUniqueId())) {

                    // if player's faction is not the same as the holder of the chunk and player isn't bypassing
                    if (!(faction.getName().equalsIgnoreCase(chunk.getHolder())) && !main.adminsBypassingProtections.contains(player.getUniqueId())) {
                        event.setCancelled(true);
                        return;
                    }

                    // if block is locked
                    if (main.utilities.isBlockLocked(event.getBlock())) {

                        // if player is not the owner and isn't bypassing
                        if (!main.utilities.getLockedBlock(event.getBlock()).getOwner().equals(player.getUniqueId())
                                && !main.adminsBypassingProtections.contains(event.getPlayer().getUniqueId())) {
                            event.setCancelled(true);
                            player.sendMessage(ChatColor.RED + "You don't own this!");
                            return;
                        }

                    	UtilitySubsystem.removeLock(event.getBlock(), main.lockedBlocks);

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

}
