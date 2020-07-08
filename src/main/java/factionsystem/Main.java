package factionsystem;

import factionsystem.Commands.*;
import factionsystem.EventHandlers.EntityDamageByEntityEventHandler;
import factionsystem.EventHandlers.PlayerInteractEventHandler;
import factionsystem.Objects.ClaimedChunk;
import factionsystem.Objects.Faction;
import factionsystem.Objects.LockedBlock;
import factionsystem.Objects.PlayerPowerRecord;
import factionsystem.Subsystems.StorageSubsystem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import static factionsystem.Utility.UtilityFunctions.*;

public class Main extends JavaPlugin implements Listener {

    public static String version = "v2.5";

    // subsysytems
    public StorageSubsystem storage = new StorageSubsystem(this);

    // saved lists
    public ArrayList<Faction> factions = new ArrayList<>();
    public ArrayList<ClaimedChunk> claimedChunks = new ArrayList<>();
    public ArrayList<PlayerPowerRecord> playerPowerRecords = new ArrayList<>();
    public ArrayList<LockedBlock> lockedBlocks = new ArrayList<>();

    // temporary lists
    public ArrayList<String> lockingPlayers = new ArrayList<>();
    public ArrayList<String> unlockingPlayers = new ArrayList<>();
    public HashMap<String, String> playersGrantingAccess = new HashMap<>();
    public ArrayList<String> playersCheckingAccess = new ArrayList<>();
    public HashMap<String, String> playersRevokingAccess = new HashMap<>();

    @Override
    public void onEnable() {
        System.out.println("Medieval Factions plugin enabling....");

        schedulePowerIncrease();
        scheduleAutosave();

        this.getServer().getPluginManager().registerEvents(this, this);

        storage.load();

        System.out.println("Medieval Factions plugin enabled.");
    }

