package dansplugins.factionsystem.events;

import dansplugins.factionsystem.objects.IFaction;
import org.bukkit.OfflinePlayer;

/**
 * @author C A L L U M#4160
 */
public class FactionKickEvent extends FactionLeaveEvent {

    // Variables.
    private final OfflinePlayer kicker;

    /**
     * Constructor to initialise a FactionKickEvent.
     * <p>
     *     This event is called when a Player is kicked from a Faction.
     * </p>
     *
     * @param faction which the player was kicked from.
     * @param player  who was kicked.
     * @param kicker who kicked the player.
     */
    public FactionKickEvent(IFaction faction, OfflinePlayer player, OfflinePlayer kicker) {
        super(faction, player);
        this.kicker = kicker;
    }

    // Getters.
    public OfflinePlayer getKicker() {
        return kicker;
    }

}
