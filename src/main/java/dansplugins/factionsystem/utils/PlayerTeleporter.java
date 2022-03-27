package dansplugins.factionsystem.utils;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class PlayerTeleporter {
    private static PlayerTeleporter instance;

    private PlayerTeleporter() {

    }

    public static PlayerTeleporter getInstance() {
        if (instance == null) {
            instance = new PlayerTeleporter();
        }
        return instance;
    }

    public boolean teleportPlayer(Player player, Location location) {
        Logger.getInstance().debug("Attempting to teleport " + player.getName() + " to " + location.toString());
        boolean success = player.teleport(location);
        if (success) {
            Logger.getInstance().debug("Successfully teleported " + player.getName());
        }
        else {
            Logger.getInstance().debug("Failed to teleport " + player.getName());
        }
        return success;
    }
}