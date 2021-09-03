package dansplugins.factionsystem.events;

import dansplugins.factionsystem.objects.Faction;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

/**
 * @author C A L L U M#4160
 */
public class FactionWarEndEvent extends FactionClaimEvent {

    /**
     * Constructor to initialise a FactionWarEndEvent.
     *
     * @param faction related to the claim.
     * @param player  who unclaimed for the Faction.
     * @param chunk   to be unclaimed.
     */
    public FactionWarEndEvent(Faction faction, Player player, Chunk chunk) {
        super(faction, player, chunk);
    }

}