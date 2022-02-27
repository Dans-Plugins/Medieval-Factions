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
 * @author DanTheTechMan#3438
 */
public class FactionWarStartEvent extends FactionEvent implements Cancellable {

    private final String attackerName;
    private final String defenderName;
    private final String playerName;
    // Variables.
    private boolean cancelled = false;

    /**
     * Constructor to initialise a FactionWarStartEvent.
     * <p>
     * This event is called when a faction declares war on another faction.
     * </p>
     *
     * @param attacker - Faction declaring war.
     * @param defender - Faction getting declared war on.
     * @param declarer - Player responsible.
     */
    public FactionWarStartEvent(Faction attacker, Faction defender, Player declarer) {
        super(attacker, declarer);
        attackerName = attacker.getName();
        defenderName = defender.getName();
        playerName = declarer.getName();
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

    public String getAttacker() {
        return attackerName;
    }

    public String getDefender() {
        return defenderName;
    }

    public String getDeclarer() {
        return playerName;
    }
}