    @Override
    public void onDisable(){
        System.out.println("Medieval Factions plugin disabling....");

        storage.save();

        System.out.println("Medieval Factions plugin disabled.");
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        // mf commands
        if (label.equalsIgnoreCase("mf")) {

            // no arguments check
            if (args.length == 0) {
                if (sender.hasPermission("mf.help") || sender.hasPermission("mf.default")) {
                    HelpCommand command = new HelpCommand();
                    command.sendHelpMessage(sender, args);
                }
                else {
                    sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.help'");
                }
            }

            // argument check
            if (args.length > 0) {

                // default commands ----------------------------------------------------------------------------------

                // help command
                if (args[0].equalsIgnoreCase("help")) {
                    if (sender.hasPermission("mf.help") || sender.hasPermission("mf.default")) {
                        HelpCommand command = new HelpCommand();
                        command.sendHelpMessage(sender, args);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.help'");
                    }
                }

                // create command
                if (args[0].equalsIgnoreCase("create") ) {
                    if (sender.hasPermission("mf.create")|| sender.hasPermission("mf.default")) {
                        CreateCommand command = new CreateCommand(this);
                        command.createFaction(sender, args);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.create'");
                    }
                }

                // list command
                if  (args[0].equalsIgnoreCase("list")) {
                    if (sender.hasPermission("mf.list") || sender.hasPermission("mf.default")) {
                        ListCommand command = new ListCommand(this);
                        command.listFactions(sender);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.list'");
                    }
                }

                // disband command
                if (args[0].equalsIgnoreCase("disband")) {
                    if (sender.hasPermission("mf.disband") || sender.hasPermission("mf.default")) {
                        DisbandCommand command = new DisbandCommand(this);
                        command.deleteFaction(sender);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.disband'");
                    }
                }

                // members command
                if (args[0].equalsIgnoreCase("members")) {
                    if (sender.hasPermission("mf.members") || sender.hasPermission("mf.default")) {
                        MembersCommand command = new MembersCommand(this);
                        command.showMembers(sender, args);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.members'");
                    }
                }

                // info command
                if (args[0].equalsIgnoreCase("info")) {
                    if (sender.hasPermission("mf.info") || sender.hasPermission("mf.default")) {
                        InfoCommand command = new InfoCommand(this);
                        command.showInfo(sender, args);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.info'");
                    }

                }

                // desc command
                if (args[0].equalsIgnoreCase("desc")) {
                    if (sender.hasPermission("mf.desc") || sender.hasPermission("mf.default")) {
                        DescCommand command = new DescCommand(this);
                        command.setDescription(sender, args);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.desc'");
                    }

                }

                // invite command
                if (args[0].equalsIgnoreCase("invite")) {
                    if (sender.hasPermission("mf.invite") || sender.hasPermission("mf.default")) {
                        InviteCommand command = new InviteCommand(this);
                        command.invitePlayer(sender, args);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.invite'");
                    }
                }

                // join command
                if (args[0].equalsIgnoreCase("join")) {
                    if (sender.hasPermission("mf.join") || sender.hasPermission("mf.default")) {
                        JoinCommand command = new JoinCommand(this);
                        command.joinFaction(sender, args);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.join'");
                    }
                }

                // kick command
                if (args[0].equalsIgnoreCase("kick")) {
                    if (sender.hasPermission("mf.kick") || sender.hasPermission("mf.default")) {
                        KickCommand command = new KickCommand(this);
                        command.kickPlayer(sender, args);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.kick'");
                    }
                }

                // leave commmand
                if (args[0].equalsIgnoreCase("leave")) {
                    if (sender.hasPermission("mf.leave") || sender.hasPermission("mf.default")) {
                        LeaveCommand command = new LeaveCommand(this);
                        command.leaveFaction(sender);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.leave'");
                    }
                }

                // transfer command
                if (args[0].equalsIgnoreCase("transfer")) {
                    if (sender.hasPermission("mf.transfer") || sender.hasPermission("mf.default")) {
                        TransferCommand command = new TransferCommand(this);
                        command.transferOwnership(sender, args);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.transfer'");
                    }
                }

                // declare war command
                if (args[0].equalsIgnoreCase("declarewar")) {
                    if (sender.hasPermission("mf.declarewar") || sender.hasPermission("mf.default")) {
                        DeclareWarCommand command = new DeclareWarCommand(this);
                        command.declareWar(sender, args);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.declarewar'");
                    }

                }

                // make peace command
                if (args[0].equalsIgnoreCase("makepeace")) {
                    if (sender.hasPermission("mf.makepeace") || sender.hasPermission("mf.default")) {
                        MakePeaceCommand command = new MakePeaceCommand(this);
                        command.makePeace(sender, args);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.makepeace'");
                    }
                }

                // claim command
                if (args[0].equalsIgnoreCase("claim")) {
                    if (sender.hasPermission("mf.claim") || sender.hasPermission("mf.default")) {
                        if (sender instanceof Player) {
                            Player player = (Player) sender;

                            // if not at demesne limit
                            if (isInFaction(player.getName(), factions)) {
                                Faction playersFaction = getPlayersFaction(player.getName(), factions);
                                if (getChunksClaimedByFaction(playersFaction.getName(), claimedChunks) < playersFaction.getCumulativePowerLevel()) {
                                    addChunkAtPlayerLocation(player);
                                    return true;
                                }
                                else {
                                    player.sendMessage(ChatColor.RED + "You have reached your demesne limit! Invite more players to increase this.");
                                    return false;
                                }
                            }
                            else {
                                player.sendMessage(ChatColor.RED + "You must be in a faction to use this command.");
                                return false;
                            }
                        }
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.claim'");
                    }
                }

                // unclaim command
                if (args[0].equalsIgnoreCase("unclaim")) {
                    if (sender.hasPermission("mf.unclaim") || sender.hasPermission("mf.default")) {
                        if (sender instanceof Player) {
                            Player player = (Player) sender;
                            if (isInFaction(player.getName(), factions)) {
                                removeChunkAtPlayerLocation(player);
                            }
                            else {
                                player.sendMessage(ChatColor.RED + "You need to be in a faction to use this command.");
                            }

                        }
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.unclaim'");
                    }
                }

                // unclaimall command
                if (args[0].equalsIgnoreCase("unclaimall")) {
                    if (sender.hasPermission("mf.unclaimall") || sender.hasPermission("mf.default")) {
                        if (sender instanceof Player) {
                            Player player = (Player) sender;
                            for (Faction faction : factions) {
                                if (faction.isOwner(player.getName())) {
                                    // remove faction home
                                    faction.setFactionHome(null);
                                    sendAllPlayersInFactionMessage(faction, ChatColor.RED + "Your faction home has been removed!");

                                    // remove claimed chunks
                                    removeAllClaimedChunks(faction.getName(), claimedChunks);
                                    player.sendMessage(ChatColor.GREEN + "All land unclaimed.");
                                }
                            }
                        }
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.unclaimall'");
                    }
                }

                // checkclaim command
                if (args[0].equalsIgnoreCase("checkclaim")) {
                    if (sender.hasPermission("mf.unclaimall") || sender.hasPermission("mf.default")) {
                        if (sender instanceof Player) {
                            Player player = (Player) sender;
                            String result = checkOwnershipAtPlayerLocation(player);
                            if (result.equalsIgnoreCase("unclaimed")) {
                                player.sendMessage(ChatColor.GREEN + "This land is unclaimed.");
                            }
                            else {
                                player.sendMessage(ChatColor.RED + "This land is claimed by " + result + ".");
                            }
                        }
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.unclaimall'");
                    }
                }

                // autoclaim command
                if (args[0].equalsIgnoreCase("autoclaim")) {
                    if (sender.hasPermission("mf.autoclaim") || sender.hasPermission("mf.default")) {
                        if (sender instanceof Player) {
                            Player player = (Player) sender;

                            if (isInFaction(player.getName(), factions)) {
                                boolean owner = false;
                                for (Faction faction : factions) {
                                    if (faction.isOwner(player.getName())) {
                                        owner = true;
                                        faction.toggleAutoClaim();
                                        player.sendMessage(ChatColor.AQUA + "Autoclaim toggled.");
                                    }

                                }
                                if (!owner) {
                                    player.sendMessage(ChatColor.RED + "You must be the owner to use this command.");
                                }
                            }
                            else {
                                player.sendMessage(ChatColor.RED + "You need to be in a faction to use this command.");
                                return false;
                            }

                        }
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.autoclaim'");
                    }
                }

                // promote command
                if (args[0].equalsIgnoreCase("promote")) {
                    if (sender.hasPermission("mf.promote") || sender.hasPermission("mf.default")) {
                        PromoteCommand command = new PromoteCommand(this);
                        command.promotePlayer(sender, args);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.promote'");
                    }
                }

                // demote command
                if (args[0].equalsIgnoreCase("demote")) {
                    if (sender.hasPermission("mf.demote")) {
                        DemoteCommand command = new DemoteCommand(this);
                        command.demotePlayer(sender, args);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.demote'");
                    }
                }

                // power command
                if  (args[0].equalsIgnoreCase("power")) {
                    if (sender.hasPermission("mf.power") || sender.hasPermission("mf.default")) {
                        PowerCommand command = new PowerCommand(this);
                        command.powerCheck(sender);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.power'");
                    }

                }

                // sethome command
                if (args[0].equalsIgnoreCase("sethome")) {
                    if (sender.hasPermission("mf.sethome") || sender.hasPermission("mf.default")) {
                        SetHomeCommand command = new SetHomeCommand(this);
                        command.setHome(sender);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.sethome'");
                    }
                }

                // home command
                if (args[0].equalsIgnoreCase("home")) {
                    if (sender.hasPermission("mf.home") || sender.hasPermission("mf.default")) {
                        HomeCommand command = new HomeCommand(this);
                        command.teleportPlayer(sender);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.home'");
                    }
                }

                // version command
                if (args[0].equalsIgnoreCase("version")) {
                    if (sender.hasPermission("mf.version") || sender.hasPermission("mf.default")) {
                        sender.sendMessage(ChatColor.AQUA + "Medieval-Factions-" + version);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.version'");
                    }

                }

                // who command
                if (args[0].equalsIgnoreCase("who")) {
                    if (sender.hasPermission("mf.who") || sender.hasPermission("mf.default")) {
                        WhoCommand command = new WhoCommand(this);
                        command.sendInformation(sender, args);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.who'");
                    }

                }

                // ally command
                if (args[0].equalsIgnoreCase("ally")) {
                    if (sender.hasPermission("mf.ally") || sender.hasPermission("mf.default")) {
                        AllyCommand command = new AllyCommand(this);
                        command.requestAlliance(sender, args);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.ally'");
                    }

                }

                // breakalliance command
                if (args[0].equalsIgnoreCase("breakalliance")) {
                    if (sender.hasPermission("mf.breakalliance") || sender.hasPermission("mf.default")) {
                        BreakAllianceCommand command = new BreakAllianceCommand(this);
                        command.breakAlliance(sender, args);
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.breakalliance'");
                    }
                }

                // TODO: shift responsibility of perm checking from Main to the Command class of each command, like below

                // rename command
                if (args[0].equalsIgnoreCase("rename")) {
                    RenameCommand command = new RenameCommand(this);
                    command.renameFaction(sender, args);
                }

                // lock command
                if (args[0].equalsIgnoreCase("lock")) {
                    LockCommand command = new LockCommand(this);
                    command.lockBlock(sender, args);
                }

                // unlock command
                if (args[0].equalsIgnoreCase("unlock")) {
                    UnlockCommand command = new UnlockCommand(this);
                    command.unlockBlock(sender, args);
                }

                // grantaccess command
                if (args[0].equalsIgnoreCase("grantaccess")) {
                    GrantAccessCommand command = new GrantAccessCommand(this);
                    command.grantAccess(sender, args);
                }

                // checkaccess command
                if (args[0].equalsIgnoreCase("checkaccess")) {
                    CheckAccessCommand command = new CheckAccessCommand(this);
                    command.checkAccess(sender, args);
                }

                // admin commands ----------------------------------------------------------------------------------

                // forcesave command
                if (args[0].equalsIgnoreCase("forcesave")) {
                    if (sender.hasPermission("mf.forcesave") || sender.hasPermission("mf.admin")) {
                        sender.sendMessage(ChatColor.GREEN + "Medieval Factions plugin is saving...");
                        storage.save();
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.forcesave'");
                    }
                }

                // forceload command
                if (args[0].equalsIgnoreCase("forceload")) {
                    if (sender.hasPermission("mf.forceload") || sender.hasPermission("mf.admin")) {
                        sender.sendMessage(ChatColor.GREEN + "Medieval Factions plugin is loading...");
                        storage.load();
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.forceload'");
                    }
                }

                // reset power levels command
                if (args[0].equalsIgnoreCase("resetpowerlevels")) {
                    if (sender.hasPermission("mf.resetpowerlevels") || sender.hasPermission("mf.admin")) {
                        sender.sendMessage(ChatColor.GREEN + "Power level resetting...");
                        resetPowerRecords();
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.resetpowerlevels'");
                    }
                }

            }
        }
        return false;
    }

