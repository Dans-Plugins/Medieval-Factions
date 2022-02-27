/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.events;

import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import dansplugins.factionsystem.objects.domain.Faction;

/**
 * @author C A L L U M#4160
 */
public class FactionUnclaimEvent extends FactionClaimEvent {

    /**
     * Constructor to initialise a FactionUnclaimEvent.
     *
     * @param faction related to the claim.
     * @param player  who unclaimed for the Faction.
     * @param chunk   to be unclaimed.
     */
    public FactionUnclaimEvent(Faction faction, Player player, Chunk chunk) {
        super(faction, player, chunk);
    }

}
