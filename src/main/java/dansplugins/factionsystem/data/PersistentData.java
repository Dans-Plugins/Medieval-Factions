package dansplugins.factionsystem.data;

import dansplugins.factionsystem.objects.*;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.UUID;

public class PersistentData {

    private static PersistentData instance;

    public ArrayList<IFaction> factions = new ArrayList<>();
    public ArrayList<ClaimedChunk> claimedChunks = new ArrayList<>();
    public ArrayList<PlayerPowerRecord> playerPowerRecords = new ArrayList<>();
    public ArrayList<PlayerActivityRecord> playerActivityRecords = new ArrayList<>();
    public ArrayList<LockedBlock> lockedBlocks = new ArrayList<>();

    private PersistentData() {}

    public static PersistentData getInstance() {
        if (instance == null) {
            instance = new PersistentData();
        }
        return instance;
    }

    // arraylist getters ---

    public ArrayList<IFaction> getFactions() {
        return factions;
    }

    public ArrayList<ClaimedChunk> getClaimedChunks() {
        return claimedChunks;
    }

    public ArrayList<PlayerPowerRecord> getPlayerPowerRecords() {
        return playerPowerRecords;
    }

    public ArrayList<PlayerActivityRecord> getPlayerActivityRecords() {
        return playerActivityRecords;
    }

    public ArrayList<LockedBlock> getLockedBlocks() {
        return lockedBlocks;
    }

    // specific getters ---

    /**
     * Method to get a Faction by its name.
     * <p>
     *     This method utilises {@link #getFaction(String, boolean, boolean)} to obtain the Faction with the given name.
     * </p>
     *
     * @param name of the Faction desired (Can be {@code null}).
     * @return {@link IFaction} or {@code null}.
     * @see #getFaction(String, boolean, boolean)
     */
    public IFaction getFaction(String name) {
        return getFaction(name, false, false);
    }

    /**
     * Method to get a Faction by its prefix.
     * <p>
     *     This method utilises {@link #getFaction(String, boolean, boolean)} to obtain the Faction with the given prefix.
     * </p>
     *
     * @param prefix of the Faction desired (Can be {@code null}).
     * @return {@link IFaction} or {@code null}.
     * @see #getFaction(String, boolean, boolean)
     */
    public IFaction getFactionByPrefix(String prefix) {
        return getFaction(prefix, true, true);
    }

    /**
     * Method to obtain a Faction from the given string.
     * <p>
     *     This method can check Faction name and/or Faction prefix depending on the parameters specified.
     *     <br>If you wish to only check prefix, provide the string and make sure both booleans are {@code true}.
     *     <br>If you wish to only check name, provide the string and make sure both booleans are {@code false}.
     *     <br>If you wish to check everything, provide the string and make sure the first boolean is {@code true} only.
     * </p>
     * @param text which you'd like to obtain the Faction from.
     * @param checkPrefix a toggle for checking prefix.
     * @param onlyCheckPrefix a toggle for only checking prefix.
     * @return {@link IFaction} or {@code null}.
     * @see #getFaction(String)
     * @see #getFactionByPrefix(String)
     */
    public IFaction getFaction(String text, boolean checkPrefix, boolean onlyCheckPrefix) {
        for (IFaction faction : getFactions()) {
            if ((!onlyCheckPrefix && faction.getName().equalsIgnoreCase(text)) ||
                    (faction.getPrefix().equalsIgnoreCase(text) && checkPrefix)) {
                return faction;
            }
        }
        return null;
    }

    public IFaction getPlayersFaction(UUID playerUUID) {
        // membership check
        for (IFaction faction : getFactions()) {
            if (faction.isMember(playerUUID)) {
                return faction;
            }
        }
        return null;
    }

    public PlayerPowerRecord getPlayersPowerRecord(UUID playerUUID) {
        for (PlayerPowerRecord record : getPlayerPowerRecords()) {
            if (record.getPlayerUUID().equals(playerUUID)) {
                return record;
            }
        }
        return null;
    }

    public PlayerActivityRecord getPlayerActivityRecord(UUID uuid)
    {
        for (PlayerActivityRecord record : getPlayerActivityRecords())
        {
            if (record.getPlayerUUID().equals(uuid))
            {
                return record;
            }
        }
        return null;
    }

    public LockedBlock getLockedBlock(Block block) {
        return getLockedBlock(block.getX(), block.getY(), block.getZ(), block.getWorld().getName());
    }

    private LockedBlock getLockedBlock(int x, int y, int z, String world) {
        for (LockedBlock block : getLockedBlocks()) {
            if (block.getX() == x && block.getY() == y && block.getZ() == z && block.getWorld().equalsIgnoreCase(world)) {
                return block;
            }
        }
        return null;
    }

