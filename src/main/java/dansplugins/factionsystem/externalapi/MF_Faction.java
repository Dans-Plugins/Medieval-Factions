/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.externalapi;

import java.util.UUID;

import org.bukkit.entity.Player;

import dansplugins.factionsystem.objects.domain.Faction;

/**
 * @author Daniel McCoy Stephenson
 */
public class MF_Faction {
    Faction faction;

    public MF_Faction(Faction f) {
        faction = f;
    }

    public String getName() {
        return faction.getName();
    }

    public String getPrefix() {
        return faction.getPrefix();
    }

    public UUID getOwner() {
        return faction.getOwner();
    }

    public boolean isMember(Player player) {
        return faction.isMember(player.getUniqueId());
    }

    public boolean isOfficer(Player player) {
        return faction.isOfficer(player.getUniqueId());
    }

    public Object getFlag(String flag) {
        return faction.getFlags().getFlag(flag);
    }
}
