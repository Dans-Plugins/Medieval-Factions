package dansplugins.factionsystem.externalapi;

import org.bukkit.entity.Player;

public interface IMedievalFactionsAPI {
    FactionInfo getFaction(String factionName);
    FactionInfo getFaction(Player player);
}
