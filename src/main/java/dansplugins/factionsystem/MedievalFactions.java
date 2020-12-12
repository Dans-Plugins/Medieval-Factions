package dansplugins.factionsystem;

import dansplugins.factionsystem.bstats.Metrics;
import dansplugins.factionsystem.utils.Utilities;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class MedievalFactions extends JavaPlugin {

    // instance
    private static MedievalFactions instance;

    // version
    public static String version = "v3.6.0.3-beta-8";

    public Utilities utilities = new Utilities();

    public static MedievalFactions getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        System.out.println("Medieval Factions plugin enabling....");

        instance = this;

        utilities.ensureSmoothTransitionBetweenVersions();

        // config creation/loading
        if (!(new File("./plugins/MedievalFactions/config.yml").exists())) {
            ConfigManager.getInstance().saveConfigDefaults();
        }
        else {
            // pre load compatibility checks
            if (!getConfig().getString("version").equalsIgnoreCase(MedievalFactions.version)) {
                System.out.println("[ALERT] Version mismatch! Adding missing defaults and setting version!");
                ConfigManager.getInstance().handleVersionMismatch();
            }
            reloadConfig();
        }

        Scheduler.getInstance().schedulePowerIncrease();
        Scheduler.getInstance().schedulePowerDecrease();
        Scheduler.getInstance().scheduleAutosave();

        EventRegistry.getInstance().registerEvents();

        StorageManager.getInstance().load();

        // post load compatibility checks
        if (!getConfig().getString("version").equalsIgnoreCase(MedievalFactions.version)) {
            utilities.createActivityRecordForEveryOfflinePlayer(); // make sure every player experiences power decay in case we updated from pre-v3.5
        }

        int pluginId = 8929;
        Metrics metrics = new Metrics(this, pluginId);

        System.out.println("Medieval Factions plugin enabled.");
    }

    @Override
    public void onDisable() {
        System.out.println("Medieval Factions plugin disabling....");
        StorageManager.getInstance().save();
        System.out.println("Medieval Factions plugin disabled.");
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        return CommandInterpreter.getInstance().interpretCommand(sender, label, args);
    }

}