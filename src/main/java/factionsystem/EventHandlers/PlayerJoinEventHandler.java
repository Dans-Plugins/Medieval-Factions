package factionsystem.EventHandlers;

import factionsystem.Main;
import factionsystem.Objects.PlayerPowerRecord;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinEventHandler {

    Main main = null;

    public PlayerJoinEventHandler(Main plugin) {
        main = plugin;
    }

    public void handle(PlayerJoinEvent event) {
        if (!main.utilities.hasPowerRecord(event.getPlayer().getUniqueId())) {
            PlayerPowerRecord newRecord = new PlayerPowerRecord(event.getPlayer().getUniqueId(),
                    main.getConfig().getInt("initialPowerLevel"),
                    main.getConfig().getInt("maxPowerLevel"),
                    main);
            main.playerPowerRecords.add(newRecord);
        }
    }
}