    public ArrayList<IFaction> getFactionsInVassalageTree(IFaction initialFaction) {
        // create list
        ArrayList<IFaction> foundFactions = new ArrayList<>();

        foundFactions.add(initialFaction);

        boolean newFactionsFound = true;

        int numFactionsFound = -1;

        // while new factions found
        while (newFactionsFound) {
            ArrayList<IFaction> toAdd = new ArrayList<>();
            for (IFaction current : foundFactions) {

                // record number of factions
                numFactionsFound = foundFactions.size();

                // get liege
                IFaction liege = PersistentData.getInstance().getFaction(current.getLiege());
                if (liege != null) {
                    if (!containsFactionByName(toAdd, liege) && !containsFactionByName(foundFactions, liege)) {
                        toAdd.add(liege);
                        numFactionsFound++;
                    }

                    // get vassals of liege
                    for (String vassalName : liege.getVassals()) {
                        IFaction vassal = PersistentData.getInstance().getFaction(vassalName);
                        if (!containsFactionByName(toAdd, vassal) && !containsFactionByName(foundFactions, vassal)) {
                            toAdd.add(vassal);
                            numFactionsFound++;
                        }
                    }
                }

                // get vassals of current
                for (String vassalName : current.getVassals()) {
                    IFaction vassal = PersistentData.getInstance().getFaction(vassalName);
                    if (!containsFactionByName(toAdd, vassal) && !containsFactionByName(foundFactions, vassal)) {
                        toAdd.add(vassal);
                        numFactionsFound++;
                    }
                }
                // if number of factions not different then break loop
                if (numFactionsFound == foundFactions.size()) {
                    newFactionsFound = false;
                }
            }
            foundFactions.addAll(toAdd);
            toAdd.clear();
        }
//        System.out.println(String.format("DEBUG: Found %d factions in vassalage tree of %s", foundFactions.size(), initialFaction.getName()));
        return foundFactions;
    }

    private boolean containsFactionByName(ArrayList<IFaction> list, IFaction faction) {
        for (IFaction f : list) {
            if (f.getName().equalsIgnoreCase(faction.getName())) {
                return true;
            }
        }
        return false;
    }

    // checkers --

    public boolean isInFaction(UUID playerUUID) {
        // membership check
        for (IFaction faction : getFactions()) {
            if (faction.isMember(playerUUID)) {
                return true;
            }
        }
        return false;
    }

    public boolean isBlockLocked(Block block) {
        return isBlockLocked(block.getX(), block.getY(), block.getZ(), block.getWorld().getName());
    }

    private boolean isBlockLocked(int x, int y, int z, String world) {
        for (LockedBlock block : getLockedBlocks()) {
            if (block.getX() == x && block.getY() == y && block.getZ() == z && block.getWorld().equalsIgnoreCase(world)) {
                return true;
            }
        }
        return false;
    }

    public boolean isGateBlock(Block targetBlock)
    {
        for (IFaction faction : getFactions())
        {
            for (Gate gate : faction.getGates())
            {
                if (gate.hasBlock(targetBlock))
                {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isPlayerInFactionInVassalageTree(Player player, IFaction faction) {
        ArrayList<IFaction> factionsToCheck = getFactionsInVassalageTree(faction);
        for (IFaction f : factionsToCheck) {
            if (f.isMember(player.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    public boolean isPlayerInAlliedFaction(Player player, IFaction faction) {
        ArrayList<String> factionNames = faction.getAllies();
        for (String name : factionNames) {
            IFaction f = getFaction(name);
            if (f.isMember(player.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    // actors/mutators

    public void removeAllLocks(String factionName) {
        Iterator<LockedBlock> itr = getLockedBlocks().iterator();

        while (itr.hasNext()) {
            LockedBlock currentBlock = itr.next();
            if (currentBlock.getFactionName().equalsIgnoreCase(factionName)) {
                try {
                    itr.remove();
                }
                catch(Exception e) {
                    System.out.println("An error has occurred during lock removal.");
                }
            }
        }
    }

    public void createActivityRecordForEveryOfflinePlayer() { // this method is to ensure that when updating to a version with power decay, even players who never log in again will experience power decay
        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            PlayerActivityRecord record = PersistentData.getInstance().getPlayerActivityRecord(player.getUniqueId());
            if (record == null) {
                PlayerActivityRecord newRecord = new PlayerActivityRecord(player.getUniqueId(), 1);
                newRecord.setLastLogout(ZonedDateTime.now());
                PersistentData.getInstance().getPlayerActivityRecords().add(newRecord);
            }
        }
    }

    public IFaction getRandomFaction() {
        Random generator = new Random();
        int randomIndex = generator.nextInt(factions.size());
        return factions.get(randomIndex);
    }
}
