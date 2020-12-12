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
    public static String version = "v3.6.0.3-beta-7";

    // scheduler
    public Scheduler scheduler = new Scheduler();

    // subsystems
    public StorageSubsystem storage = new StorageSubsystem();
    public UtilitySubsystem utilities = new UtilitySubsystem();
    public ConfigSubsystem config = new ConfigSubsystem();

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
            config.saveConfigDefaults();
        }
        else {
            // pre load compatibility checks
            if (!getConfig().getString("version").equalsIgnoreCase(MedievalFactions.version)) {
                System.out.println("[ALERT] Version mismatch! Adding missing defaults and setting version!");
                config.handleVersionMismatch();
            }
            reloadConfig();
        }

        scheduler.schedulePowerIncrease();
        scheduler.schedulePowerDecrease();
        scheduler.scheduleAutosave();

        EventRegistry.registerEvents();

        storage.load();

        // post load compatibility checks
        if (!getConfig().getString("version").equalsIgnoreCase(MedievalFactions.version)) {
            utilities.createActivityRecordForEveryOfflinePlayer(); // make sure every player experiences power decay in case we updated from pre-v3.5
        }

        int pluginId = 8929;
        Metrics metrics = new Metrics(this, pluginId);

        System.out.println("Medieval Factions plugin enabled.");
    }

    @Override
    public void onDisable(){
        System.out.println("Medieval Factions plugin disabling....");
        storage.save();
        System.out.println("Medieval Factions plugin disabled.");
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        CommandSubsystem commandInterpreter = new CommandSubsystem();
        return commandInterpreter.interpretCommand(sender, label, args);
    }

}
