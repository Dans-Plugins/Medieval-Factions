package dansplugins.factionsystem.eventhandlers;

import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.PersistentData;

public class SpawnHandler implements Listener {

    @EventHandler()
    public void handle(EntitySpawnEvent event) {
        if (isLandClaimed(event) && event.getEntity() instanceof Monster && !MedievalFactions.getInstance().getConfig().getBoolean("mobsSpawnInFactionTerritory")) {
            event.setCancelled(true);
        }
    }

    private boolean isLandClaimed(EntitySpawnEvent event) {
        return PersistentData.getInstance().getChunkDataAccessor().isClaimed(event.getLocation().getChunk());
    }
}