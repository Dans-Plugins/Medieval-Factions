package factionsystem.EventHandlers;

import factionsystem.Main;
import factionsystem.Objects.PlayerPowerRecord;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;

import static factionsystem.Subsystems.UtilitySubsystem.*;
import static factionsystem.Subsystems.UtilitySubsystem.getPlayersFaction;

public class PlayerDeathEventHandler {

    Main main = null;

    public PlayerDeathEventHandler(Main plugin) {
        main = plugin;
    }

    public void handle(PlayerDeathEvent event) {
        event.getEntity();
        Player player = (Player) event.getEntity();

        // decrease dying player's power
        for (PlayerPowerRecord record : main.playerPowerRecords) {
            if (record.getPlayerName().equalsIgnoreCase(player.getName())) {
                record.decreasePower();
                if (getPlayersPowerRecord(player.getName(), main.playerPowerRecords).getPowerLevel() > 0) {
                    player.sendMessage(ChatColor.RED + "Your power level has decreased!");
                }
            }
        }

        // if player's cause of death was another player killing them
        if (player.getKiller() instanceof Player) {
            Player killer = (Player) player.getKiller();
            System.out.println(player.getName() + " has killed " + killer.getName());

            for (PlayerPowerRecord record : main.playerPowerRecords) {
                if (record.getPlayerName().equalsIgnoreCase(killer.getName())) {
                    record.increasePower();
                    if (getPlayersPowerRecord(killer.getName(), main.playerPowerRecords).getPowerLevel() < 20) {
                        killer.sendMessage(ChatColor.GREEN + "Your power level has increased!");
                    }
                }
            }

            // add power to killer's faction
            if (isInFaction(killer.getName(), main.factions)) {
                if (getPlayersPowerRecord(killer.getName(), main.playerPowerRecords).getPowerLevel() < 20) {
                    getPlayersFaction(killer.getName(), main.factions).addPower();
                }
            }
        }

        // decrease power from player's faction
        if (isInFaction(player.getName(), main.factions)) {
            if (getPlayersPowerRecord(player.getName(), main.playerPowerRecords).getPowerLevel() > 0) {
                getPlayersFaction(player.getName(), main.factions).subtractPower();
            }
        }
    }
}
