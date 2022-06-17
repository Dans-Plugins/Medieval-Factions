/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.utils.extended;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.services.LocalConfigService;
import dansplugins.factionsystem.utils.Locale;
import dansplugins.factionsystem.utils.Logger;
import dansplugins.factionsystem.utils.PlayerTeleporter;

/**
 * @author Daniel McCoy Stephenson
 */
public class Scheduler {
    private static Scheduler instance;

    private Scheduler() {

    }

    public static Scheduler getInstance() {
        if (instance == null) {
            instance = new Scheduler();
        }
        return instance;
    }

    public void scheduleAutosave() {
        Logger.getInstance().debug(Locale.get("SchedulingHourlyAutoSave"));
        int delay = 60 * 60; // 1 hour
        int secondsUntilRepeat = 60 * 60; // 1 hour
        Bukkit.getScheduler().scheduleSyncRepeatingTask(MedievalFactions.getInstance(), new Runnable() {
            @Override
            public void run() {
                Logger.getInstance().debug(Locale.get("HourlySaveAlert"));
                PersistentData.getInstance().getLocalStorageService().save();
            }
        }, delay * 20, secondsUntilRepeat * 20);
    }

    public void schedulePowerIncrease() {
        Logger.getInstance().debug(Locale.get("SchedulingPowerIncrease"));
        int delay = MedievalFactions.getInstance().getConfig().getInt("minutesBeforeInitialPowerIncrease") * 60; // 30 minutes
        int secondsUntilRepeat = MedievalFactions.getInstance().getConfig().getInt("minutesBetweenPowerIncreases") * 60; // 1 hour
        Bukkit.getScheduler().scheduleSyncRepeatingTask(MedievalFactions.getInstance(), new Runnable() {
            @Override
            public void run() {
                Logger.getInstance().debug(String.format((Locale.get("AlertIncreasingThePowerOfEveryPlayer")) + "%n", MedievalFactions.getInstance().getConfig().getInt("powerIncreaseAmount"), MedievalFactions.getInstance().getConfig().getInt("minutesBetweenPowerIncreases")));
                PersistentData.getInstance().initiatePowerIncreaseForAllPlayers();
            }
        }, delay * 20L, secondsUntilRepeat * 20L);
    }

    public void schedulePowerDecrease() {
        Logger.getInstance().debug(Locale.get("SchedulingPowerDecrease"));
        int delay = MedievalFactions.getInstance().getConfig().getInt("minutesBetweenPowerDecreases") * 60;
        int secondsUntilRepeat = MedievalFactions.getInstance().getConfig().getInt("minutesBetweenPowerDecreases") * 60;
        Bukkit.getScheduler().scheduleSyncRepeatingTask(MedievalFactions.getInstance(), new Runnable() {
            @Override
            public void run() {
                Logger.getInstance().debug(String.format((Locale.get("AlertDecreasingThePowerOfInactivePlayers")) + "%n", MedievalFactions.getInstance().getConfig().getInt("powerDecreaseAmount"), MedievalFactions.getInstance().getConfig().getInt("minutesBeforePowerDecrease"), MedievalFactions.getInstance().getConfig().getInt("minutesBetweenPowerDecreases")));

                PersistentData.getInstance().decreasePowerForInactivePlayers();

                if (MedievalFactions.getInstance().getConfig().getBoolean("zeroPowerFactionsGetDisbanded")) {
                    PersistentData.getInstance().disbandAllZeroPowerFactions();
                }

                for (Player player : MedievalFactions.getInstance().getServer().getOnlinePlayers()) {
                    informPlayerIfTheirLandIsInDanger(player);
                }
            }
        }, delay * 20L, secondsUntilRepeat * 20L);
    }

    private void informPlayerIfTheirLandIsInDanger(Player player) {
        Faction faction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());
        if (faction != null) {
            if (isFactionExceedingTheirDemesneLimit(faction)) {
                player.sendMessage(ChatColor.RED + Locale.get("AlertMoreClaimedChunksThanPower"));
            }
        }
    }

    private boolean isFactionExceedingTheirDemesneLimit(Faction faction) {
        return (PersistentData.getInstance().getChunkDataAccessor().getChunksClaimedByFaction(faction.getName()) > faction.getCumulativePowerLevel());
    }

    public void scheduleTeleport(Player player, Location destinationLocation) {
        final int teleport_delay = LocalConfigService.getInstance().getInt("teleportDelay");
        player.sendMessage(ChatColor.AQUA + "Teleporting in " + teleport_delay + " seconds...");
        DelayedTeleportTask delayedTeleportTask = new DelayedTeleportTask(player, destinationLocation);
        delayedTeleportTask.runTaskLater(MedievalFactions.getInstance(), (long) (teleport_delay * getRandomNumberBetween(15, 25)));
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
            this.seconds = seconds;
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
            PlayerTeleporter.getInstance().teleportPlayer(player, destinationLocation);
        }
    }
}