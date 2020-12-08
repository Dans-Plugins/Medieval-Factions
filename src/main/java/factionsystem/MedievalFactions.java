package factionsystem;

import factionsystem.EventHandlers.*;
import factionsystem.Objects.ClaimedChunk;
import factionsystem.Objects.Duel;
import factionsystem.Objects.Faction;
import factionsystem.Objects.Gate;
import factionsystem.Objects.LockedBlock;
import factionsystem.Objects.PlayerActivityRecord;
import factionsystem.Objects.PlayerPowerRecord;
import factionsystem.Subsystems.CommandSubsystem;
import factionsystem.Subsystems.ConfigSubsystem;
import factionsystem.Subsystems.StorageSubsystem;
import factionsystem.Subsystems.UtilitySubsystem;
import factionsystem.Util.Pair;
import factionsystem.bStats.Metrics;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class MedievalFactions extends JavaPlugin {

    // instance
    private static MedievalFactions instance;

    // version
    public static String version = "v3.6.0.3-beta-4";

    // scheduler
    public Scheduler scheduler = new Scheduler();

    // subsystems
    public StorageSubsystem storage = new StorageSubsystem();
    public UtilitySubsystem utilities = new UtilitySubsystem();
    public ConfigSubsystem config = new ConfigSubsystem();

    // saved lists
    public ArrayList<Faction> factions = new ArrayList<>();
    public ArrayList<ClaimedChunk> claimedChunks = new ArrayList<>();
    public ArrayList<PlayerPowerRecord> playerPowerRecords = new ArrayList<>();
    public ArrayList<PlayerActivityRecord> playerActivityRecords = new ArrayList<>();
    public ArrayList<LockedBlock> lockedBlocks = new ArrayList<>();
    public ArrayList<Duel> duelingPlayers = new ArrayList<Duel>();

    // temporary lists
    public HashMap<UUID, Gate> creatingGatePlayers = new HashMap<>(); 
    public ArrayList<UUID> lockingPlayers = new ArrayList<>();
    public ArrayList<UUID> unlockingPlayers = new ArrayList<>();
    // Left user granting access, right user receiving access;
    public HashMap<UUID, UUID> playersGrantingAccess = new HashMap<>();
    public ArrayList<UUID> playersCheckingAccess = new ArrayList<>();
    // Left user granting access, right user receiving access;
    public HashMap<UUID, UUID> playersRevokingAccess = new HashMap<>();
    public ArrayList<UUID> playersInFactionChat = new ArrayList<>();
    public ArrayList<UUID> adminsBypassingProtections = new ArrayList<>();
    // List of players who made the cloud and the cloud itself in a pair
    public ArrayList<Pair<Player, AreaEffectCloud>> activeAOEClouds = new ArrayList<>();

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

        // blocks and interaction
        this.getServer().getPluginManager().registerEvents(new BlockInteractionHandler(), this);

        // joining, leaving and spawning
        this.getServer().getPluginManager().registerEvents(new JoiningLeavingAndSpawningHandler(), this);

        // damage, effects and death
        this.getServer().getPluginManager().registerEvents(new DamageEffectsAndDeathHandler(), this);

        // movement
        this.getServer().getPluginManager().registerEvents(new MoveHandler(), this);

        // chat
        this.getServer().getPluginManager().registerEvents(new ChatHandler(), this);

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
