package factionsystem.EventHandlers;

import factionsystem.MedievalFactions;
import factionsystem.Subsystems.UtilitySubsystem;
import org.bukkit.entity.Monster;
import org.bukkit.event.entity.EntitySpawnEvent;

public class EntitySpawnEventHandler {

    public void handle(EntitySpawnEvent event) {

        int x = 0;
        int z = 0;

        x = event.getEntity().getLocation().getChunk().getX();
        z = event.getEntity().getLocation().getChunk().getZ();

        // check if land is claimed
        if (UtilitySubsystem.isClaimed(event.getLocation().getChunk(), MedievalFactions.getInstance().claimedChunks))
        {
            if (event.getEntity() instanceof Monster && !MedievalFactions.getInstance().getConfig().getBoolean("mobsSpawnInFactionTerritory")) {
                event.setCancelled(true);
            }
        }
    }

}
