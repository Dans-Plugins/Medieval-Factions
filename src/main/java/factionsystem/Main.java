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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class Main extends JavaPlugin implements Listener {

    // version
    public static String version = "v3.6.0.2";

    // subsystems
    public StorageSubsystem storage = new StorageSubsystem(this);
    public UtilitySubsystem utilities = new UtilitySubsystem(this);
    public ConfigSubsystem config = new ConfigSubsystem(this);

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

    @Override
    public void onEnable() {
        System.out.println("Medieval Factions plugin enabling....");

        utilities.ensureSmoothTransitionBetweenVersions();

        // config creation/loading
        if (!(new File("./plugins/MedievalFactions/config.yml").exists())) {
            config.saveConfigDefaults();
        }
        else {
            // pre load compatibility checks
            if (!getConfig().getString("version").equalsIgnoreCase(Main.version)) {
                System.out.println("[ALERT] Version mismatch! Adding missing defaults and setting version!");
                config.handleVersionMismatch();
            }
            reloadConfig();
        }

        utilities.schedulePowerIncrease();
        utilities.schedulePowerDecrease();
        utilities.scheduleAutosave();
        this.getServer().getPluginManager().registerEvents(this, this);
        storage.load();

        // post load compatibility checks
        if (!getConfig().getString("version").equalsIgnoreCase(Main.version)) {
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
        CommandSubsystem commandInterpreter = new CommandSubsystem(this);
        return commandInterpreter.interpretCommand(sender, label, args);
    }

    @EventHandler()
    public void onDamage(EntityDamageByEntityEvent event) {
        EntityDamageByEntityEventHandler handler = new EntityDamageByEntityEventHandler(this);
        handler.handle(event);
    }

    @EventHandler()
    public void onPlayerMove(PlayerMoveEvent event) {
        PlayerMoveEventHandler handler = new PlayerMoveEventHandler(this);
        handler.handle(event);
    }

    @EventHandler()
    public void onBlockBreak(BlockBreakEvent event) {
        BlockBreakEventHandler handler = new BlockBreakEventHandler(this);
        handler.handle(event);
    }

    @EventHandler()
    public void onBlockPlace(BlockPlaceEvent event) {
        BlockPlaceEventHandler handler = new BlockPlaceEventHandler(this);
        handler.handle(event);
    }

    @EventHandler()
    public void onRightClick(PlayerInteractEvent event) {
        PlayerInteractEventHandler handler = new PlayerInteractEventHandler(this);
        handler.handle(event);
    }

    @EventHandler()
    public void onDeath(PlayerDeathEvent event) {
        PlayerDeathEventHandler handler = new PlayerDeathEventHandler(this);
        handler.handle(event);
    }

    @EventHandler()
    public void onJoin(PlayerJoinEvent event) {
        PlayerJoinEventHandler handler = new PlayerJoinEventHandler(this);
        handler.handle(event);
    }
    
    @EventHandler()
    public void onLeave(PlayerQuitEvent event)
    {
    	PlayerLeaveEventHandler handler = new PlayerLeaveEventHandler(this);
    	handler.handle(event);
    }

    @EventHandler()
    public void onJoin(EntitySpawnEvent event) {
        EntitySpawnEventHandler handler = new EntitySpawnEventHandler(this);
        handler.handle(event);
    }

    @EventHandler()
    public void onChat(AsyncPlayerChatEvent event) {
        AsyncPlayerChatEventHandler handler = new AsyncPlayerChatEventHandler(this);
        handler.handle(event);
    }

    @EventHandler
    public void onPotionSplash(PotionSplashEvent event) {
        PotionSplashEventHandler handler = new PotionSplashEventHandler(this);
        handler.handle(event);
    }

    @EventHandler
    public void onLingeringPotionSplash(LingeringPotionSplashEvent event) {
        LingeringPotionSplashEventHandler handler = new LingeringPotionSplashEventHandler(this);
        handler.handle(event);
    }

    @EventHandler
    public void onAreaOfEffectCloudApply(AreaEffectCloudApplyEvent event){
        AreaEffectCloudApplyEventHandler handler = new AreaEffectCloudApplyEventHandler(this);
        handler.handle(event);
    }

}
