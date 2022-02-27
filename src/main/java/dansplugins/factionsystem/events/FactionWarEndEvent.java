/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.events;

import org.bukkit.event.Cancellable;

import dansplugins.factionsystem.events.abs.FactionEvent;
import dansplugins.factionsystem.objects.domain.Faction;

/**
 * @author DanTheTechMan#3438
 */
public class FactionWarEndEvent extends FactionEvent implements Cancellable {

    private final String factionOneName;
    private final String factionTwoName;
    // Variables.
    private boolean cancelled = false;

    /**
     * Constructor to initialise a FactionWarEndEvent.
     * <p>
     * This event is called when a war ends due to a peace agreement.
     * </p>
     *
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