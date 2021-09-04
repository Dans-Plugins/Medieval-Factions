package dansplugins.factionsystem.externalapi;

import org.bukkit.entity.Player;

public interface IMedievalFactionsAPI {
    MF_Faction getFaction(String factionName);
    MF_Faction getFaction(Player player);
}
