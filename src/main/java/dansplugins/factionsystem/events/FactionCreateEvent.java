/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

import dansplugins.factionsystem.events.abs.FactionEvent;
import dansplugins.factionsystem.objects.domain.Faction;

/**
 * @author C A L L U M#4160
 */
public class FactionCreateEvent extends FactionEvent implements Cancellable {

    // Variables.
    private boolean cancelled = false;

    /**
     * Constructor to initialise a FactionCreateEvent.
     * <p>
     * This event is called when a Player creates a Faction.
     * </p>
     *
     * @param faction being created.
     * @param player  who created it.
     */
    public FactionCreateEvent(Faction faction, Player player) {
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
