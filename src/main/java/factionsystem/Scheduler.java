package factionsystem;

import factionsystem.Data.PersistentData;
import factionsystem.Objects.Faction;
import factionsystem.Objects.PlayerActivityRecord;
import factionsystem.Objects.PlayerPowerRecord;
import factionsystem.Subsystems.StorageSubsystem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;

import static factionsystem.Subsystems.UtilitySubsystem.*;
import static org.bukkit.Bukkit.getServer;

public class Scheduler {

    public void scheduleAutosave() {
        System.out.println("Scheduling hourly auto save...");
        int delay = 60 * 60; // 1 hour
        int secondsUntilRepeat = 60 * 60; // 1 hour
        Bukkit.getScheduler().scheduleSyncRepeatingTask(MedievalFactions.getInstance(), new Runnable() {
            @Override
            public void run() {
                System.out.println("Medieval Factions is saving. This will happen every hour.");
                StorageSubsystem.getInstance().save();
            }
        }, delay * 20, secondsUntilRepeat * 20);
    }

    public void schedulePowerIncrease() {
        System.out.println("Scheduling power increase...");
        int delay = MedievalFactions.getInstance().getConfig().getInt("minutesBeforeInitialPowerIncrease") * 60; // 30 minutes
        int secondsUntilRepeat = MedievalFactions.getInstance().getConfig().getInt("minutesBetweenPowerIncreases") * 60; // 1 hour
        Bukkit.getScheduler().scheduleSyncRepeatingTask(MedievalFactions.getInstance(), new Runnable() {
            @Override
            public void run() {
                System.out.println("Medieval Factions is increasing the power of every player by " + MedievalFactions.getInstance().getConfig().getInt("powerIncreaseAmount") + ". This will happen every " + MedievalFactions.getInstance().getConfig().getInt("minutesBetweenPowerIncreases") + " minutes.");
                for (PlayerPowerRecord powerRecord : PersistentData.getInstance().getPlayerPowerRecords()) {
                    try {
                        if (powerRecord.getPowerLevel() < powerRecord.maxPower()) {
                            if (getServer().getPlayer(powerRecord.getPlayerUUID()).isOnline()) {
                                powerRecord.increasePower();
                                getServer().getPlayer(powerRecord.getPlayerUUID()).sendMessage(ChatColor.GREEN + "You feel stronger. Your power has increased by " + MedievalFactions.getInstance().getConfig().getInt("powerIncreaseAmount") + ".");
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
        System.out.println("Scheduling power decrease...");
        int delay = MedievalFactions.getInstance().getConfig().getInt("minutesBetweenPowerDecreases") * 60;
        int secondsUntilRepeat = MedievalFactions.getInstance().getConfig().getInt("minutesBetweenPowerDecreases") * 60;
        Bukkit.getScheduler().scheduleSyncRepeatingTask(MedievalFactions.getInstance(), new Runnable () {
            @Override
            public void run() {
                System.out.println("Medieval Factions is decreasing the power of every player by " + MedievalFactions.getInstance().getConfig().getInt("powerDecreaseAmount") + " if they haven't been online in over " + MedievalFactions.getInstance().getConfig().getInt("minutesBeforePowerDecrease") + " minutes. This will happen every " + MedievalFactions.getInstance().getConfig().getInt("minutesBetweenPowerDecreases") + " minutes.");

                for (PlayerActivityRecord record : PersistentData.getInstance().getPlayerActivityRecords())
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
                        PlayerPowerRecord power = getPlayersPowerRecord(record.getPlayerUUID(), PersistentData.getInstance().getPlayerPowerRecords());
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
        Faction faction = getPlayersFaction(player.getUniqueId(), PersistentData.getInstance().getFactions());
        if (faction != null) {
            if (isFactionExceedingTheirDemesneLimit(faction)) {
                player.sendMessage(ChatColor.RED + "Your faction has more claimed chunks than power! Your land can be conquered!");
            }
        }
    }

    private boolean isFactionExceedingTheirDemesneLimit(Faction faction) {
        return (getChunksClaimedByFaction(faction.getName(), PersistentData.getInstance().getClaimedChunks()) > faction.getCumulativePowerLevel());
    }

    private void disbandAllZeroPowerFactions() {
        ArrayList<String> factionsToDisband = new ArrayList<>();
        for (Faction faction : PersistentData.getInstance().getFactions()) {
            if (faction.getCumulativePowerLevel() == 0) {
                factionsToDisband.add(faction.getName());
            }
        }
        for (String factionName : factionsToDisband) {
            MedievalFactions.getInstance().utilities.sendAllPlayersInFactionMessage(getFaction(factionName, PersistentData.getInstance().getFactions()), ChatColor.RED + "Your faction has been disbanded due to its cumulative power reaching zero.");
            removeFaction(factionName);
            System.out.println(factionName + " has been disbanded due to its cumulative power reaching zero.");
        }
    }

    private void removeFaction(String name) {

        Faction factionToRemove = getFaction(name, PersistentData.getInstance().getFactions());

        if (factionToRemove != null) {
            // remove claimed land objects associated with this faction
            removeAllClaimedChunks(factionToRemove.getName(), PersistentData.getInstance().getClaimedChunks());

            // remove locks associated with this faction
            removeAllLocks(factionToRemove.getName(), PersistentData.getInstance().getLockedBlocks());

            // remove records of alliances/wars associated with this faction
            for (Faction faction : PersistentData.getInstance().getFactions()) {
                if (faction.isAlly(factionToRemove.getName())) {
                    faction.removeAlly(factionToRemove.getName());
                }
                if (faction.isEnemy(factionToRemove.getName())) {
                    faction.removeEnemy(factionToRemove.getName());
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
