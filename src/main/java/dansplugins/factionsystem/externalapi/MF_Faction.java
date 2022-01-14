/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.externalapi;

import dansplugins.factionsystem.objects.domain.Faction;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * @author Daniel McCoy Stephenson
 */
public class MF_Faction implements IMF_Faction {
    Faction faction;

    public MF_Faction(Faction f) {
        faction = f;
    }

    @Override
    public String getName() {
        return faction.getName();
    }

    @Override
    public String getPrefix() {
        return faction.getPrefix();
    }

    @Override
    public UUID getOwner() {
        return faction.getOwner();
    }

    @Override
    public boolean isMember(Player player) {
        return faction.isMember(player.getUniqueId());
    }

    @Override
    public boolean isOfficer(Player player) {
        return faction.isOfficer(player.getUniqueId());
    }

    @Override
    public Object getFlag(String flag) {
        return faction.getFlags().getFlag(flag);
    }
}
