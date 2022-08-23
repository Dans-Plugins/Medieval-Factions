package dansplugins.factionsystem.utils;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class PlayerTeleporter {
    private final Logger logger;

    public PlayerTeleporter(Logger logger) {
        this.logger = logger;
    }

    public void teleportPlayer(Player player, Location location) {
        logger.debug("Attempting to teleport " + player.getName() + " to " + location.toString());
        boolean success = player.teleport(location);
        if (success) {
            logger.debug("Successfully teleported " + player.getName());
        } else {
            logger.debug("Failed to teleport " + player.getName());
        }
    }
}