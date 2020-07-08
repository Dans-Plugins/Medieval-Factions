package factionsystem.EventHandlers;

import factionsystem.Main;
import factionsystem.Objects.ClaimedChunk;
import factionsystem.Objects.Faction;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;

import static factionsystem.Utility.UtilityFunctions.getClaimedChunk;
import static factionsystem.Utility.UtilityFunctions.isInFaction;

public class BlockPlaceEventHandler {
    Main main = null;

    public BlockPlaceEventHandler(Main plugin) {
        main = plugin;
    }

    public void handle(BlockPlaceEvent event) {
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
                }
            }
        }
    }
}