    @EventHandler()
    public void onDamage(EntityDamageByEntityEvent event) {
        EntityDamageByEntityEventHandler handler = new EntityDamageByEntityEventHandler(this);
        handler.handle(event);
    }

    public void addChunkAtPlayerLocation(Player player) {
        double[] playerCoords = new double[2];
        playerCoords[0] = player.getLocation().getChunk().getX();
        playerCoords[1] = player.getLocation().getChunk().getZ();
        for (Faction faction : factions) {
            if (faction.isOwner(player.getName()) || faction.isOfficer(player.getName())) {

                // check if land is already claimed
                for (ClaimedChunk chunk : claimedChunks) {
                    if (playerCoords[0] == chunk.getCoordinates()[0] && playerCoords[1] == chunk.getCoordinates()[1]) {

                        // if holder is player's faction
                        if (chunk.getHolder().equalsIgnoreCase(faction.getName()) && getPlayersFaction(player.getName(), factions).getAutoClaimStatus() == false) {
                            player.sendMessage(ChatColor.RED + "This land is already claimed by your faction!");
                            return;
                        }
                        else {

                            // check if faction has more land than their demesne limit
                            for (Faction targetFaction : factions) {
                                if (chunk.getHolder().equalsIgnoreCase(targetFaction.getName())) {
                                    if (targetFaction.getCumulativePowerLevel() < getChunksClaimedByFaction(targetFaction.getName(), claimedChunks)) {

                                        // is at war with target faction
                                        if (faction.isEnemy(targetFaction.getName())) {
                                            claimedChunks.remove(chunk);

                                            ClaimedChunk newChunk = new ClaimedChunk(player.getLocation().getChunk());
                                            newChunk.setHolder(faction.getName());
                                            newChunk.setWorld(player.getLocation().getWorld().getName());
                                            claimedChunks.add(newChunk);
                                            player.sendMessage(ChatColor.GREEN + "Land conquered from " + targetFaction.getName() + "! Demesne Size: " + getChunksClaimedByFaction(faction.getName(), claimedChunks) + "/" + faction.getCumulativePowerLevel());

                                            sendAllPlayersInFactionMessage(targetFaction, ChatColor.RED + getPlayersFaction(player.getName(), factions).getName() + " has conquered land from your faction!");

                                            return;
                                        }
                                        else {
                                            player.sendMessage(ChatColor.RED + "Your factions have to be at war in order for you to conquer land.");
                                            return;
                                        }
                                    }
                                }
                            }

                            if (getPlayersFaction(player.getName(), factions).getAutoClaimStatus() == false) {
                                player.sendMessage(ChatColor.RED + "This land is already claimed by " + chunk.getHolder());
                            }

                            return;
                        }
                    }
                }

                ClaimedChunk newChunk = new ClaimedChunk(player.getLocation().getChunk());
                newChunk.setHolder(faction.getName());
                newChunk.setWorld(player.getLocation().getWorld().getName());
                claimedChunks.add(newChunk);
                player.sendMessage(ChatColor.GREEN + "Land claimed! Demesne Size: " + getChunksClaimedByFaction(faction.getName(), claimedChunks) + "/" + faction.getCumulativePowerLevel());
                return;
            }
        }
    }

