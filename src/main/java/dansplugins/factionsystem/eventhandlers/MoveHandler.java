package dansplugins.factionsystem.eventhandlers;

import dansplugins.factionsystem.DynmapIntegrator;
import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.managers.ChunkManager;
import dansplugins.factionsystem.managers.LocaleManager;
import dansplugins.factionsystem.objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Objects;

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
            Player player =  event.getPlayer();
            Faction faction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());
            if (faction != null && faction.isOwner(player.getUniqueId())) {
                if (faction.getAutoClaimStatus()) {

                    // if not at demesne limit
                    Faction playersFaction = PersistentData.getInstance().getPlayersFaction(event.getPlayer().getUniqueId());
                    if (ChunkManager.getInstance().getChunksClaimedByFaction(playersFaction.getName(), PersistentData.getInstance().getClaimedChunks()) < playersFaction.getCumulativePowerLevel()) {
                        getServer().getScheduler().runTaskLater(MedievalFactions.getInstance(), new Runnable() {
                            @Override
                            public void run() {
                                // add new chunk to claimed chunks
                                ChunkManager.getInstance().claimChunkAtLocation(player, player.getLocation(), faction);
                                DynmapIntegrator.getInstance().updateClaims();
                            }
                        }, 1); // delayed by 1 tick (1/20th of a second) because otherwise players will claim the chunk they just left
                    }
                    else {
                        event.getPlayer().sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertReachedDemesne"));
                    }
                }
            }

            // if new chunk is claimed and old chunk was not
            if (ChunkManager.getInstance().isClaimed(event.getTo().getChunk(), PersistentData.getInstance().getClaimedChunks()) && !ChunkManager.getInstance().isClaimed(event.getFrom().getChunk(), PersistentData.getInstance().getClaimedChunks())) {
                String title = ChunkManager.getInstance().getClaimedChunk(event.getTo().getChunk()).getHolder();
                sendPlayerLandInfo(event.getPlayer(), title);
                return;
            }

            // if new chunk is unclaimed and old chunk was not
            if (!ChunkManager.getInstance().isClaimed(event.getTo().getChunk(), PersistentData.getInstance().getClaimedChunks()) && ChunkManager.getInstance().isClaimed(event.getFrom().getChunk(), PersistentData.getInstance().getClaimedChunks())) {
                String title = LocaleManager.getInstance().getText("Wilderness");
                sendPlayerLandInfo(event.getPlayer(), title);
                return;
            }

            // if new chunk is claimed and old chunk was also claimed
            if (ChunkManager.getInstance().isClaimed(event.getTo().getChunk(), PersistentData.getInstance().getClaimedChunks()) && ChunkManager.getInstance().isClaimed(event.getFrom().getChunk(), PersistentData.getInstance().getClaimedChunks())) {
                // if chunk holders are not equal
                if (!(ChunkManager.getInstance().getClaimedChunk(event.getFrom().getChunk()).getHolder().equalsIgnoreCase(ChunkManager.getInstance().getClaimedChunk(event.getTo().getChunk()).getHolder()))) {
                    String title = ChunkManager.getInstance().getClaimedChunk(event.getTo().getChunk()).getHolder();
                    sendPlayerLandInfo(event.getPlayer(), title);
                }
            }

        }
    }

    public void sendPlayerLandInfo(Player player, String information) {
        if (MedievalFactions.getInstance().getConfig().getBoolean("territoryAlertPopUp")) {
            String subtitle = null;
            int fadeIn = 10;
            int stay = 70;
            int fadeOut = 20;
            player.sendTitle(ChatColor.AQUA + information, subtitle, fadeIn, stay, fadeOut);
        }
        else {
            player.sendMessage(ChatColor.AQUA + information);
        }
    }

}
