package dansplugins.factionsystem.externalapi;

import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import org.bukkit.entity.Player;

/*
    This class gives developers access to the external API for Medieval Factions.
*/
public class MedievalFactionsAPI implements IMedievalFactionsAPI {

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
}
