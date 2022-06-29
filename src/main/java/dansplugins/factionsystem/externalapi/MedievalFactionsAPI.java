/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.externalapi;

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

    public String getAPIVersion() {
        return APIVersion;
    }

    public String getVersion() {
        return medievalFactions.getVersion();
    }

    public MF_Faction getFaction(String factionName) {
        Faction faction = persistentData.getFaction(factionName);
        if (faction == null) {
            return null;
        }
        return new MF_Faction(faction);
    }

    public MF_Faction getFaction(Player player) {
        Faction faction = persistentData.getPlayersFaction(player.getUniqueId());
        if (faction == null) {
            return null;
        }
        return new MF_Faction(faction);
    }

    public MF_Faction getFaction(UUID playerUUID) {
        Faction faction = persistentData.getPlayersFaction(playerUUID);
        if (faction == null) {
            return null;
        }
        return new MF_Faction(faction);
    }

    public boolean isPlayerInFactionChat(Player player) {
        return ephemeralData.isPlayerInFactionChat(player);
    }

    public boolean isPrefixesFeatureEnabled() {
        return configService.getBoolean("playersChatWithPrefixes");
    }

    public boolean isChunkClaimed(Chunk chunk) {
        return (persistentData.getChunkDataAccessor().getClaimedChunk(chunk) != null);
    }

    public double getPower(Player player) {
        return persistentData.getPlayersPowerRecord(player.getUniqueId()).getPower();
    }

    public double getPower(UUID playerUUID) {
        return persistentData.getPlayersPowerRecord(playerUUID).getPower();
    }

    public void forcePlayerToLeaveFactionChat(UUID uuid) {
        ephemeralData.getPlayersInFactionChat().remove(uuid);
    }

    public void increasePower(Player player, int amount) {
        PowerRecord powerRecord = persistentData.getPlayersPowerRecord(player.getUniqueId());
        double originalPower = powerRecord.getPower();
        double newPower = originalPower + amount;
        powerRecord.setPower(newPower);
    }

    public void decreasePower(Player player, int amount) {
        PowerRecord powerRecord = persistentData.getPlayersPowerRecord(player.getUniqueId());
        double originalPower = powerRecord.getPower();
        double newPower = originalPower - amount;
        if (newPower >= 0) {
            powerRecord.setPower(originalPower - amount);
        } else {
            powerRecord.setPower(0);
        }
    }
}