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
    private static MedievalFactionsAPI API;

    private String version = "v4.1-beta-2";

    public static MedievalFactions getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        API = new MedievalFactionsAPI();

        // create/load config
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

        // load strings and save files
        LocaleManager.getInstance().loadStrings();
        StorageManager.getInstance().load();

        // schedule recurring tasks
        Scheduler.getInstance().schedulePowerIncrease();
        Scheduler.getInstance().schedulePowerDecrease();
        Scheduler.getInstance().scheduleAutosave();

        // register events
        EventRegistry.getInstance().registerEvents();

        // make sure every player experiences power decay in case we updated from pre-v3.5
        createActivityRecordForEveryOfflinePlayer();

        // bStats
        int pluginId = 8929;
        Metrics metrics = new Metrics(this, pluginId);

        // Dynmap
        if (DynmapIntegrator.hasDynmap()) {
            DynmapIntegrator.getInstance().scheduleClaimsUpdate(600); // Check once every 30 seconds for updates.
            DynmapIntegrator.getInstance().updateClaims();
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

    public MedievalFactionsAPI getAPI() {
        return API;
    }

    // this method is to ensure that when updating to a version with power decay, even players who never log in again will experience power decay
    private void createActivityRecordForEveryOfflinePlayer() {
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
