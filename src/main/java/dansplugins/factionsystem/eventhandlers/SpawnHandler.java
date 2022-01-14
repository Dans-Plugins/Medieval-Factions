package dansplugins.factionsystem.eventhandlers;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.services.LocalChunkService;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;

public class SpawnHandler implements Listener {

    @EventHandler()
    public void handle(EntitySpawnEvent event) {
        if (isLandClaimed(event) && event.getEntity() instanceof Monster && !MedievalFactions.getInstance().getConfig().getBoolean("mobsSpawnInFactionTerritory")) {
            event.setCancelled(true);
        }
    }

    private boolean isLandClaimed(EntitySpawnEvent event) {
        return LocalChunkService.getInstance().isClaimed(event.getLocation().getChunk(), PersistentData.getInstance().getClaimedChunks());
    }
}