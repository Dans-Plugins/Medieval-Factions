package dansplugins.factionsystem.externalapi;

import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.UUIDChecker;

public class FactionInfo implements IFactionInfo {
    private String name;
    private String owner;

    public FactionInfo(Faction faction) {
        name = faction.getName();
        owner = UUIDChecker.getInstance().findPlayerNameBasedOnUUID(faction.getOwner());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getOwner() {
        return owner;
    }
}
