package factionsystem.EventHandlers;

import factionsystem.Main;
import factionsystem.Objects.Faction;
import factionsystem.Objects.PlayerActivityRecord;
import factionsystem.Objects.PlayerPowerRecord;
import factionsystem.Subsystems.UtilitySubsystem;

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
        if (!main.utilities.hasActivityRecord(event.getPlayer().getUniqueId())) {
        	PlayerActivityRecord newRecord = new PlayerActivityRecord(event.getPlayer().getUniqueId(), 1, main);
        	main.playerActivityRecords.add(newRecord);
        }
        else
        {
        	PlayerActivityRecord record = UtilitySubsystem.getPlayerActivityRecord(event.getPlayer().getUniqueId(), main.playerActivityRecords);
        	if (record != null)
        	{
        		PlayerPowerRecord power = UtilitySubsystem.getPlayersPowerRecord(event.getPlayer().getUniqueId(), main.playerPowerRecords);
        		record.incrementLogins();

        		int newPower = power.getPowerLevel();
        		if (newPower < 0)
        			newPower = 0;
        		
        		if (record.getLastLogout() != null)
        		{
        			if (record.getMinutesSinceLastLogout() > 1)
        			{
        				event.getPlayer().sendMessage(ChatColor.GREEN + "Welcome back " + event.getPlayer().getName() + "! You last logged out " + record.getTimeSinceLastLogout() + " ago.");
        			}
        		}
        		if (record.getPowerLost() > 0)
        		{
        			event.getPlayer().sendMessage(ChatColor.RED + "Your power has decayed by " + record.getPowerLost() + " since you last logged out. Your power is now " + newPower + ".");
        		}
        		record.setPowerLost(0);
        	}
        }

        UtilitySubsystem.informPlayerIfTheirLandIsInDanger(event.getPlayer(), main.factions, main.claimedChunks);
    }

}
