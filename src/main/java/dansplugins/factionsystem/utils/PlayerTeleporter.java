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
        Logger.getInstance().log("Attempting to teleport " + player.getName() + " to " + location.toString());
        boolean success = player.teleport(location);
        if (success) {
            Logger.getInstance().log("Successfully teleported " + player.getName());
        }
        else {
            Logger.getInstance().log("Failed to teleport " + player.getName());
        }
        return success;
    }
}