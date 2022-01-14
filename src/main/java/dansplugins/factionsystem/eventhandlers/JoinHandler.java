/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.eventhandlers;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.events.FactionJoinEvent;
import dansplugins.factionsystem.objects.domain.ActivityRecord;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.objects.domain.PowerRecord;
import dansplugins.factionsystem.services.LocalChunkService;
import dansplugins.factionsystem.services.LocalLocaleService;
import dansplugins.factionsystem.utils.Logger;
import dansplugins.factionsystem.utils.Messenger;
import dansplugins.factionsystem.utils.TerritoryOwnerNotifier;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

/**
 * @author Daniel McCoy Stephenson
 */
public class JoinHandler implements Listener {

	@EventHandler()
    public void handle(PlayerJoinEvent event) {
		Player player = event.getPlayer();
        if (dataExistsForPlayer(player)) {
			ActivityRecord activityRecord = PersistentData.getInstance().getPlayerActivityRecord(player.getUniqueId());
			activityRecord.incrementLogins();
			handlePowerDecay(activityRecord, player, event);
        }
        else {
			createRecordsForPlayer(player);
			handleRandomFactionAssignmentIfNecessary(player);
        }
        setPlayerActionBarTerritoryInfo(event.getPlayer());
        LocalChunkService.getInstance().informPlayerIfTheirLandIsInDanger(player, PersistentData.getInstance().getFactions(), PersistentData.getInstance().getClaimedChunks());
		informPlayerIfTheirFactionIsWeakened(player);
    }

	private void handlePowerDecay(ActivityRecord activityRecord, Player player, PlayerJoinEvent event) {
		int newPower = getNewPower(player);

		if (activityRecord.getLastLogout() != null && activityRecord.getMinutesSinceLastLogout() > 1) {
			player.sendMessage(ChatColor.GREEN + String.format(LocalLocaleService.getInstance().getText("WelcomeBackLastLogout"), event.getPlayer().getName(), activityRecord.getTimeSinceLastLogout()));}

		if (activityRecord.getPowerLost() > 0) {
			player.sendMessage(ChatColor.RED + String.format(LocalLocaleService.getInstance().getText("PowerHasDecayed"), activityRecord.getPowerLost(), newPower));
		}

		activityRecord.setPowerLost(0);
	}

	private int getNewPower(Player player) {
		PowerRecord powerRecord = PersistentData.getInstance().getPlayersPowerRecord(player.getUniqueId());

		int newPower = powerRecord.getPowerLevel();
		if (newPower < 0) {
			return 0;
		}
		return newPower;
	}

	private void handleRandomFactionAssignmentIfNecessary(Player player) {
		if (MedievalFactions.getInstance().getConfig().getBoolean("randomFactionAssignment")) {
			assignPlayerToRandomFaction(player);
		}
	}

	private void createRecordsForPlayer(Player player) {
		createPowerRecordForPlayer(player);
		createActivityRecordForPlayer(player);
	}

	private boolean dataExistsForPlayer(Player player) {
		return hasPowerRecord(player.getUniqueId()) && hasActivityRecord(player.getUniqueId());
	}

	private void createActivityRecordForPlayer(Player player) {
		ActivityRecord newRecord = new ActivityRecord(player.getUniqueId(), 1);
		PersistentData.getInstance().getPlayerActivityRecords().add(newRecord);
	}

	private void createPowerRecordForPlayer(Player player) {
		PowerRecord newRecord = new PowerRecord(player.getUniqueId(), MedievalFactions.getInstance().getConfig().getInt("initialPowerLevel"));
		PersistentData.getInstance().getPlayerPowerRecords().add(newRecord);
	}

	private void assignPlayerToRandomFaction(Player player) {
		Faction faction = PersistentData.getInstance().getRandomFaction();
		if (faction != null) {
			FactionJoinEvent joinEvent = new FactionJoinEvent(faction, player);
			Bukkit.getPluginManager().callEvent(joinEvent);
			if (joinEvent.isCancelled()) {
				// TODO Locale Message
				return;
			}
			Messenger.getInstance().sendAllPlayersInFactionMessage(faction, String.format(ChatColor.GREEN + "" + LocalLocaleService.getInstance().getText("HasJoined"), player.getName(), faction.getName()));
			faction.addMember(player.getUniqueId());
			player.sendMessage(ChatColor.GREEN + "" + LocalLocaleService.getInstance().getText("AssignedToRandomFaction"));

			Logger.getInstance().log(player.getName() + " has been randomly assigned to " + faction.getName() + "!");
		}
		else {
			Logger.getInstance().log("Attempted to assign " + player.getName() + " to a random faction, but no factions are existent.");
		}
	}

	private void setPlayerActionBarTerritoryInfo(Player player) {
		if (MedievalFactions.getInstance().getConfig().getBoolean("territoryIndicatorActionbar")) {
			if (chunkIsClaimed(player)) {
				String factionName = LocalChunkService.getInstance().getClaimedChunk(player.getLocation().getChunk()).getHolder();
				Faction holder = PersistentData.getInstance().getFaction(factionName);
				TerritoryOwnerNotifier.getInstance().sendPlayerTerritoryAlert(player, holder);
				return;
			}

			TerritoryOwnerNotifier.getInstance().sendPlayerTerritoryAlert(player, null);
		}
	}

	private boolean chunkIsClaimed(Player player) {
		return LocalChunkService.getInstance().isClaimed(player.getLocation().getChunk(), PersistentData.getInstance().getClaimedChunks());
	}

	private void informPlayerIfTheirFactionIsWeakened(Player player) {
		Faction playersFaction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());
		if (playersFaction == null) {
			return;
		}

		if (playersFaction.isLiege() && playersFaction.isWeakened()) {
			player.sendMessage(ChatColor.RED + LocalLocaleService.getInstance().getText("AlertFactionIsWeakened"));
		}
	}

	private boolean hasPowerRecord(UUID playerUUID) {
		for (PowerRecord record : PersistentData.getInstance().getPlayerPowerRecords()){
			if (record.getPlayerUUID().equals(playerUUID)){
				return true;
			}
		}
		return false;
	}

	private boolean hasActivityRecord(UUID playerUUID) {
		for (ActivityRecord record : PersistentData.getInstance().getPlayerActivityRecords()) {
			if (record.getPlayerUUID().equals(playerUUID)) {
				return true;
			}
		}
		return false;
	}
}