package factionsystem.EventHandlers;

import factionsystem.EphemeralData;
import factionsystem.MedievalFactions;
import factionsystem.Objects.PlayerActivityRecord;
import factionsystem.Objects.PlayerPowerRecord;
import factionsystem.PersistentData;
import factionsystem.Subsystems.UtilitySubsystem;

import org.bukkit.ChatColor;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.ZonedDateTime;
import java.util.UUID;

public class JoiningLeavingAndSpawningHandler implements Listener {

	@EventHandler()
    public void handle(PlayerJoinEvent event) {
        if (!hasPowerRecord(event.getPlayer().getUniqueId())) {
            PlayerPowerRecord newRecord = new PlayerPowerRecord(event.getPlayer().getUniqueId(),
                    MedievalFactions.getInstance().getConfig().getInt("initialPowerLevel"));
            PersistentData.getInstance().getPlayerPowerRecords().add(newRecord);
        }
        if (!hasActivityRecord(event.getPlayer().getUniqueId())) {
        	PlayerActivityRecord newRecord = new PlayerActivityRecord(event.getPlayer().getUniqueId(), 1);
        	PersistentData.getInstance().getPlayerActivityRecords().add(newRecord);
        }
        else
        {
        	PlayerActivityRecord record = UtilitySubsystem.getPlayerActivityRecord(event.getPlayer().getUniqueId(), PersistentData.getInstance().getPlayerActivityRecords());
        	if (record != null)
        	{
        		PlayerPowerRecord power = UtilitySubsystem.getPlayersPowerRecord(event.getPlayer().getUniqueId(), PersistentData.getInstance().getPlayerPowerRecords());
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

        UtilitySubsystem.informPlayerIfTheirLandIsInDanger(event.getPlayer(), PersistentData.getInstance().getFactions(), PersistentData.getInstance().getClaimedChunks());
    }

	private boolean hasPowerRecord(UUID playerUUID) {
		for (PlayerPowerRecord record : PersistentData.getInstance().getPlayerPowerRecords()){
			if (record.getPlayerUUID().equals(playerUUID)){
				return true;
			}
		}
		return false;
	}

	private boolean hasActivityRecord(UUID playerUUID) {
		for (PlayerActivityRecord record : PersistentData.getInstance().getPlayerActivityRecords()){
			if (record.getPlayerUUID().equals(playerUUID)){
				return true;
			}
		}
		return false;
	}

	@EventHandler()
	public void handle(PlayerQuitEvent event)
	{
		if (EphemeralData.getInstance().getLockingPlayers().contains(event.getPlayer().getUniqueId()))
		{
			EphemeralData.getInstance().getLockingPlayers().remove(event.getPlayer().getUniqueId());
		}
		if (EphemeralData.getInstance().getUnlockingPlayers().contains(event.getPlayer().getUniqueId()))
		{
			EphemeralData.getInstance().getUnlockingPlayers().remove(event.getPlayer().getUniqueId());
		}
		if (EphemeralData.getInstance().getPlayersGrantingAccess().containsKey(event.getPlayer().getUniqueId()))
		{
			EphemeralData.getInstance().getPlayersGrantingAccess().remove(event.getPlayer().getUniqueId());
		}
		if (EphemeralData.getInstance().getPlayersCheckingAccess().contains(event.getPlayer().getUniqueId()))
		{
			EphemeralData.getInstance().getPlayersCheckingAccess().remove(event.getPlayer().getUniqueId());
		}
		if (EphemeralData.getInstance().getPlayersRevokingAccess().containsKey(event.getPlayer().getUniqueId()))
		{
			EphemeralData.getInstance().getPlayersRevokingAccess().remove(event.getPlayer().getUniqueId());
		}

		PlayerActivityRecord record = UtilitySubsystem.getPlayerActivityRecord(event.getPlayer().getUniqueId(), PersistentData.getInstance().getPlayerActivityRecords());
		if (record != null)
		{
			record.setLastLogout(ZonedDateTime.now());
		}
	}

	@EventHandler()
	public void handle(EntitySpawnEvent event) {

		int x = 0;
		int z = 0;

		x = event.getEntity().getLocation().getChunk().getX();
		z = event.getEntity().getLocation().getChunk().getZ();

		// check if land is claimed
		if (UtilitySubsystem.isClaimed(event.getLocation().getChunk(), PersistentData.getInstance().getClaimedChunks()))
		{
			if (event.getEntity() instanceof Monster && !MedievalFactions.getInstance().getConfig().getBoolean("mobsSpawnInFactionTerritory")) {
				event.setCancelled(true);
			}
		}
	}

}
