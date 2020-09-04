package factionsystem.EventHandlers;

import java.time.ZonedDateTime;

import org.bukkit.ChatColor;
import org.bukkit.event.player.PlayerQuitEvent;

import factionsystem.Main;
import factionsystem.Objects.PlayerActivityRecord;
import factionsystem.Objects.PlayerPowerRecord;
import factionsystem.Subsystems.UtilitySubsystem;

public class PlayerLeaveEventHandler {

	Main main = null;
	
	public PlayerLeaveEventHandler(Main plugin)
	{
		main = plugin;
	}
	
	public void handle(PlayerQuitEvent event)
	{
    	PlayerActivityRecord record = UtilitySubsystem.getPlayerActivityRecord(event.getPlayer().getUniqueId(), main.playerActivityRecords);
    	if (record != null)
    	{
    		record.setLastLogout(ZonedDateTime.now());
    	}
	}
	
}