    public void removeChunkAtPlayerLocation(Player player) {
        double[] playerCoords = new double[2];
        playerCoords[0] = player.getLocation().getChunk().getX();
        playerCoords[1] = player.getLocation().getChunk().getZ();
        for (Faction faction : factions) {
            if (faction.isOwner(player.getName()) || faction.isOfficer(player.getName())) {

                // check if land is claimed by player's faction
                for (ClaimedChunk chunk : claimedChunks) {
                    if (playerCoords[0] == chunk.getCoordinates()[0] && playerCoords[1] == chunk.getCoordinates()[1]) {
                        // if holder is player's faction
                        if (chunk.getHolder().equalsIgnoreCase(faction.getName())) {

                            String identifier = (int)chunk.getChunk().getX() + "_" + (int)chunk.getChunk().getZ();

                            // delete file associated with chunk
                            System.out.println("Attempting to delete file plugins plugins/medievalfactions/claimedchunks/" + identifier + ".txt");
                            try {
                                File fileToDelete = new File("plugins/medievalfactions/claimedchunks/" + identifier + ".txt");
                                if (fileToDelete.delete()) {
                                    System.out.println("Success. File deleted.");
                                }
                                else {
                                    System.out.println("There was a problem deleting the file.");
                                }
                            } catch(Exception e) {
                                System.out.println("There was a problem encountered during file deletion.");
                            }

                            // if faction home is located on this chunk
                            Location factionHome = getPlayersFaction(player.getName(), factions).getFactionHome();
                            if (factionHome != null) {
                                if (factionHome.getChunk().getX() == chunk.getChunk().getX() && factionHome.getChunk().getZ() == chunk.getChunk().getZ()) {

                                    // remove faction home
                                    faction.setFactionHome(null);
                                    sendAllPlayersInFactionMessage(faction, ChatColor.RED + "Your faction home has been removed!");

                                }
                            }

                            claimedChunks.remove(chunk);
                            player.sendMessage(ChatColor.GREEN + "Land unclaimed.");

                            return;
                        }
                        else {
                            player.sendMessage(ChatColor.RED + "This land is claimed by " + chunk.getHolder());
                            return;
                        }
                    }
                }

            }
        }
    }

