package dansplugins.factionsystem.events;

import dansplugins.factionsystem.events.abs.FactionEvent;
import dansplugins.factionsystem.objects.IFaction;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

/**
 * @author C A L L U M#4160
 */
public class FactionCreateEvent extends FactionEvent implements Cancellable {

    // Variables.
    private boolean cancelled = false;

    /**
     * Constructor to initialise a FactionCreateEvent.
     * <p>
     *     This event is called when a Player creates a Faction.
     * </p>
     * @param faction being created.
     * @param player who created it.
     */
    public FactionCreateEvent(IFaction faction, Player player) {
        super(faction, player);
    }

    // Cancellable methodology.
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        this.cancelled = b;
    }
}
