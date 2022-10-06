/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.utils.extended;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.utils.Logger;
import dansplugins.factionsystem.utils.PlayerTeleporter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Objects;
import java.util.Random;

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
    private final PlayerService playerService;
    private final MessageService messageService;

    public Scheduler(Logger logger, LocaleService localeService, MedievalFactions medievalFactions, PersistentData persistentData, ConfigService configService, PlayerTeleporter playerTeleporter, PlayerService playerService, MessageService messageService) {
        this.logger = logger;
        this.localeService = localeService;
        this.medievalFactions = medievalFactions;
        this.persistentData = persistentData;
        this.configService = configService;
        this.playerTeleporter = playerTeleporter;
        this.playerService = playerService;
        this.messageService = messageService;
    }

    public void scheduleAutosave() {
        logger.debug(localeService.get("SchedulingHourlyAutoSave"));
        int delay = configService.getInt("secondsBeforeInitialAutosave");
        int secondsUntilRepeat = configService.getInt("secondsBetweenAutosaves");
        if (delay == 0 || secondsUntilRepeat == 0) {
            return;
        }
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
        Bukkit.getScheduler().scheduleSyncRepeatingTask(medievalFactions, () -> {
            logger.debug(String.format((localeService.get("AlertDecreasingThePowerOfInactivePlayers")) + "%n", configService.getInt("powerDecreaseAmount"), configService.getInt("minutesBeforePowerDecrease"), configService.getInt("minutesBetweenPowerDecreases")));

            persistentData.decreasePowerForInactivePlayers();

            if (configService.getBoolean("zeroPowerFactionsGetDisbanded")) {
                persistentData.disbandAllZeroPowerFactions();
            }

            for (Player player : medievalFactions.getServer().getOnlinePlayers()) {
                informPlayerIfTheirLandIsInDanger(player);
            }
        }, delay * 20L, secondsUntilRepeat * 20L);
    }

    private void informPlayerIfTheirLandIsInDanger(Player player) {
        Faction faction = persistentData.getPlayersFaction(player.getUniqueId());
        if (faction != null) {
            if (isFactionExceedingTheirDemesneLimit(faction)) {
                playerService.sendMessage(player, ChatColor.RED + localeService.get("AlertMoreClaimedChunksThanPower")
                        , "AlertMoreClaimedChunksThanPower", false);
            }
        }
    }

    private boolean isFactionExceedingTheirDemesneLimit(Faction faction) {
        return (persistentData.getChunkDataAccessor().getChunksClaimedByFaction(faction.getName()) > faction.getCumulativePowerLevel());
    }

    public void scheduleTeleport(Player player, Location destinationLocation) {
        final int teleport_delay = configService.getInt("teleportDelay");
        playerService.sendMessage(player, ChatColor.AQUA + "Teleporting in " + teleport_delay + " seconds..."
                , Objects.requireNonNull(messageService.getLanguage().getString("Teleport")).replace("#time#", String.valueOf(teleport_delay)), true);
        DelayedTeleportTask delayedTeleportTask = new DelayedTeleportTask(player, destinationLocation);
        delayedTeleportTask.runTaskLater(medievalFactions, (long) teleport_delay * getRandomNumberBetween(15, 25));
    }

    private int getRandomNumberBetween(int num1, int num2) {
        Random random = new Random();
        int span = num2 - num1;
        return random.nextInt(span) + num1;
    }

    private class DelayedTeleportTask extends BukkitRunnable {
        private final Player player;
        private final Location initialLocation;
        private final Location destinationLocation;

        public DelayedTeleportTask(Player player, Location destinationLocation) {
            this.player = player;
            this.initialLocation = player.getLocation();
            this.destinationLocation = destinationLocation;
        }

        @Override
        public void run() {
            if (playerHasNotMoved()) {
                teleportPlayer();
            } else {
                playerService.sendMessage(player, ChatColor.RED + "Teleport cancelled.",
                        "TeleportCancelled", false);
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