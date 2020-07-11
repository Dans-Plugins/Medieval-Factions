package factionsystem.EventHandlers;

import factionsystem.Main;
import factionsystem.Objects.ClaimedChunk;
import org.bukkit.entity.EntityType;
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

                if (event.getEntity().getType() != EntityType.HORSE &&
                        event.getEntity().getType() != EntityType.DONKEY &&
                        event.getEntity().getType() != EntityType.SHEEP &&
                        event.getEntity().getType() != EntityType.COW &&
                        event.getEntity().getType() != EntityType.MUSHROOM_COW &&
                        event.getEntity().getType() != EntityType.PIG &&
                        event.getEntity().getType() != EntityType.CHICKEN &&
                        event.getEntity().getType() != EntityType.CAT &&
                        event.getEntity().getType() != EntityType.OCELOT &&
                        event.getEntity().getType() != EntityType.RABBIT &&
                        event.getEntity().getType() != EntityType.LLAMA &&
                        event.getEntity().getType() != EntityType.TURTLE &&
                        event.getEntity().getType() != EntityType.PANDA &&
                        event.getEntity().getType() != EntityType.FOX &&
                        event.getEntity().getType() != EntityType.BEE) {

                    event.setCancelled(true);

                }

            }
        }
    }

}
