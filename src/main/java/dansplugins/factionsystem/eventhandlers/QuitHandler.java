package dansplugins.factionsystem.eventhandlers;

import java.time.ZonedDateTime;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.domain.ActivityRecord;
import dansplugins.factionsystem.services.LocalActionBarService;

public class QuitHandler implements Listener {

    @EventHandler()
    public void handle(PlayerQuitEvent event) {
        EphemeralData.getInstance().getLockingPlayers().remove(event.getPlayer().getUniqueId());
        EphemeralData.getInstance().getUnlockingPlayers().remove(event.getPlayer().getUniqueId());
        EphemeralData.getInstance().getPlayersGrantingAccess().remove(event.getPlayer().getUniqueId());
        EphemeralData.getInstance().getPlayersCheckingAccess().remove(event.getPlayer().getUniqueId());
        EphemeralData.getInstance().getPlayersRevokingAccess().remove(event.getPlayer().getUniqueId());

        ActivityRecord record = PersistentData.getInstance().getPlayerActivityRecord(event.getPlayer().getUniqueId());
        if (record != null) {
            record.setLastLogout(ZonedDateTime.now());
        }

        LocalActionBarService.getInstance(MedievalFactions.getInstance()).clearPlayerActionBar(event.getPlayer());
    }
}