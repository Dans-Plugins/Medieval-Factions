package factionsystem.EventHandlers;

import factionsystem.Main;
import factionsystem.Objects.Faction;
import factionsystem.Objects.PlayerPowerRecord;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;

import static factionsystem.Subsystems.UtilitySubsystem.getChunksClaimedByFaction;
import static factionsystem.Subsystems.UtilitySubsystem.getPlayersFaction;

public class PlayerJoinEventHandler {

    Main main = null;

    public PlayerJoinEventHandler(Main plugin) {
        main = plugin;
    }

    public void handle(PlayerJoinEvent event) {
        if (!main.utilities.hasPowerRecord(event.getPlayer().getUniqueId())) {
            PlayerPowerRecord newRecord = new PlayerPowerRecord(event.getPlayer().getUniqueId(),
                    main.getConfig().getInt("initialPowerLevel"),
                    main);
            main.playerPowerRecords.add(newRecord);
        }

        informPlayerIfTheirLandIsInDanger(event.getPlayer());
    }

    public void informPlayerIfTheirLandIsInDanger(Player player) {
        Faction faction = getPlayersFaction(player.getUniqueId(), main.factions);
        if (faction != null) {
            if (isFactionExceedingTheirDemesneLimit(faction)) {
                player.sendMessage(ChatColor.RED + "Your faction has more claimed chunks than power! Your land can be conquered!");
            }
        }
    }

    public boolean isFactionExceedingTheirDemesneLimit(Faction faction) {
        return (getChunksClaimedByFaction(faction.getName(), main.claimedChunks) > faction.getCumulativePowerLevel());
    }
}
