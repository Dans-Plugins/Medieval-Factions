package dansplugins.factionsystem;

import dansplugins.factionsystem.bstats.Metrics;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.PlayerActivityRecord;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.time.ZonedDateTime;

public class MedievalFactions extends JavaPlugin {

    private static MedievalFactions instance;

    private String version = "v3.7-beta-7";

    public static MedievalFactions getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        ensureSmoothTransitionBetweenVersions();

        // config creation/loading
        if (!(new File("./plugins/MedievalFactions/config.yml").exists())) {
            ConfigManager.getInstance().saveConfigDefaults();
        }
        else {
            // pre load compatibility checks
            if (isVersionMismatched()) {
                ConfigManager.getInstance().handleVersionMismatch();
            }
            reloadConfig();
        }

        LocaleManager.getInstance().loadStrings();

        Scheduler.getInstance().schedulePowerIncrease();
        Scheduler.getInstance().schedulePowerDecrease();
        Scheduler.getInstance().scheduleAutosave();

        EventRegistry.getInstance().registerEvents();

        StorageManager.getInstance().load();

        // post load compatibility checks
        if (isVersionMismatched()) {
            createActivityRecordForEveryOfflinePlayer(); // make sure every player experiences power decay in case we updated from pre-v3.5
        }

        int pluginId = 8929;
        Metrics metrics = new Metrics(this, pluginId);

        if (DynmapManager.hasDynmap()) {
            DynmapManager.scheduleClaimsUpdate(600); // Check once every 30 seconds for updates.
            DynmapManager.updateClaims();
        }
    }

    @Override
    public void onDisable() {
        StorageManager.getInstance().save();
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        return CommandInterpreter.getInstance().interpretCommand(sender, label, args);
    }

    public String getVersion() {
        return version;
    }

    public boolean isVersionMismatched() {
        return !getConfig().getString("version").equalsIgnoreCase(getVersion());
    }

    private void ensureSmoothTransitionBetweenVersions() {
        // this piece of code is to ensure that saves don't become broken when updating to v3.2 from a previous version
        File saveFolder = new File("./plugins/medievalfactions/");
        if (saveFolder.exists()) { // TODO: fix this so that it doesn't run every time
//            System.out.println("[ALERT] Old save folder name (pre v3.2) detected. Updating for compatibility.");

            // rename directory
            File newSaveFolder = new File("./plugins/MedievalFactions/");
            saveFolder.renameTo(newSaveFolder);
        }

        // this piece of code is to fix config values not matching when updating to v3.3 (after v3.3 there is version mismatch handling)
        if (!MedievalFactions.getInstance().getConfig().isSet("version")) {
            ConfigManager.getInstance().handleVersionMismatch();
        }
    }

    // this method is to ensure that when updating to a version with power decay, even players who never log in again will experience power decay
    private void createActivityRecordForEveryOfflinePlayer() { // TODO: ensure that this is working
        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            PlayerActivityRecord record = PersistentData.getInstance().getPlayerActivityRecord(player.getUniqueId());
            if (record == null) {
                PlayerActivityRecord newRecord = new PlayerActivityRecord(player.getUniqueId(), 1);
                newRecord.setLastLogout(ZonedDateTime.now());
                PersistentData.getInstance().getPlayerActivityRecords().add(newRecord);
            }
        }
    }

}
