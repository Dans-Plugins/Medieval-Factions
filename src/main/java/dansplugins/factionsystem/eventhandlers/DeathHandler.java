package dansplugins.factionsystem.eventhandlers;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.domain.PowerRecord;
import dansplugins.factionsystem.services.LocalConfigService;
import dansplugins.factionsystem.utils.Locale;

public class DeathHandler implements Listener {

    @EventHandler()
    public void handle(PlayerDeathEvent event) {
        event.getEntity();
        Player player = event.getEntity();
        if (LocalConfigService.getInstance().getBoolean("playersLosePowerOnDeath")) {
            decreaseDyingPlayersPower(player);
        }
        if (!wasPlayersCauseOfDeathAnotherPlayerKillingThem(player)) {
            return;
        }
        Player killer = player.getKiller();
        if (killer == null) {
            return;
        }
        PowerRecord record = PersistentData.getInstance().getPlayersPowerRecord(killer.getUniqueId());
        if (record == null) {
            return;
        }
        record.grantPowerDueToKill();
        killer.sendMessage(ChatColor.GREEN + Locale.get("PowerLevelHasIncreased"));
    }

    private boolean wasPlayersCauseOfDeathAnotherPlayerKillingThem(Player player) {
        return player.getKiller() != null;
    }

    private void decreaseDyingPlayersPower(Player player) {
        PowerRecord playersPowerRecord = PersistentData.getInstance().getPlayersPowerRecord(player.getUniqueId());
        double powerLost = playersPowerRecord.revokePowerDueToDeath();
        if (powerLost != 0) {
            player.sendMessage(ChatColor.RED + "You lost " + powerLost + " power.");
        }
    }
}