package dansplugins.factionsystem.externalapi;

import dansplugins.factionsystem.data.PersistentData;
import org.bukkit.entity.Player;

/*
    This class gives developers access to the external API for Medieval Factions.
*/
public class MedievalFactionsAPI implements IMedievalFactionsAPI {

    @Override
    public MF_Faction getFaction(String factionName) {
        return new MF_Faction(PersistentData.getInstance().getFaction(factionName));
    }

    @Override
    public MF_Faction getFaction(Player player) {
        return new MF_Faction(PersistentData.getInstance().getPlayersFaction(player.getUniqueId()));
    }
}
