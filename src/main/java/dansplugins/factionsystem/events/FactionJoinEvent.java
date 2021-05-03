package dansplugins.factionsystem.events;

import dansplugins.factionsystem.events.abs.FactionEvent;
import dansplugins.factionsystem.objects.Faction;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Cancellable;

/**
 * @author C A L L U M#4160
 */
public class FactionJoinEvent extends FactionEvent implements Cancellable {

    // Variables.
    private boolean cancelled = false;

    /**
     * Constructor to initialise a FactionJoinEvent
     * <p>
     *     This event is called when a Player joins a Faction.
     * </p>
     * @param faction which was joined.
     * @param player who joined.
     */
    public FactionJoinEvent(Faction faction, OfflinePlayer player) {
        super(faction, player);
    }

    // Bukkit Cancellable methodology.
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        this.cancelled = b;
    }

}
