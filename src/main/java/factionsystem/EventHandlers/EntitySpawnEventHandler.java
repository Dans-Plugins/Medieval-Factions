package factionsystem.EventHandlers;

import factionsystem.Main;
import factionsystem.Objects.ClaimedChunk;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.event.entity.EntitySpawnEvent;

import static factionsystem.Subsystems.UtilitySubsystem.getPlayersFaction;

public class EntitySpawnEventHandler {

    Main main = null;

    public EntitySpawnEventHandler(Main plugin) {
        main = plugin;
    }

    public void handle(EntitySpawnEvent event) {

        int x = 0;
        int z = 0;

        x = event.getEntity().getLocation().getChunk().getX();
        z = event.getEntity().getLocation().getChunk().getZ();

        // check if land is claimed
        for (ClaimedChunk chunk : main.claimedChunks) {
            if (x == chunk.getCoordinates()[0] && z == chunk.getCoordinates()[1]) {

                if (event.getEntity() instanceof Monster) {
                    event.setCancelled(true);
                    System.out.println("Entity spawn cancelled!"); // delete this line after test
                }

            }
        }
    }

}
