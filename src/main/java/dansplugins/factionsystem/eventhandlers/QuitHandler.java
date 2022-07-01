package dansplugins.factionsystem.eventhandlers;

import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.domain.ActivityRecord;
import dansplugins.factionsystem.services.ActionBarService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.ZonedDateTime;

public class QuitHandler implements Listener {
    private final EphemeralData ephemeralData;
    private final PersistentData persistentData;
    private final ActionBarService actionBarService;

    public QuitHandler(EphemeralData ephemeralData, PersistentData persistentData, ActionBarService actionBarService) {
        this.ephemeralData = ephemeralData;
        this.persistentData = persistentData;
        this.actionBarService = actionBarService;
    }

    @EventHandler()
    public void handle(PlayerQuitEvent event) {
        ephemeralData.getLockingPlayers().remove(event.getPlayer().getUniqueId());
        ephemeralData.getUnlockingPlayers().remove(event.getPlayer().getUniqueId());
        ephemeralData.getPlayersGrantingAccess().remove(event.getPlayer().getUniqueId());
        ephemeralData.getPlayersCheckingAccess().remove(event.getPlayer().getUniqueId());
        ephemeralData.getPlayersRevokingAccess().remove(event.getPlayer().getUniqueId());

        ActivityRecord record = persistentData.getPlayerActivityRecord(event.getPlayer().getUniqueId());
        if (record != null) {
            record.setLastLogout(ZonedDateTime.now());
        }

        actionBarService.clearPlayerActionBar(event.getPlayer());
    }
}