package dansplugins.factionsystem.events;

import dansplugins.factionsystem.events.abs.FactionEvent;
import dansplugins.factionsystem.objects.Faction;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

/**
 * @author DanTheTechMan#3438
 */
public class FactionWarEndEvent extends FactionEvent implements Cancellable {

    // Variables.
    private boolean cancelled = false;
    private String factionOneName;
    private String factionTwoName;

    /**
     * Constructor to initialise a FactionCreateEvent.
     * <p>
     *     This event is called when a faction declares war on another faction.
     * </p>
     * @param faction1 - First faction involved.
     * @param faction2 - Second faction involved.
     */
    public FactionWarEndEvent(Faction faction1, Faction faction2) {
        super(faction1);
        factionOneName = faction1.getName();
        factionTwoName = faction2.getName();
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

    public String getFirstFaction() {
        return factionOneName;
    }

    public String getSecondFaction() {
        return factionTwoName;
    }
}