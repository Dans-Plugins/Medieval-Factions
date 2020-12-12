package factionsystem.EventHandlers;

import factionsystem.MedievalFactions;
import factionsystem.Objects.Faction;
import factionsystem.Data.PersistentData;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Objects;

import static factionsystem.Util.Utilities.*;
import static org.bukkit.Bukkit.getServer;

public class MoveHandler implements Listener {

    @EventHandler()
    public void handle(PlayerMoveEvent event) {
        // Full disclosure, I feel like this method might be extremely laggy, especially if a player is travelling.
        // May have to optimise this, or just not have this mechanic.
        // - Dan
        // if player enters a new chunk
        if (event.getFrom().getChunk() != Objects.requireNonNull(event.getTo()).getChunk()) {

            // auto claim check
            for (Faction faction : PersistentData.getInstance().getFactions()) {
                if (faction.isOwner(event.getPlayer().getUniqueId())) {

                    if (faction.getAutoClaimStatus()) {

                        // if not at demesne limit
                        Faction playersFaction = getPlayersFaction(event.getPlayer().getUniqueId(), PersistentData.getInstance().getFactions());
                        if (getChunksClaimedByFaction(playersFaction.getName(), PersistentData.getInstance().getClaimedChunks()) < playersFaction.getCumulativePowerLevel()) {
                            getServer().getScheduler().runTaskLater(MedievalFactions.getInstance(), new Runnable() {
                                @Override
                                public void run() {
                                    // add new chunk to claimed chunks
                                    MedievalFactions.getInstance().utilities.addChunkAtPlayerLocation(event.getPlayer());
                                }
                            }, 1); // delayed by 1 tick (1/20th of a second) because otherwise players will claim the chunk they just left
                        }
                        else {
                            event.getPlayer().sendMessage(ChatColor.RED + "You have reached your demesne limit! Invite more players to increase this.");
                        }
                    }
                }
            }

            // if new chunk is claimed and old chunk was not
            if (isClaimed(event.getTo().getChunk(), PersistentData.getInstance().getClaimedChunks()) && !isClaimed(event.getFrom().getChunk(), PersistentData.getInstance().getClaimedChunks())) {
                String title = getClaimedChunk(event.getTo().getChunk().getX(), event.getTo().getChunk().getZ(), event.getTo().getChunk().getWorld().getName(), PersistentData.getInstance().getClaimedChunks()).getHolder();
                event.getPlayer().sendTitle(title, null, 10, 70, 20);
                return;
            }

            // if new chunk is unclaimed and old chunk was not
            if (!isClaimed(event.getTo().getChunk(), PersistentData.getInstance().getClaimedChunks()) && isClaimed(event.getFrom().getChunk(), PersistentData.getInstance().getClaimedChunks())) {
                event.getPlayer().sendTitle("Wilderness", null, 10, 70, 20);
                return;
            }

            // if new chunk is claimed and old chunk was also claimed
            if (isClaimed(event.getTo().getChunk(), PersistentData.getInstance().getClaimedChunks()) && isClaimed(event.getFrom().getChunk(), PersistentData.getInstance().getClaimedChunks())) {
                // if chunk holders are not equal
                if (!(getClaimedChunk(event.getFrom().getChunk().getX(), event.getFrom().getChunk().getZ(), event.getFrom().getWorld().getName(), PersistentData.getInstance().getClaimedChunks()).getHolder().equalsIgnoreCase(getClaimedChunk(event.getTo().getChunk().getX(), event.getTo().getChunk().getZ(), event.getTo().getChunk().getWorld().getName(), PersistentData.getInstance().getClaimedChunks()).getHolder()))) {
                    String title = getClaimedChunk(event.getTo().getChunk().getX(), event.getTo().getChunk().getZ(), event.getTo().getChunk().getWorld().getName(), PersistentData.getInstance().getClaimedChunks()).getHolder();
                    event.getPlayer().sendTitle(title, null, 10, 70, 20);
                }
            }

        }
    }

}
