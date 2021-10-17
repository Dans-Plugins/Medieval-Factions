package dansplugins.factionsystem.events;

import dansplugins.factionsystem.objects.IFaction;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

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
    public FactionUnclaimEvent(IFaction faction, Player player, Chunk chunk) {
        super(faction, player, chunk);
    }

}
