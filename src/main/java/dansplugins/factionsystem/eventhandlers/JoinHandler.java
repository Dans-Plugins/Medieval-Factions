/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.eventhandlers;

import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.events.FactionJoinEvent;
import dansplugins.factionsystem.objects.domain.ActivityRecord;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.objects.domain.PowerRecord;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.utils.Logger;
import dansplugins.factionsystem.utils.TerritoryOwnerNotifier;
import dansplugins.factionsystem.utils.extended.Messenger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;


/**
 * @author Daniel McCoy Stephenson
 */
public class JoinHandler implements Listener {
    private final PersistentData persistentData;
    private final LocaleService localeService;
    private final ConfigService configService;
    private final Logger logger;
    private final Messenger messenger;
    private final TerritoryOwnerNotifier territoryOwnerNotifier;

    public JoinHandler(PersistentData persistentData, LocaleService localeService, ConfigService configService, Logger logger, Messenger messenger, TerritoryOwnerNotifier territoryOwnerNotifier) {
        this.persistentData = persistentData;
        this.localeService = localeService;
        this.configService = configService;
        this.logger = logger;
        this.messenger = messenger;
        this.territoryOwnerNotifier = territoryOwnerNotifier;
    }

    @EventHandler()
    public void handle(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (dataExistsForPlayer(player)) {
            ActivityRecord activityRecord = persistentData.getPlayerActivityRecord(player.getUniqueId());
            activityRecord.incrementLogins();
            handlePowerDecay(activityRecord, player, event);
        } else {
            createRecordsForPlayer(player);
            handleRandomFactionAssignmentIfNecessary(player);
        }
        setPlayerActionBarTerritoryInfo(event.getPlayer());
        persistentData.getChunkDataAccessor().informPlayerIfTheirLandIsInDanger(player);
        informPlayerIfTheirFactionIsWeakened(player);
    }

    private void handlePowerDecay(ActivityRecord activityRecord, Player player, PlayerJoinEvent event) {
        double newPower = getNewPower(player);

        if (activityRecord.getLastLogout() != null && activityRecord.getMinutesSinceLastLogout() > 1) {
            player.sendMessage(ChatColor.GREEN + String.format(localeService.get("WelcomeBackLastLogout"), event.getPlayer().getName(), activityRecord.getTimeSinceLastLogout()));
        }

        if (activityRecord.getPowerLost() > 0) {
            player.sendMessage(ChatColor.RED + String.format(localeService.get("PowerHasDecayed"), activityRecord.getPowerLost(), newPower));
        }

        activityRecord.setPowerLost(0);
    }

    private double getNewPower(Player player) {
        PowerRecord powerRecord = persistentData.getPlayersPowerRecord(player.getUniqueId());

        double newPower = powerRecord.getPower();
        if (newPower < 0) {
            return 0;
        }
        return newPower;
    }

    private void handleRandomFactionAssignmentIfNecessary(Player player) {
        if (configService.getBoolean("randomFactionAssignment")) {
            assignPlayerToRandomFaction(player);
        }
    }

    private void createRecordsForPlayer(Player player) {
        createPowerRecordForPlayer(player);
        createActivityRecordForPlayer(player);
    }

    private boolean dataExistsForPlayer(Player player) {
        return persistentData.hasPowerRecord(player.getUniqueId()) && persistentData.hasActivityRecord(player.getUniqueId());
    }

    private void createActivityRecordForPlayer(Player player) {
        ActivityRecord newRecord = new ActivityRecord(player.getUniqueId(), configService, 1);
        persistentData.addActivityRecord(newRecord);
    }

    private void createPowerRecordForPlayer(Player player) {
        PowerRecord newRecord = new PowerRecord(player.getUniqueId(), configService, persistentData, configService.getInt("initialPowerLevel"));
        persistentData.addPowerRecord(newRecord);
    }

    private void assignPlayerToRandomFaction(Player player) {
        Faction faction = persistentData.getRandomFaction();
        if (faction != null) {
            FactionJoinEvent joinEvent = new FactionJoinEvent(faction, player);
            Bukkit.getPluginManager().callEvent(joinEvent);
            if (joinEvent.isCancelled()) {
                logger.debug("Join event was cancelled.");
                return;
            }
            messenger.sendAllPlayersInFactionMessage(faction, String.format(ChatColor.GREEN + "" + localeService.get("HasJoined"), player.getName(), faction.getName()));
            faction.addMember(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "" + localeService.get("AssignedToRandomFaction"));

            logger.debug(player.getName() + " has been randomly assigned to " + faction.getName() + "!");
        } else {
            logger.debug("Attempted to assign " + player.getName() + " to a random faction, but no factions are existent.");
        }
    }

    private void setPlayerActionBarTerritoryInfo(Player player) {
        if (configService.getBoolean("territoryIndicatorActionbar")) {
            if (chunkIsClaimed(player)) {
                String factionName = persistentData.getChunkDataAccessor().getClaimedChunk(player.getLocation().getChunk()).getHolder();
                Faction holder = persistentData.getFaction(factionName);
                territoryOwnerNotifier.sendPlayerTerritoryAlert(player, holder);
                return;
            }

            territoryOwnerNotifier.sendPlayerTerritoryAlert(player, null);
        }
    }

    private boolean chunkIsClaimed(Player player) {
        return persistentData.getChunkDataAccessor().isClaimed(player.getLocation().getChunk());
    }

    private void informPlayerIfTheirFactionIsWeakened(Player player) {
        Faction playersFaction = persistentData.getPlayersFaction(player.getUniqueId());
        if (playersFaction == null) {
            return;
        }

        if (playersFaction.isLiege() && playersFaction.isWeakened()) {
            player.sendMessage(ChatColor.RED + localeService.get("AlertFactionIsWeakened"));
        }
    }
}