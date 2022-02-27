/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.externalapi;

import java.util.ArrayList;
import java.util.UUID;

import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.objects.domain.PowerRecord;

/**
 * @author Daniel McCoy Stephenson
 * @brief This class gives developers access to the external API for Medieval Factions.
 */
public class MedievalFactionsAPI {
    private final String APIVersion = "v1.0.0"; // every time the external API is altered, this should be incremented
    private ArrayList<String> allyFactions = new ArrayList<>();
    private ArrayList<String> enemyFactions = new ArrayList<>();

    public String getAPIVersion() {
        return APIVersion;
    }

    public String getVersion() {
        return MedievalFactions.getInstance().getVersion();
    }

    public MF_Faction getFaction(String factionName) {
        Faction faction = PersistentData.getInstance().getFaction(factionName);
        if (faction == null) {
            return null;
        }
        return new MF_Faction(faction);
    }

    public MF_Faction getFaction(Player player) {
        Faction faction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());
        if (faction == null) {
            return null;
        }
        return new MF_Faction(faction);
    }

    public MF_Faction getFaction(UUID playerUUID) {
        Faction faction = PersistentData.getInstance().getPlayersFaction(playerUUID);
        if (faction == null) {
            return null;
        }
        return new MF_Faction(faction);
    }

    public boolean isPlayerInFactionChat(Player player) {
        return EphemeralData.getInstance().isPlayerInFactionChat(player);
    }

    public boolean isPrefixesFeatureEnabled() {
        return MedievalFactions.getInstance().getConfig().getBoolean("playersChatWithPrefixes");
    }

    public boolean isChunkClaimed(Chunk chunk) {
        return (PersistentData.getInstance().getChunkDataAccessor().getClaimedChunk(chunk) != null);
    }

    public double getPower(Player player) {
        return PersistentData.getInstance().getPlayersPowerRecord(player.getUniqueId()).getPower();
    }

    public double getPower(UUID playerUUID) {
        return PersistentData.getInstance().getPlayersPowerRecord(playerUUID).getPower();
    }

    public void forcePlayerToLeaveFactionChat(UUID uuid) {
        EphemeralData.getInstance().getPlayersInFactionChat().remove(uuid);
    }

    public void increasePower(Player player, int amount) {
        PowerRecord powerRecord = PersistentData.getInstance().getPlayersPowerRecord(player.getUniqueId());
        double originalPower = powerRecord.getPower();
        double newPower = originalPower + amount;
        powerRecord.setPower(newPower);
    }

    public void decreasePower(Player player, int amount) {
        PowerRecord powerRecord = PersistentData.getInstance().getPlayersPowerRecord(player.getUniqueId());
        double originalPower = powerRecord.getPower();
        double newPower = originalPower - amount;
        if (newPower >= 0) {
            powerRecord.setPower(originalPower - amount);
        } else {
            powerRecord.setPower(0);
        }
    }

    /**
     * Checks the faction's list of allies for the supplied faction name
     * @return boolean
     */
    public boolean isAlly(String factionname) {
        return containsIgnoreCase(allyFactions, factionname);
    }

    /**
     * @return list of allied factions
     */
    public ArrayList<String> getAllies() {
        return allyFactions;
    }

    /**
     * @return boolean
     */
    public boolean isEnemy(String factionName) {
        return containsIgnoreCase(enemyFactions, factionName);
    }

    /**
     * @return list of enemies for the faction
     */
    public ArrayList<String> getEnemies() {
        return enemyFactions;
    }

    private boolean containsIgnoreCase(ArrayList<String> list, String str) {
        for (String string : list) {
            if (string.equalsIgnoreCase(str)) {
                return true;
            }
        }
        return false;
    }
}