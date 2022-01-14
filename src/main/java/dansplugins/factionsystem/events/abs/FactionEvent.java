/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.events.abs;

import dansplugins.factionsystem.objects.domain.Faction;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * @author C A L L U M#4160
 */
public abstract class FactionEvent extends Event {

    // Constants.
    private static final HandlerList handlers = new HandlerList();

    // Variables.
    private final Faction faction;
    private OfflinePlayer offlinePlayer = null;

    /**
     * Constructor for a FactionEvent with a reference to a Faction.
     * @param faction related to the event.
     */
    public FactionEvent(Faction faction) {
        this.faction = faction;
    }

    /**
     * Constructor for a FactionEvent with a reference to both a Faction and player.
     * @param faction related to the event.
     * @param player related to the event.
     */
    public FactionEvent(Faction faction, OfflinePlayer player) {
        this.faction = faction;
        this.offlinePlayer = player;
    }

    // Getters.
    public Faction getFaction() {
        return faction;
    }

    public OfflinePlayer getOfflinePlayer() {
        return offlinePlayer;
    }

    // Bukkit Event API requirements.
    public HandlerList getHandlers() {
        return FactionEvent.handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
