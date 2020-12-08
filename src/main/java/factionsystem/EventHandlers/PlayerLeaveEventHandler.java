package factionsystem.EventHandlers;

import java.time.ZonedDateTime;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import factionsystem.MedievalFactions;
import factionsystem.Objects.PlayerActivityRecord;
import factionsystem.Subsystems.UtilitySubsystem;

public class PlayerLeaveEventHandler implements Listener {

	@EventHandler()
	public void handle(PlayerQuitEvent event)
	{
		if (MedievalFactions.getInstance().lockingPlayers.contains(event.getPlayer().getUniqueId()))
		{
			MedievalFactions.getInstance().lockingPlayers.remove(event.getPlayer().getUniqueId());
		}
		if (MedievalFactions.getInstance().unlockingPlayers.contains(event.getPlayer().getUniqueId()))
		{
			MedievalFactions.getInstance().unlockingPlayers.remove(event.getPlayer().getUniqueId());
		}
		if (MedievalFactions.getInstance().playersGrantingAccess.containsKey(event.getPlayer().getUniqueId()))
		{
			MedievalFactions.getInstance().playersGrantingAccess.remove(event.getPlayer().getUniqueId());
		}
		if (MedievalFactions.getInstance().playersCheckingAccess.contains(event.getPlayer().getUniqueId()))
		{
			MedievalFactions.getInstance().playersCheckingAccess.remove(event.getPlayer().getUniqueId());
		}
		if (MedievalFactions.getInstance().playersRevokingAccess.containsKey(event.getPlayer().getUniqueId()))
		{
			MedievalFactions.getInstance().playersRevokingAccess.remove(event.getPlayer().getUniqueId());
		}
			
    	PlayerActivityRecord record = UtilitySubsystem.getPlayerActivityRecord(event.getPlayer().getUniqueId(), MedievalFactions.getInstance().playerActivityRecords);
    	if (record != null)
    	{
    		record.setLastLogout(ZonedDateTime.now());
    	}
	}
	
}
