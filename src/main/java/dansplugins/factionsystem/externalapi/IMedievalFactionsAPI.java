package dansplugins.factionsystem.externalapi;

import org.bukkit.entity.Player;

import java.util.UUID;

public interface IMedievalFactionsAPI {
    MF_Faction getFaction(String factionName);
    MF_Faction getFaction(Player player);
    void forcePlayerToLeaveFactionChat(UUID uuid);
}
