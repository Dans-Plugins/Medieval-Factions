/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.events;

import dansplugins.factionsystem.events.abs.FactionEvent;
import dansplugins.factionsystem.objects.domain.Faction;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

/**
 * @author C A L L U M#4160
 */
public class FactionClaimEvent extends FactionEvent implements Cancellable {

    // Variables.
    private boolean cancelled = false;
    private final Chunk chunk;

    /**
     * Constructor to initialise a FactionClaimEvent.
     * @param faction related to the claim.
     * @param player who claimed for the Faction.
     * @param chunk to be claimed.
     */
    public FactionClaimEvent(Faction faction, Player player, Chunk chunk) {
        super(faction, player);
        this.chunk = chunk;
    }

    // Getters.
    public Chunk getChunk() {
        return chunk;
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
