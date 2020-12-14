package dansplugins.factionsystem.data;

import dansplugins.factionsystem.objects.*;

import java.util.ArrayList;
import java.util.UUID;

public class PersistentData {

    // instance
    private static PersistentData instance;

    public ArrayList<Faction> factions = new ArrayList<>();
    public ArrayList<ClaimedChunk> claimedChunks = new ArrayList<>();
    public ArrayList<PlayerPowerRecord> playerPowerRecords = new ArrayList<>();
    public ArrayList<PlayerActivityRecord> playerActivityRecords = new ArrayList<>();
    public ArrayList<LockedBlock> lockedBlocks = new ArrayList<>();

    private PersistentData() {

    }

    public static PersistentData getInstance() {
        if (instance == null) {
            instance = new PersistentData();
        }
        return instance;
    }

    // arraylist getters ---

    public ArrayList<Faction> getFactions() {
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
}
