/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.utils.extended;

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
        Logger.getInstance().log(Locale.get("SchedulingHourlyAutoSave"));
        int delay = 60 * 60; // 1 hour
        int secondsUntilRepeat = 60 * 60; // 1 hour
        Bukkit.getScheduler().scheduleSyncRepeatingTask(MedievalFactions.getInstance(), new Runnable() {
            @Override
            public void run() {
                Logger.getInstance().log(Locale.get("HourlySaveAlert"));
                PersistentData.getInstance().getLocalStorageService().save();
            }
        }, delay * 20, secondsUntilRepeat * 20);
    }

    public void schedulePowerIncrease() {
        Logger.getInstance().log(Locale.get("SchedulingPowerIncrease"));
        int delay = MedievalFactions.getInstance().getConfig().getInt("minutesBeforeInitialPowerIncrease") * 60; // 30 minutes
        int secondsUntilRepeat = MedievalFactions.getInstance().getConfig().getInt("minutesBetweenPowerIncreases") * 60; // 1 hour
        Bukkit.getScheduler().scheduleSyncRepeatingTask(MedievalFactions.getInstance(), new Runnable() {
            @Override
            public void run() {
                Logger.getInstance().log(String.format((Locale.get("AlertIncreasingThePowerOfEveryPlayer")) + "%n", MedievalFactions.getInstance().getConfig().getInt("powerIncreaseAmount"), MedievalFactions.getInstance().getConfig().getInt("minutesBetweenPowerIncreases")));
                PersistentData.getInstance().initiatePowerIncreaseForAllPlayers();
            }
        }, delay * 20L, secondsUntilRepeat * 20L);
    }

    public void schedulePowerDecrease() {
        Logger.getInstance().log(Locale.get("SchedulingPowerDecrease"));
        int delay = MedievalFactions.getInstance().getConfig().getInt("minutesBetweenPowerDecreases") * 60;
        int secondsUntilRepeat = MedievalFactions.getInstance().getConfig().getInt("minutesBetweenPowerDecreases") * 60;
        Bukkit.getScheduler().scheduleSyncRepeatingTask(MedievalFactions.getInstance(), new Runnable() {
            @Override
            public void run() {
                Logger.getInstance().log(String.format((Locale.get("AlertDecreasingThePowerOfInactivePlayers")) + "%n", MedievalFactions.getInstance().getConfig().getInt("powerDecreaseAmount"), MedievalFactions.getInstance().getConfig().getInt("minutesBeforePowerDecrease"), MedievalFactions.getInstance().getConfig().getInt("minutesBetweenPowerDecreases")));

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
        DelayedTeleportTask delayedTeleportTask = new DelayedTeleportTask(teleport_delay, player, destinationLocation);
        delayedTeleportTask.runTaskLater(MedievalFactions.getInstance(), teleport_delay * 20);
    }

    private class DelayedTeleportTask extends BukkitRunnable {
        private int seconds;
        private Player player;
        private Location initialLocation;
        private Location destinationLocation;

        public DelayedTeleportTask(int seconds, Player player, Location destinationLocation) {
            this.seconds = seconds;
            this.player = player;
            this.initialLocation = player.getLocation();
            this.destinationLocation = destinationLocation;
        }

        @Override
        public void run() {
            try {
                delay();
            } catch(Exception e) {
                player.sendMessage(ChatColor.RED + "Something went wrong.");
                Logger.getInstance().log("Something went wrong running a delayed teleport task.");
                return;
            }

            if (playerHasNotMoved()) {
                teleportPlayer();
            }
            else {
                player.sendMessage(ChatColor.RED + "Teleport cancelled.");
            }
        }

        private void delay() throws InterruptedException {
            TimeUnit.SECONDS.sleep(seconds);
        }

        private boolean playerHasNotMoved() {
            return initialLocation.getX() == player.getLocation().getX() && initialLocation.getY() == player.getLocation().getY() && initialLocation.getZ() == player.getLocation().getZ();
        }

        private void teleportPlayer() {
            PlayerTeleporter.getInstance().teleportPlayer(player, destinationLocation);
        }
    }
}