    public String checkOwnershipAtPlayerLocation(Player player) {
        double[] playerCoords = new double[2];
        playerCoords[0] = player.getLocation().getChunk().getX();
        playerCoords[1] = player.getLocation().getChunk().getZ();
        System.out.println("Checking if chunk at location of player " + player.getName() + " is claimed.");
        for (ClaimedChunk chunk : claimedChunks) {
//            System.out.println("Comparing player coords " + playerCoords[0] + ", " + playerCoords[1] + " to chunk coords " + chunk.getCoordinates()[0] + ", " + chunk.getCoordinates()[1]);
            if (playerCoords[0] == chunk.getCoordinates()[0] && playerCoords[1] == chunk.getCoordinates()[1]) {
                System.out.println("Match!");
                return chunk.getHolder();
            }
        }
        System.out.println("No match found.");
        return "unclaimed";
    }

    @EventHandler()
    public void onPlayerMove(PlayerMoveEvent event) {
        // Full disclosure, I feel like this method might be extremely laggy, especially if a player is travelling.
        // May have to optimise this, or just not have this mechanic.
        // - Dan

        // if player enters a new chunk
        if (event.getFrom().getChunk() != Objects.requireNonNull(event.getTo()).getChunk()) {

            // auto claim check
            for (Faction faction : factions) {
                if (faction.isOwner(event.getPlayer().getName())) {

                    if (faction.getAutoClaimStatus()) {

                        // if not at demesne limit
                        Faction playersFaction = getPlayersFaction(event.getPlayer().getName(), factions);
                        if (getChunksClaimedByFaction(playersFaction.getName(), claimedChunks) < playersFaction.getCumulativePowerLevel()) {
                            int seconds = 1;
                            getServer().getScheduler().runTaskLater(this, new Runnable() {
                                @Override
                                public void run() {
                                    // add new chunk to claimed chunks
                                    addChunkAtPlayerLocation(event.getPlayer());
                                }
                            }, seconds * 20);
                        }
                        else {
                            event.getPlayer().sendMessage(ChatColor.RED + "You have reached your demesne limit! Invite more players to increase this.");
                        }
                    }
                }
            }


            // if new chunk is claimed and old chunk was not
            if (isClaimed(event.getTo().getChunk(), claimedChunks) && !isClaimed(event.getFrom().getChunk(), claimedChunks)) {
                event.getPlayer().sendMessage(ChatColor.GREEN + "Entering the territory of " + getClaimedChunk(event.getTo().getChunk().getX(), event.getTo().getChunk().getZ(), claimedChunks).getHolder());
                return;
            }

            // if new chunk is unclaimed and old chunk was not
            if (!isClaimed(event.getTo().getChunk(), claimedChunks) && isClaimed(event.getFrom().getChunk(), claimedChunks)) {
                event.getPlayer().sendMessage(ChatColor.GREEN + "Entering the wilderness");
                return;
            }


            // if new chunk is claimed and old chunk was also claimed
            if (isClaimed(event.getTo().getChunk(), claimedChunks) && isClaimed(event.getFrom().getChunk(), claimedChunks)) {
                // if chunk holders are not equal
                if (!(getClaimedChunk(event.getFrom().getChunk().getX(), event.getFrom().getChunk().getZ(), claimedChunks).getHolder().equalsIgnoreCase(getClaimedChunk(event.getTo().getChunk().getX(), event.getTo().getChunk().getZ(), claimedChunks).getHolder()))) {
                    event.getPlayer().sendMessage(ChatColor.GREEN + "Leaving the territory of " + getClaimedChunk(event.getFrom().getChunk().getX(), event.getFrom().getChunk().getZ(), claimedChunks).getHolder());
                    event.getPlayer().sendMessage(ChatColor.GREEN + "Entering the territory of " + getClaimedChunk(event.getTo().getChunk().getX(), event.getTo().getChunk().getZ(), claimedChunks).getHolder());
                }
            }

        }

    }

