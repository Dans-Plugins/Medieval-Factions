package dansplugins.factionsystem.eventhandlers;

import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.services.ConfigService;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;

public class SpawnHandler implements Listener {
    private final ConfigService configService;
    private final PersistentData persistentData;

    public SpawnHandler(ConfigService configService, PersistentData persistentData) {
        this.configService = configService;
        this.persistentData = persistentData;
    }

    @EventHandler()
    public void handle(EntitySpawnEvent event) {
        if (isLandClaimed(event) && event.getEntity() instanceof Monster && !configService.getBoolean("mobsSpawnInFactionTerritory")) {
            event.setCancelled(true);
        }
    }

    private boolean isLandClaimed(EntitySpawnEvent event) {
        return persistentData.getChunkDataAccessor().isClaimed(event.getLocation().getChunk());
    }
}