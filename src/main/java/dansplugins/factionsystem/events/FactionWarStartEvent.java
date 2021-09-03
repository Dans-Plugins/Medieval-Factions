package dansplugins.factionsystem.events;

import dansplugins.factionsystem.objects.Faction;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

/**
 * @author DanTheTechMan#3438
 */
public class FactionWarStartEvent extends FactionClaimEvent {

    /**
     * Constructor to initialise a FactionWarStartEvent.
     *
     * @param faction related to the claim.
     * @param player  who unclaimed for the Faction.
     * @param chunk   to be unclaimed.
     */
    public FactionWarStartEvent(Faction faction, Player player, Chunk chunk) {
        super(faction, player, chunk);
    }

}