package factionsystem.EventHandlers;

import factionsystem.MedievalFactions;
import factionsystem.Objects.PlayerActivityRecord;
import factionsystem.Objects.PlayerPowerRecord;
import factionsystem.Subsystems.UtilitySubsystem;

import org.bukkit.ChatColor;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

public class PlayerJoinEventHandler {

    public void handle(PlayerJoinEvent event) {
        if (!hasPowerRecord(event.getPlayer().getUniqueId())) {
            PlayerPowerRecord newRecord = new PlayerPowerRecord(event.getPlayer().getUniqueId(),
                    MedievalFactions.getInstance().getConfig().getInt("initialPowerLevel"));
            MedievalFactions.getInstance().playerPowerRecords.add(newRecord);
        }
        if (!hasActivityRecord(event.getPlayer().getUniqueId())) {
        	PlayerActivityRecord newRecord = new PlayerActivityRecord(event.getPlayer().getUniqueId(), 1);
        	MedievalFactions.getInstance().playerActivityRecords.add(newRecord);
        }
        else
        {
        	PlayerActivityRecord record = UtilitySubsystem.getPlayerActivityRecord(event.getPlayer().getUniqueId(), MedievalFactions.getInstance().playerActivityRecords);
        	if (record != null)
        	{
        		PlayerPowerRecord power = UtilitySubsystem.getPlayersPowerRecord(event.getPlayer().getUniqueId(), MedievalFactions.getInstance().playerPowerRecords);
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

        UtilitySubsystem.informPlayerIfTheirLandIsInDanger(event.getPlayer(), MedievalFactions.getInstance().factions, MedievalFactions.getInstance().claimedChunks);
    }

	private boolean hasPowerRecord(UUID playerUUID) {
		for (PlayerPowerRecord record : MedievalFactions.getInstance().playerPowerRecords){
			if (record.getPlayerUUID().equals(playerUUID)){
				return true;
			}
		}
		return false;
	}

	private boolean hasActivityRecord(UUID playerUUID) {
		for (PlayerActivityRecord record : MedievalFactions.getInstance().playerActivityRecords){
			if (record.getPlayerUUID().equals(playerUUID)){
				return true;
			}
		}
		return false;
	}

}
