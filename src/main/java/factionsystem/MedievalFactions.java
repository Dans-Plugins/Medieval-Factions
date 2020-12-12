package factionsystem;

import factionsystem.Subsystems.CommandSubsystem;
import factionsystem.Subsystems.ConfigSubsystem;
import factionsystem.Subsystems.StorageSubsystem;
import factionsystem.Subsystems.UtilitySubsystem;
import factionsystem.bStats.Metrics;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class MedievalFactions extends JavaPlugin {

    // instance
    private static MedievalFactions instance;

    // version
    public static String version = "v3.6.0.3-beta-8";

    public UtilitySubsystem utilities = new UtilitySubsystem();

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
            ConfigSubsystem.getInstance().saveConfigDefaults();
        }
        else {
            // pre load compatibility checks
            if (!getConfig().getString("version").equalsIgnoreCase(MedievalFactions.version)) {
                System.out.println("[ALERT] Version mismatch! Adding missing defaults and setting version!");
                ConfigSubsystem.getInstance().handleVersionMismatch();
            }
            reloadConfig();
        }

        Scheduler.getInstance().schedulePowerIncrease();
        Scheduler.getInstance().schedulePowerDecrease();
        Scheduler.getInstance().scheduleAutosave();

        EventRegistry.registerEvents();

        StorageSubsystem.getInstance().load();

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
        StorageSubsystem.getInstance().save();
        System.out.println("Medieval Factions plugin disabled.");
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        return CommandSubsystem.getInstance().interpretCommand(sender, label, args);
    }

}