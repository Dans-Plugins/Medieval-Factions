package dansplugins.factionsystem.eventhandlers;

import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.domain.PowerRecord;
import dansplugins.factionsystem.services.LocalConfigService;
import dansplugins.factionsystem.services.LocalLocaleService;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

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
        if (record.increasePowerByTenPercent()) {
            killer.sendMessage(ChatColor.GREEN + LocalLocaleService.getInstance().getText("PowerLevelHasIncreased"));
        }
    }

    private boolean wasPlayersCauseOfDeathAnotherPlayerKillingThem(Player player) {
        return player.getKiller() != null;
    }

    private void decreaseDyingPlayersPower(Player player) {
        PowerRecord playersPowerRecord = PersistentData.getInstance().getPlayersPowerRecord(player.getUniqueId());
        int powerLost = playersPowerRecord.decreasePowerByTenPercent();
        if (powerLost != 0) {
            player.sendMessage(ChatColor.RED + "You lost " + powerLost + " power."); // TODO: add locale message
        }
    }
}