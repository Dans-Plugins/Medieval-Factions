/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import dansplugins.factionsystem.bstats.Metrics;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.eventhandlers.ChatHandler;
import dansplugins.factionsystem.eventhandlers.DamageHandler;
import dansplugins.factionsystem.eventhandlers.DeathHandler;
import dansplugins.factionsystem.eventhandlers.EffectHandler;
import dansplugins.factionsystem.eventhandlers.InteractionHandler;
import dansplugins.factionsystem.eventhandlers.JoinHandler;
import dansplugins.factionsystem.eventhandlers.MoveHandler;
import dansplugins.factionsystem.eventhandlers.QuitHandler;
import dansplugins.factionsystem.eventhandlers.SpawnHandler;
import dansplugins.factionsystem.externalapi.MedievalFactionsAPI;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.placeholders.PlaceholderAPI;
import dansplugins.factionsystem.services.LocalCommandService;
import dansplugins.factionsystem.services.LocalConfigService;
import dansplugins.factionsystem.services.LocalLocaleService;
import dansplugins.factionsystem.utils.extended.Scheduler;
import preponderous.ponder.minecraft.bukkit.tools.EventHandlerRegistry;

/**
 * @author Daniel McCoy Stephenson
 * @since May 30th, 2020
 */
public class MedievalFactions extends JavaPlugin {
    private static MedievalFactions instance;
    private final String pluginVersion = "v" + getDescription().getVersion();

    /**
     * This can be used to get the instance of the main class that is managed by itself.
     *
     * @return The managed instance of the main class.
     */
    public static MedievalFactions getInstance() {
        return instance;
    }

    /**
     * This runs when the server starts.
     */
    @Override
    public void onEnable() {
        instance = this;
        initializeConfig();
        load();
        scheduleRecurringTasks();
        registerEventHandlers();
        handleIntegrations();
        makeSureEveryPlayerExperiencesPowerDecay();
    }

    /**
     * This runs when the server stops.
     */
    @Override
    public void onDisable() {
        PersistentData.getInstance().getLocalStorageService().save();
    }

    /**
     * This method handles commands sent to the minecraft server and interprets them if the label matches one of the core commands.
     *
     * @param sender The sender of the command.
     * @param cmd    The command that was sent. This is unused.
     * @param label  The core command that has been invoked.
     * @param args   Arguments of the core command. Often sub-commands.
     * @return A boolean indicating whether the execution of the command was successful.
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        return LocalCommandService.getInstance().interpretCommand(sender, label, args);
    }

    /**
     * This can be used to get the version of the plugin.
     *
     * @return A string containing the version preceded by 'v'
     */
    public String getVersion() {
        return pluginVersion;
    }

    /**
     * Checks if the version is mismatched.
     *
     * @return A boolean indicating if the version is mismatched.
     */
    public boolean isVersionMismatched() {
        String configVersion = getConfig().getString("version");
        if (configVersion == null || this.getVersion() == null) {
            return true;
        } else {
            return !configVersion.equalsIgnoreCase(this.getVersion());
        }
    }

    /**
     * This can be utilized to access the external API of Medieval Factions.
     *
     * @return A reference to the external API.
     */
    public MedievalFactionsAPI getAPI() {
        return new MedievalFactionsAPI();
    }

    /**
     * Checks if debug is enabled.
     *
     * @return Whether debug is enabled.
     */
    public boolean isDebugEnabled() {
        return getConfig().getBoolean("debugMode");
    }

    private void makeSureEveryPlayerExperiencesPowerDecay() {
        PersistentData.getInstance().createActivityRecordForEveryOfflinePlayer();
    }

    /**
     * Creates or loads the config, depending on the situation.
     */
    private void initializeConfig() {
        if (configFileExists()) {
            performCompatibilityChecks();
        } else {
            LocalConfigService.getInstance().saveConfigDefaults();
        }
    }

    private void performCompatibilityChecks() {
        if (isVersionMismatched()) {
            LocalConfigService.getInstance().handleVersionMismatch();
        }
        reloadConfig();
    }

    private boolean configFileExists() {
        return new File("./plugins/MedievalFactions/config.yml").exists();
    }

    /**
     * Loads stored data into Persistent Data.
     */
    private void load() {
        LocalLocaleService.getInstance().loadStrings();
        PersistentData.getInstance().getLocalStorageService().load();
    }

    /**
     * Calls the Scheduler to schedule tasks that have to repeatedly be executed.
     */
    private void scheduleRecurringTasks() {
        Scheduler.getInstance().schedulePowerIncrease();
        Scheduler.getInstance().schedulePowerDecrease();
        Scheduler.getInstance().scheduleAutosave();
    }

    /**
     * Registers the event handlers of the plugin using Ponder.
     */
    private void registerEventHandlers() {
        ArrayList<Listener> listeners = initializeListeners();
        EventHandlerRegistry eventHandlerRegistry = new EventHandlerRegistry();
        eventHandlerRegistry.registerEventHandlers(listeners, this);
    }

    private ArrayList<Listener> initializeListeners() {
        return new ArrayList<>(Arrays.asList(
                new ChatHandler(),
                new DamageHandler(),
                new DeathHandler(),
                new EffectHandler(),
                new InteractionHandler(),
                new JoinHandler(),
                new MoveHandler(),
                new QuitHandler(),
                new SpawnHandler()
        ));
    }

    /**
     * Takes care of integrations for other plugins and tools.
     */
    private void handleIntegrations() {
        handlebStatsIntegration();
        handleDynmapIntegration();
        handlePlaceholdersIntegration();
    }

    private void handlebStatsIntegration() {
        int pluginId = 8929;
        new Metrics(this, pluginId);
    }

    private void handleDynmapIntegration() {
        if (DynmapIntegrator.hasDynmap()) {
            DynmapIntegrator.getInstance().scheduleClaimsUpdate(600); // Check once every 30 seconds for updates.
            DynmapIntegrator.getInstance().updateClaims();
        }
    }

    private void handlePlaceholdersIntegration() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderAPI().register();
        }
    }
}