package dansplugins.factionsystem.externalapi;

import org.bukkit.entity.Player;

import java.util.UUID;

public interface IMedievalFactionsAPI {

    // accessors
    MF_Faction getFaction(String factionName);
    MF_Faction getFaction(Player player);
    boolean isPlayerInFactionChat(Player player);
    boolean isPrefixesFeatureEnabled();

    // mutators
    void forcePlayerToLeaveFactionChat(UUID uuid);
}
