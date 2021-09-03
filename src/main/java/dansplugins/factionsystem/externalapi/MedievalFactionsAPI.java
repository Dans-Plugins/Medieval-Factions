package dansplugins.factionsystem.externalapi;

import dansplugins.factionsystem.data.PersistentData;
import org.bukkit.entity.Player;

public class MedievalFactionsAPI implements IMedievalFactionsAPI {

    @Override
    public FactionInfo getFaction(String factionName) {
        return new FactionInfo(PersistentData.getInstance().getFaction(factionName));
    }

    @Override
    public FactionInfo getFaction(Player player) {
        return new FactionInfo(PersistentData.getInstance().getPlayersFaction(player.getUniqueId()));
    }
}
