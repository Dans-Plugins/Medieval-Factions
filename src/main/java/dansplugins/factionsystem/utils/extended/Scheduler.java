/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.utils.extended;

import java.util.Random;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.utils.Logger;
import dansplugins.factionsystem.utils.PlayerTeleporter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import dansplugins.factionsystem.objects.domain.Faction;

/**
 * @author Daniel McCoy Stephenson
 */
public class Scheduler {
    private final Logger logger;
    private final LocaleService localeService;
    private final MedievalFactions medievalFactions;
    private final PersistentData persistentData;
    private final ConfigService configService;
    private final PlayerTeleporter playerTeleporter;

    public Scheduler(Logger logger, LocaleService localeService, MedievalFactions medievalFactions, PersistentData persistentData, ConfigService configService, PlayerTeleporter playerTeleporter) {
        this.logger = logger;
        this.localeService = localeService;
        this.medievalFactions = medievalFactions;
        this.persistentData = persistentData;
        this.configService = configService;
        this.playerTeleporter = playerTeleporter;
    }

    public void scheduleAutosave() {
        logger.debug(localeService.get("SchedulingHourlyAutoSave"));
        int delay = 60 * 60; // 1 hour
        int secondsUntilRepeat = 60 * 60; // 1 hour
        Bukkit.getScheduler().scheduleSyncRepeatingTask(medievalFactions, new Runnable() {
            @Override
            public void run() {
                logger.debug(localeService.get("HourlySaveAlert"));
                persistentData.getLocalStorageService().save();
            }
        }, delay * 20, secondsUntilRepeat * 20);
    }

    public void schedulePowerIncrease() {
        logger.debug(localeService.get("SchedulingPowerIncrease"));
        int delay = configService.getInt("minutesBeforeInitialPowerIncrease") * 60; // 30 minutes
        int secondsUntilRepeat = configService.getInt("minutesBetweenPowerIncreases") * 60; // 1 hour
        Bukkit.getScheduler().scheduleSyncRepeatingTask(medievalFactions, new Runnable() {
            @Override
            public void run() {
                logger.debug(String.format((localeService.get("AlertIncreasingThePowerOfEveryPlayer")) + "%n", configService.getInt("powerIncreaseAmount"), configService.getInt("minutesBetweenPowerIncreases")));
                persistentData.initiatePowerIncreaseForAllPlayers();
            }
        }, delay * 20L, secondsUntilRepeat * 20L);
    }

    public void schedulePowerDecrease() {
        logger.debug(localeService.get("SchedulingPowerDecrease"));
        int delay = configService.getInt("minutesBetweenPowerDecreases") * 60;
        int secondsUntilRepeat = configService.getInt("minutesBetweenPowerDecreases") * 60;
        Bukkit.getScheduler().scheduleSyncRepeatingTask(medievalFactions, new Runnable() {
            @Override
            public void run() {
                logger.debug(String.format((localeService.get("AlertDecreasingThePowerOfInactivePlayers")) + "%n", configService.getInt("powerDecreaseAmount"), configService.getInt("minutesBeforePowerDecrease"), configService.getInt("minutesBetweenPowerDecreases")));

                persistentData.decreasePowerForInactivePlayers();

                if (configService.getBoolean("zeroPowerFactionsGetDisbanded")) {
                    persistentData.disbandAllZeroPowerFactions();
                }

                for (Player player : medievalFactions.getServer().getOnlinePlayers()) {
                    informPlayerIfTheirLandIsInDanger(player);
                }
            }
        }, delay * 20L, secondsUntilRepeat * 20L);
    }

    private void informPlayerIfTheirLandIsInDanger(Player player) {
        Faction faction = persistentData.getPlayersFaction(player.getUniqueId());
        if (faction != null) {
            if (isFactionExceedingTheirDemesneLimit(faction)) {
                player.sendMessage(ChatColor.RED + localeService.get("AlertMoreClaimedChunksThanPower"));
            }
        }
    }

    private boolean isFactionExceedingTheirDemesneLimit(Faction faction) {
        return (persistentData.getChunkDataAccessor().getChunksClaimedByFaction(faction.getName()) > faction.getCumulativePowerLevel());
    }

    public void scheduleTeleport(Player player, Location destinationLocation) {
        final int teleport_delay = configService.getInt("teleportDelay");
        player.sendMessage(ChatColor.AQUA + "Teleporting in " + teleport_delay + " seconds...");
        DelayedTeleportTask delayedTeleportTask = new DelayedTeleportTask(player, destinationLocation);
        delayedTeleportTask.runTaskLater(medievalFactions, (long) (teleport_delay * getRandomNumberBetween(15, 25)));
    }

    private int getRandomNumberBetween(int num1, int num2) {
        Random random = new Random();
        int span = num2 - num1;
        return random.nextInt(span) + num1;
    }

    private class DelayedTeleportTask extends BukkitRunnable {
        private Player player;
        private Location initialLocation;
        private Location destinationLocation;

        public DelayedTeleportTask(Player player, Location destinationLocation) {
            this.player = player;
            this.initialLocation = player.getLocation();
            this.destinationLocation = destinationLocation;
        }

        @Override
        public void run() {
            if (playerHasNotMoved()) {
                teleportPlayer();
            }
            else {
                player.sendMessage(ChatColor.RED + "Teleport cancelled.");
            }
        }

        private boolean playerHasNotMoved() {
            return initialLocation.getX() == player.getLocation().getX() && initialLocation.getY() == player.getLocation().getY() && initialLocation.getZ() == player.getLocation().getZ();
        }

        private void teleportPlayer() {
            playerTeleporter.teleportPlayer(player, destinationLocation);
        }
    }
}