    // the following two event handlers are identical except in their event types
    // might have to fix this duplication later

    @EventHandler()
    public void onBlockBreak(BlockBreakEvent event) {
        // get player
        Player player = event.getPlayer();

        // get chunk
        ClaimedChunk chunk = getClaimedChunk(event.getBlock().getLocation().getChunk().getX(), event.getBlock().getLocation().getChunk().getZ(), claimedChunks);

        // if chunk is claimed
        if (chunk != null) {

            // player not in a faction
            if (!isInFaction(event.getPlayer().getName(), factions)) {
                event.setCancelled(true);
            }

            // if player is in faction
            for (Faction faction : factions) {
                if (faction.isMember(player.getName())) {

                    // if player's faction is not the same as the holder of the chunk
                    if (!(faction.getName().equalsIgnoreCase(chunk.getHolder()))) {
                        event.setCancelled(true);
                        return;
                    }

                    // if block is locked
                    if (isBlockLocked(event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ())) {

                        // if player is not the owner
                        if (!getLockedBlock(event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ()).getOwner().equalsIgnoreCase(player.getName())) {
                            event.setCancelled(true);
                            player.sendMessage(ChatColor.RED + "You don't own this!");
                            return;
                        }
                    }
                }
            }
        }
    }

    @EventHandler()
    public void onBlockPlace(BlockPlaceEvent event) {
        // get player
        Player player = event.getPlayer();

        // get chunk
        ClaimedChunk chunk = getClaimedChunk(event.getBlock().getLocation().getChunk().getX(), event.getBlock().getLocation().getChunk().getZ(), claimedChunks);

        // if chunk is claimed
        if (chunk != null) {

            // player not in a faction
            if (!isInFaction(event.getPlayer().getName(), factions)) {
                event.setCancelled(true);
            }

            // if player is in faction
            for (Faction faction : factions) {
                if (faction.isMember(player.getName())) {

                    // if player's faction is not the same as the holder of the chunk
                    if (!(faction.getName().equalsIgnoreCase(chunk.getHolder()))) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler()
    public void onRightClick(PlayerInteractEvent event) {
        PlayerInteractEventHandler handler = new PlayerInteractEventHandler(this);
        handler.handle(event);
    }

    public void removeLock(Block block) {
        for (LockedBlock b : lockedBlocks) {
            if (b.getX() == block.getX() && b.getY() == block.getY() && b.getZ() == block.getZ()) {
                lockedBlocks.remove(b);
                return;
            }
        }
    }

    public boolean isDoor(Block block) {
        if (block.getType() == Material.ACACIA_DOOR ||
                block.getType() == Material.BIRCH_DOOR ||
                block.getType() == Material.DARK_OAK_DOOR ||
                block.getType() == Material.IRON_DOOR ||
                block.getType() == Material.JUNGLE_DOOR ||
                block.getType() == Material.OAK_DOOR ||
                block.getType() == Material.ACACIA_DOOR) {

            return true;

        }
        return false;
    }

    public boolean isChest(Block block) {
        if (block.getType() == Material.CHEST) {
            return true;
        }
        return false;
    }

    public boolean hasPowerRecord(String playerName) {
        for (PlayerPowerRecord record : playerPowerRecords) {
            if (record.getPlayerName().equalsIgnoreCase(playerName)) {
                return true;
            }
        }
        return false;
    }

    @EventHandler()
    public void onJoin(PlayerJoinEvent event) {
        if (!hasPowerRecord(event.getPlayer().getName())) {
            PlayerPowerRecord newRecord = new PlayerPowerRecord(event.getPlayer().getName());

            playerPowerRecords.add(newRecord);
        }
    }

    @EventHandler()
    public void onDeath(PlayerDeathEvent event) {
        event.getEntity();
        Player player = (Player) event.getEntity();

        // decrease dying player's power
        for (PlayerPowerRecord record : playerPowerRecords) {
            if (record.getPlayerName().equalsIgnoreCase(player.getName())) {
                record.decreasePower();
                if (getPlayersPowerRecord(player.getName(), playerPowerRecords).getPowerLevel() > 0) {
                    player.sendMessage(ChatColor.RED + "Your power level has decreased!");
                }
            }
        }

        // if player's cause of death was another player killing them
        if (player.getKiller() instanceof Player) {
            Player killer = (Player) player.getKiller();
            System.out.println(player.getName() + " has killed " + killer.getName());

            for (PlayerPowerRecord record : playerPowerRecords) {
                if (record.getPlayerName().equalsIgnoreCase(killer.getName())) {
                    record.increasePower();
                    if (getPlayersPowerRecord(killer.getName(), playerPowerRecords).getPowerLevel() < 20) {
                        killer.sendMessage(ChatColor.GREEN + "Your power level has increased!");
                    }
                }
            }

            // add power to killer's faction
            if (isInFaction(killer.getName(), factions)) {
                if (getPlayersPowerRecord(killer.getName(), playerPowerRecords).getPowerLevel() < 20) {
                    getPlayersFaction(killer.getName(), factions).addPower();
                }
            }
        }

        // decrease power from player's faction
        if (isInFaction(player.getName(), factions)) {
            if (getPlayersPowerRecord(player.getName(), playerPowerRecords).getPowerLevel() > 0) {
                getPlayersFaction(player.getName(), factions).subtractPower();
            }
        }
    }

    public void schedulePowerIncrease() {
        System.out.println("Scheduling hourly power increase...");
        int delay = 30 * 60; // 30 minutes
        int secondsUntilRepeat = 60 * 60; // 1 hour
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                System.out.println("Medieval Factions is increasing the power of every player by 1 if their power is below 10. This will happen hourly.");
                for (PlayerPowerRecord powerRecord : playerPowerRecords) {
                    try {
                        if (powerRecord.getPowerLevel() < 20) {
                            if (Bukkit.getServer().getPlayer(powerRecord.getPlayerName()).isOnline()) {
                                powerRecord.increasePower();
                                if (isInFaction(powerRecord.getPlayerName(), factions)) {
                                    getPlayersFaction(powerRecord.getPlayerName(), factions).addPower();
                                }
                                Bukkit.getServer().getPlayer(powerRecord.getPlayerName()).sendMessage(ChatColor.GREEN + "You feel stronger. Your power has increased.");
                            }
                        }
                    } catch (Exception ignored) {
                        // player offline
                    }
                }
            }
        }, delay * 20, secondsUntilRepeat * 20);
    }

    public void scheduleAutosave() {
        System.out.println("Scheduling hourly auto save...");
        int delay = 60 * 60; // 1 hour
        int secondsUntilRepeat = 60 * 60; // 1 hour
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                System.out.println("Medieval Factions is saving. This will happen every hour.");
                storage.save();
            }
        }, delay * 20, secondsUntilRepeat * 20);
    }

    public void resetPowerRecords() {
        // reset individual records
        System.out.println("Resetting individual power records.");
        for (PlayerPowerRecord record : playerPowerRecords) {
            record.setPowerLevel(10);
        }

        // reset faction cumulative power levels
        System.out.println("Resetting faction cumulative power records.");
        for (Faction faction : factions) {
            int sum = 0;
            for (String playerName : faction.getMemberArrayList()) {
                sum = sum + getPlayersPowerRecord(playerName, playerPowerRecords).getPowerLevel();
            }
            faction.setCumulativePowerLevel(sum);
        }

    }

    public boolean isBlockLocked(int x, int y, int z) {
        for (LockedBlock block : lockedBlocks) {
            if (block.getX() == x && block.getY() == y && block.getZ() == z) {
                return true;
            }
        }
        return false;
    }

    public LockedBlock getLockedBlock(int x, int y, int z) {
        for (LockedBlock block : lockedBlocks) {
            if (block.getX() == x && block.getY() == y && block.getZ() == z) {
                return block;
            }
        }
        return null;
    }
}