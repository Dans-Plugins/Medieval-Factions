/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import dansplugins.factionsystem.services.*;
import dansplugins.factionsystem.utils.*;
import dansplugins.factionsystem.utils.extended.Messenger;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import dansplugins.factionsystem.data.EphemeralData;
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
import dansplugins.factionsystem.factories.WarFactory;
import dansplugins.factionsystem.integrators.CurrenciesIntegrator;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.integrators.FiefsIntegrator;
import dansplugins.factionsystem.placeholders.PlaceholderAPI;
import dansplugins.factionsystem.utils.extended.BlockChecker;
import dansplugins.factionsystem.utils.extended.Scheduler;
import preponderous.ponder.minecraft.bukkit.abs.PonderBukkitPlugin;
import preponderous.ponder.minecraft.bukkit.tools.EventHandlerRegistry;

/**
 * @author Daniel McCoy Stephenson
 * @since May 30th, 2020
 */
public class MedievalFactions extends PonderBukkitPlugin {
    private final String pluginVersion = "v" + getDescription().getVersion();

    private final ActionBarService actionBarService = new ActionBarService(this);
    private final ConfigService configService = new ConfigService();
    private final LocaleService localeService = new LocaleService(this, configService);
    private final EphemeralData ephemeralData = new EphemeralData();
    private final Messenger messenger = new Messenger();
    private final DynmapIntegrator dynmapIntegrator = new DynmapIntegrator();
    private final BlockChecker blockChecker = new BlockChecker();
    private final Logger logger = new Logger();
    private final WarFactory warFactory = new WarFactory();
    private final PersistentData persistentData = new PersistentData(localeService, configService, this, messenger, dynmapIntegrator, ephemeralData, blockChecker, interactionAccessChecker, logger);
    private final RelationChecker relationChecker = new RelationChecker(persistentData);
    private final PlayerTeleporter playerTeleporter = new PlayerTeleporter();
    private final Scheduler scheduler = new Scheduler(logger, localeService, this, persistentData, configService, playerTeleporter);
    private final CommandService commandService = new CommandService(localeService, this, configService, persistentData, ephemeralData, persistentData.getChunkDataAccessor(), dynmapIntegrator, warFactory, logger, scheduler, messenger, relationChecker);
    private final InteractionAccessChecker interactionAccessChecker = new InteractionAccessChecker(persistentData, configService, ephemeralData, logger);
    private final GateService gateService = new GateService(persistentData, localeService, ephemeralData);
    private final LockService lockService = new LockService();
    private final TerritoryOwnerNotifier territoryOwnerNotifier = new TerritoryOwnerNotifier(localeService, configService, actionBarService);
    private final CurrenciesIntegrator currenciesIntegrator = new CurrenciesIntegrator();
    private final FiefsIntegrator fiefsIntegrator = new FiefsIntegrator();

    /**
     * This runs when the server starts.
     */
    @Override
    public void onEnable() {
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
        persistentData.getLocalStorageService().save();
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
        return commandService.interpretCommand(sender, label, args);
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
        persistentData.createActivityRecordForEveryOfflinePlayer();
    }

    /**
     * Creates or loads the config, depending on the situation.
     */
    private void initializeConfig() {
        if (configFileExists()) {
            performCompatibilityChecks();
        } else {
            configService.saveConfigDefaults();
        }
    }

    private void performCompatibilityChecks() {
        if (isVersionMismatched()) {
            configService.handleVersionMismatch();
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
        localeService.loadStrings();
        persistentData.getLocalStorageService().load();
    }

    /**
     * Calls the Scheduler to schedule tasks that have to repeatedly be executed.
     */
    private void scheduleRecurringTasks() {
        scheduler.schedulePowerIncrease();
        scheduler.schedulePowerDecrease();
        scheduler.scheduleAutosave();
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
                new ChatHandler(persistentData, configService, ephemeralData, messenger),
                new DamageHandler(logger, persistentData, ephemeralData, localeService, configService, relationChecker),
                new DeathHandler(configService, persistentData, localeService),
                new EffectHandler(ephemeralData, this, relationChecker),
                new InteractionHandler(persistentData, interactionAccessChecker, localeService, blockChecker, this, lockService, ephemeralData, gateService),
                new JoinHandler(persistentData, localeService, configService, logger, messenger, territoryOwnerNotifier),
                new MoveHandler(persistentData, territoryOwnerNotifier, localeService, this, dynmapIntegrator),
                new QuitHandler(ephemeralData, persistentData, actionBarService),
                new SpawnHandler(configService, persistentData)
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
            dynmapIntegrator.scheduleClaimsUpdate(600); // Check once every 30 seconds for updates.
            dynmapIntegrator.updateClaims();
        }
    }

    private void handlePlaceholdersIntegration() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderAPI().register();
        }
    }
}