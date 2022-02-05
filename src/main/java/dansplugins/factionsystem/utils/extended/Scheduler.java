/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.utils.extended;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.services.LocalLocaleService;
import dansplugins.factionsystem.utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

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
        Logger.getInstance().log(LocalLocaleService.getInstance().getText("SchedulingHourlyAutoSave"));
        int delay = 60 * 60; // 1 hour
        int secondsUntilRepeat = 60 * 60; // 1 hour
        Bukkit.getScheduler().scheduleSyncRepeatingTask(MedievalFactions.getInstance(), new Runnable() {
            @Override
            public void run() {
                Logger.getInstance().log(LocalLocaleService.getInstance().getText("HourlySaveAlert"));
                PersistentData.getInstance().getLocalStorageService().save();
            }
        }, delay * 20, secondsUntilRepeat * 20);
    }

    public void schedulePowerIncrease() {
        Logger.getInstance().log(LocalLocaleService.getInstance().getText("SchedulingPowerIncrease"));
        int delay = MedievalFactions.getInstance().getConfig().getInt("minutesBeforeInitialPowerIncrease") * 60; // 30 minutes
        int secondsUntilRepeat = MedievalFactions.getInstance().getConfig().getInt("minutesBetweenPowerIncreases") * 60; // 1 hour
        Bukkit.getScheduler().scheduleSyncRepeatingTask(MedievalFactions.getInstance(), new Runnable() {
            @Override
            public void run() {
                Logger.getInstance().log(String.format((LocalLocaleService.getInstance().getText("AlertIncreasingThePowerOfEveryPlayer")) + "%n", MedievalFactions.getInstance().getConfig().getInt("powerIncreaseAmount"), MedievalFactions.getInstance().getConfig().getInt("minutesBetweenPowerIncreases")));
                PersistentData.getInstance().initiatePowerIncreaseForAllPlayers();
            }
        }, delay * 20L, secondsUntilRepeat * 20L);
    }

    public void schedulePowerDecrease() {
        Logger.getInstance().log(LocalLocaleService.getInstance().getText("SchedulingPowerDecrease"));
        int delay = MedievalFactions.getInstance().getConfig().getInt("minutesBetweenPowerDecreases") * 60;
        int secondsUntilRepeat = MedievalFactions.getInstance().getConfig().getInt("minutesBetweenPowerDecreases") * 60;
        Bukkit.getScheduler().scheduleSyncRepeatingTask(MedievalFactions.getInstance(), new Runnable() {
            @Override
            public void run() {
                Logger.getInstance().log(String.format((LocalLocaleService.getInstance().getText("AlertDecreasingThePowerOfInactivePlayers")) + "%n", MedievalFactions.getInstance().getConfig().getInt("powerDecreaseAmount"), MedievalFactions.getInstance().getConfig().getInt("minutesBeforePowerDecrease"), MedievalFactions.getInstance().getConfig().getInt("minutesBetweenPowerDecreases")));

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
                player.sendMessage(ChatColor.RED + LocalLocaleService.getInstance().getText("AlertMoreClaimedChunksThanPower"));
            }
        }
    }

    private boolean isFactionExceedingTheirDemesneLimit(Faction faction) {
        return (PersistentData.getInstance().getChunkDataAccessor().getChunksClaimedByFaction(faction.getName()) > faction.getCumulativePowerLevel());
    }


}