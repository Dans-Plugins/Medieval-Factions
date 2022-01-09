package dansplugins.factionsystem.utils;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.objects.domain.ActivityRecord;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.objects.domain.PowerRecord;
import dansplugins.factionsystem.services.LocalChunkService;
import dansplugins.factionsystem.services.LocalLocaleService;
import dansplugins.factionsystem.services.LocalStorageService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;

import static org.bukkit.Bukkit.getServer;

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
                System.out.println(LocalLocaleService.getInstance().getText("HourlySaveAlert"));
                LocalStorageService.getInstance().save();
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
                System.out.println(String.format(LocalLocaleService.getInstance().getText("AlertIncreasingThePowerOfEveryPlayer"), MedievalFactions.getInstance().getConfig().getInt("powerIncreaseAmount"), MedievalFactions.getInstance().getConfig().getInt("minutesBetweenPowerIncreases")));
                for (PowerRecord powerRecord : PersistentData.getInstance().getPlayerPowerRecords()) {
                    try {
                        if (powerRecord.getPowerLevel() < powerRecord.maxPower()) {
                            if (getServer().getPlayer(powerRecord.getPlayerUUID()).isOnline()) {
                                powerRecord.increasePower();
                                getServer().getPlayer(powerRecord.getPlayerUUID()).sendMessage(ChatColor.GREEN + String.format(LocalLocaleService.getInstance().getText("AlertPowerLevelIncreasedBy"), MedievalFactions.getInstance().getConfig().getInt("powerIncreaseAmount")));
                            }
                        }
                    } catch (Exception ignored) {
                        // player offline
                    }
                }
            }
        }, delay * 20, secondsUntilRepeat * 20);
    }

    public void schedulePowerDecrease() {
        Logger.getInstance().log(LocalLocaleService.getInstance().getText("SchedulingPowerDecrease"));
        int delay = MedievalFactions.getInstance().getConfig().getInt("minutesBetweenPowerDecreases") * 60;
        int secondsUntilRepeat = MedievalFactions.getInstance().getConfig().getInt("minutesBetweenPowerDecreases") * 60;
        Bukkit.getScheduler().scheduleSyncRepeatingTask(MedievalFactions.getInstance(), new Runnable () {
            @Override
            public void run() {
                System.out.println(String.format(LocalLocaleService.getInstance().getText("AlertDecreasingThePowerOfInactivePlayers"), MedievalFactions.getInstance().getConfig().getInt("powerDecreaseAmount"), MedievalFactions.getInstance().getConfig().getInt("minutesBeforePowerDecrease"), MedievalFactions.getInstance().getConfig().getInt("minutesBetweenPowerDecreases")));

                for (ActivityRecord record : PersistentData.getInstance().getPlayerActivityRecords())
                {
                    Player player = getServer().getPlayer(record.getPlayerUUID());
                    boolean isOnline = false;
                    if (player != null)
                    {
                        isOnline = player.isOnline();
                    }
                    if (!isOnline && MedievalFactions.getInstance().getConfig().getBoolean("powerDecreases")
                            && record.getMinutesSinceLastLogout() > MedievalFactions.getInstance().getConfig().getInt("minutesBeforePowerDecrease"))
                    {
                        record.incrementPowerLost();
                        PowerRecord power = PersistentData.getInstance().getPlayersPowerRecord(record.getPlayerUUID());
                        power.decreasePower();
                    }
                }

                if (MedievalFactions.getInstance().getConfig().getBoolean("zeroPowerFactionsGetDisbanded")) {
                    disbandAllZeroPowerFactions();
                }

                for (Player player : MedievalFactions.getInstance().getServer().getOnlinePlayers())
                {
                    informPlayerIfTheirLandIsInDanger(player);
                }
            }
        }, delay * 20, secondsUntilRepeat * 20);
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
        return (LocalChunkService.getInstance().getChunksClaimedByFaction(faction.getName(), PersistentData.getInstance().getClaimedChunks()) > faction.getCumulativePowerLevel());
    }

    private void disbandAllZeroPowerFactions() {
        ArrayList<String> factionsToDisband = new ArrayList<>();
        for (Faction faction : PersistentData.getInstance().getFactions()) {
            if (faction.getCumulativePowerLevel() == 0) {
                factionsToDisband.add(faction.getName());
            }
        }
        for (String factionName : factionsToDisband) {
            Messenger.getInstance().sendAllPlayersInFactionMessage(PersistentData.getInstance().getFaction(factionName), ChatColor.RED + LocalLocaleService.getInstance().getText("AlertDisbandmentDueToZeroPower"));
            removeFaction(factionName);
            System.out.println(String.format(LocalLocaleService.getInstance().getText("DisbandmentDueToZeroPower"), factionName));
        }
    }

    private void removeFaction(String name) {

        Faction factionToRemove = PersistentData.getInstance().getFaction(name);

        if (factionToRemove != null) {
            // remove claimed land objects associated with this faction
            LocalChunkService.getInstance().removeAllClaimedChunks(factionToRemove.getName(), PersistentData.getInstance().getClaimedChunks());
            DynmapIntegrator.getInstance().updateClaims();

            // remove locks associated with this faction
            PersistentData.getInstance().removeAllLocks(factionToRemove.getName());


            for (Faction faction : PersistentData.getInstance().getFactions()) {
                // remove records of alliances/wars associated with this faction
                if (faction.isAlly(factionToRemove.getName())) {
                    faction.removeAlly(factionToRemove.getName());
                }
                if (faction.isEnemy(factionToRemove.getName())) {
                    faction.removeEnemy(factionToRemove.getName());
                }

                // remove liege and vassal references associated with this faction
                if (faction.isLiege(factionToRemove.getName())) {
                    faction.setLiege("none");
                }

                if (faction.isVassal(factionToRemove.getName())) {
                    faction.removeVassal(factionToRemove.getName());
                }
            }

            int index = -1;
            for (int i = 0; i < PersistentData.getInstance().getFactions().size(); i++) {
                if (PersistentData.getInstance().getFactions().get(i).getName().equalsIgnoreCase(name)) {
                    index = i;
                }
            }
            if (index != -1) {
                PersistentData.getInstance().getFactions().remove(index);
            }
        }
    }

}
