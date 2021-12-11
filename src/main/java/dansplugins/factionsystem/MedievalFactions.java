package dansplugins.factionsystem;

import dansplugins.factionsystem.bstats.Metrics;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.eventhandlers.*;
import dansplugins.factionsystem.externalapi.MedievalFactionsAPI;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.services.LocalConfigService;
import dansplugins.factionsystem.services.LocalLocaleService;
import dansplugins.factionsystem.services.LocalStorageService;
import dansplugins.factionsystem.placeholders.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import preponderous.ponder.AbstractPonderPlugin;
import preponderous.ponder.misc.PonderAPI_Integrator;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author Daniel Stephenson
 */
public class MedievalFactions extends AbstractPonderPlugin {

    private static MedievalFactions instance;

    private final String pluginVersion = "v" + getDescription().getVersion();

    public static MedievalFactions getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        ponderAPI_integrator = new PonderAPI_Integrator(this);
        toolbox = getPonderAPI().getToolbox();

        // create/load config
        if (!(new File("./plugins/MedievalFactions/config.yml").exists())) {
            LocalConfigService.getInstance().saveConfigDefaults();
        }
        else {
            // pre load compatibility checks
            if (isVersionMismatched()) {
                LocalConfigService.getInstance().handleVersionMismatch();
            }
            reloadConfig();
        }

        // load strings and save files
        LocalLocaleService.getInstance().loadStrings();
        LocalStorageService.getInstance().load();

        // schedule recurring tasks
        Scheduler.getInstance().schedulePowerIncrease();
        Scheduler.getInstance().schedulePowerDecrease();
        Scheduler.getInstance().scheduleAutosave();

        // register event handlers
        registerEventHandlers();

        // make sure every player experiences power decay in case we updated from pre-v3.5
        PersistentData.getInstance().createActivityRecordForEveryOfflinePlayer();

        // bStats
        int pluginId = 8929;
        Metrics metrics = new Metrics(this, pluginId);

        // Dynmap
        if (DynmapIntegrator.hasDynmap()) {
            DynmapIntegrator.getInstance().scheduleClaimsUpdate(600); // Check once every 30 seconds for updates.
            DynmapIntegrator.getInstance().updateClaims();
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderAPI().register();
        }

    }

    @Override
    public void onDisable() {
        LocalStorageService.getInstance().save();
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        return CommandInterpreter.getInstance().interpretCommand(sender, label, args);
    }

    public String getVersion() {
        return pluginVersion;
    }

    public boolean isVersionMismatched() {
        String configVersion = getConfig().getString("version");
        if (configVersion == null || this.getVersion() == null) {
            return true;
        } else {
            return !configVersion.equalsIgnoreCase(this.getVersion());
        }
    }

    public MedievalFactionsAPI getAPI() {
        return new MedievalFactionsAPI();
    }

    @Deprecated
    public SingletonShelf getInternalAPI() {
        return new SingletonShelf();
    }

    public boolean isDebugEnabled() {
        return getConfig().getBoolean("debugMode");
    }

    private void registerEventHandlers() {
        ArrayList<Listener> listeners = new ArrayList<>(Arrays.asList(
                new ChatHandler(),
                new DamageEffectsAndDeathHandler(),
                new InteractionHandler(),
                new JoiningLeavingAndSpawningHandler(),
                new MoveHandler()
        ));
        getToolbox().getEventHandlerRegistry().registerEventHandlers(listeners, this);
    }

}
