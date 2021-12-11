package dansplugins.factionsystem.externalapi;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.services.LocalChunkService;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.objects.domain.PowerRecord;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.UUID;

/*
    This class gives developers access to the external API for Medieval Factions.
*/
public class MedievalFactionsAPI implements IMedievalFactionsAPI {

    private final String APIVersion = "v1.0.0"; // every time the external API is altered, this should be incremented

    // accessors

    @Override
    public String getAPIVersion() {
        return APIVersion;
    }

    @Override
    public String getVersion() {
        return MedievalFactions.getInstance().getVersion();
    }

    @Override
    public MF_Faction getFaction(String factionName) {
        Faction faction = PersistentData.getInstance().getFaction(factionName);
        if (faction == null) {
            return null;
        }
        return new MF_Faction(faction);
    }

    @Override
    public MF_Faction getFaction(Player player) {
        Faction faction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());
        if (faction == null) {
            return null;
        }
        return new MF_Faction(faction);
    }

    @Override
    public MF_Faction getFaction(UUID playerUUID) {
        Faction faction = PersistentData.getInstance().getPlayersFaction(playerUUID);
        if (faction == null) {
            return null;
        }
        return new MF_Faction(faction);
    }

    @Override
    public boolean isPlayerInFactionChat(Player player) {
        return EphemeralData.getInstance().isPlayerInFactionChat(player);
    }
  
    @Override
    public boolean isPrefixesFeatureEnabled() {
        return MedievalFactions.getInstance().getConfig().getBoolean("playersChatWithPrefixes");
    }

    @Override
    public boolean isChunkClaimed(Chunk chunk) {
        return (LocalChunkService.getInstance().getClaimedChunk(chunk) != null);
    }

    @Override
    public int getPower(Player player) {
        return PersistentData.getInstance().getPlayersPowerRecord(player.getUniqueId()).getPowerLevel();
    }

    @Override
    public int getPower(UUID playerUUID) {
        return PersistentData.getInstance().getPlayersPowerRecord(playerUUID).getPowerLevel();
    }

    // mutators

    @Override
    public void forcePlayerToLeaveFactionChat(UUID uuid) {
        EphemeralData.getInstance().getPlayersInFactionChat().remove(uuid);
    }

    @Override
    public void increasePower(Player player, int amount) {
        PowerRecord powerRecord = PersistentData.getInstance().getPlayersPowerRecord(player.getUniqueId());
        int originalPower = powerRecord.getPowerLevel();
        int newPower = originalPower + amount;
        powerRecord.setPowerLevel(newPower);
    }

    @Override
    public void decreasePower(Player player, int amount) {
        PowerRecord powerRecord = PersistentData.getInstance().getPlayersPowerRecord(player.getUniqueId());
        int originalPower = powerRecord.getPowerLevel();
        int newPower = originalPower - amount;
        if (newPower >= 0) {
            powerRecord.setPowerLevel(originalPower - amount);
        }
        else {
            powerRecord.setPowerLevel(0);
        }
    }
}