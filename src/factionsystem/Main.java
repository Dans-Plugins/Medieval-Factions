package factionsystem;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import factionsystem.Commands.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import java.util.Scanner;

public class Main extends JavaPlugin implements Listener {

    ArrayList<Faction> factions = new ArrayList<>();
    ArrayList<ClaimedChunk> claimedChunks = new ArrayList<>();
    ArrayList<PlayerPowerRecord> playerPowerRecords = new ArrayList<>();

    @Override
    public void onEnable() {
        System.out.println("Medieval Factions plugin enabling....");

        this.getServer().getPluginManager().registerEvents(this, this);

        loadFactions();
        loadClaimedChunks();
        loadPlayerPowerRecords();

        System.out.println("Medieval Factions plugin enabled.");
    }

    @Override
    public void onDisable(){
        System.out.println("Medieval Factions plugin disabling....");

        saveFactionNames();
        saveFactions();
        saveClaimedChunkFilenames();
        saveClaimedChunks();
        savePlayerPowerRecordFilenames();
        savePlayerPowerRecords();

        System.out.println("Medieval Factions plugin disabled.");
    }

    public void saveFactionNames() {
        try {
            File saveFolder = new File("./plugins/medievalfactions/");
            if (!saveFolder.exists()) {
                saveFolder.mkdir();
            }
            File saveFile = new File("./plugins/medievalfactions/" + "faction-names.txt");
            if (saveFile.createNewFile()) {
                System.out.println("Save file for faction names created.");
            } else {
                System.out.println("Save file for faction names already exists. Overwriting.");
            }

            FileWriter saveWriter = new FileWriter(saveFile);

            // actual saving takes place here
            for (Faction faction : factions) {
                saveWriter.write(faction.getName() + "\n");
            }

            saveWriter.close();

        } catch (IOException e) {
            System.out.println("An error occurred while saving faction names.");
        }
    }

    public void saveFactions() {
        System.out.println("Saving factions...");
        for (Faction faction : factions) {
            faction.save(factions);
        }
        System.out.println("Factions saved.");
    }

    public void saveClaimedChunkFilenames() {
        try {
            File saveFolder = new File("./plugins/medievalfactions/claimedchunks/");
            if (!saveFolder.exists()) {
                saveFolder.mkdir();
            }
            File saveFile = new File("./plugins/medievalfactions/claimedchunks/" + "claimedchunks.txt");
            if (saveFile.createNewFile()) {
                System.out.println("Save file for claimed chunk filenames created.");
            } else {
                System.out.println("Save file for claimed chunk filenames already exists. Overwriting.");
            }

            FileWriter saveWriter = new FileWriter(saveFile);

            // actual saving takes place here
            for (ClaimedChunk chunk : claimedChunks) {
                double[] coords = chunk.getCoordinates();

                saveWriter.write((int)coords[0] + "_" + (int)coords[1] + ".txt" + "\n");
            }

            saveWriter.close();

        } catch (IOException e) {
            System.out.println("An error occurred while saving claimed chunk filenames.");
        }
    }

    public void saveClaimedChunks() {
        System.out.println("Saving claimed chunks...");
        for (ClaimedChunk chunk : claimedChunks) {
            chunk.save();
        }
        System.out.println("Claimed chunks saved.");
    }

    public void savePlayerPowerRecordFilenames() {
        try {
            File saveFolder = new File("./plugins/medievalfactions/player-power-records/");
            if (!saveFolder.exists()) {
                saveFolder.mkdir();
            }
            File saveFile = new File("./plugins/medievalfactions/player-power-records/" + "playerpowerrecords.txt");
            if (saveFile.createNewFile()) {
                System.out.println("Save file for player power record filenames created.");
            } else {
                System.out.println("Save file for player power record filenames already exists. Overwriting.");
            }

            FileWriter saveWriter = new FileWriter(saveFile);

            // actual saving takes place here
            for (PlayerPowerRecord record : playerPowerRecords) {
                saveWriter.write(record.getPlayerName() + ".txt");
            }

            saveWriter.close();

        } catch (IOException e) {
            System.out.println("An error occurred while saving player power record filenames.");
        }
    }

    public void savePlayerPowerRecords() {
        System.out.println("Saving player power records...");
        for (PlayerPowerRecord record: playerPowerRecords) {
            record.save();
        }
        System.out.println("Player power records saved.");
    }

    public void loadFactions() {
        try {
            System.out.println("Attempting to load factions...");
            File loadFile = new File("./plugins/medievalfactions/" + "faction-names.txt");
            Scanner loadReader = new Scanner(loadFile);

            // actual loading
            while (loadReader.hasNextLine()) {
                String nextName = loadReader.nextLine();
                Faction temp = new Faction(nextName); // uses server constructor, only temporary
                temp.load(nextName + ".txt"); // provides owner field among other things

                // existence check
                boolean exists = false;
                for (int i = 0; i < factions.size(); i++) {
                    if (factions.get(i).getName().equalsIgnoreCase(temp.getName())) {
                        factions.remove(i);
                    }
                }

                factions.add(temp);

            }

            loadReader.close();
            System.out.println("Factions successfully loaded.");
        } catch (FileNotFoundException e) {
            System.out.println("Error loading the factions!");
        }
    }

    public void loadClaimedChunks() {
        System.out.println("Loading claimed chunks...");

        try {
            System.out.println("Attempting to load claimed chunks...");
            File loadFile = new File("./plugins/medievalfactions/claimedchunks/" + "claimedchunks.txt");
            Scanner loadReader = new Scanner(loadFile);

            // actual loading
            while (loadReader.hasNextLine()) {
                String nextName = loadReader.nextLine();
                ClaimedChunk temp = new ClaimedChunk(); // uses no-parameter constructor since load provides chunk
                temp.load(nextName); // provides owner field among other things

                claimedChunks.add(temp);

            }

            loadReader.close();
            System.out.println("Claimed chunks successfully loaded.");
        } catch (FileNotFoundException e) {
            System.out.println("Error loading the claimed chunks!");
            e.printStackTrace();
        }

        System.out.println("Claimed chunks loaded.");
    }

    public void loadPlayerPowerRecords() {
        System.out.println("Loading player power records...");

        try {
            System.out.println("Attempting to load player power record filenames...");
            File loadFile = new File("./plugins/medievalfactions/player-power-records/" + "playerpowerrecords.txt");
            Scanner loadReader = new Scanner(loadFile);

            // actual loading
            while (loadReader.hasNextLine()) {
                String nextName = loadReader.nextLine();
                PlayerPowerRecord temp = new PlayerPowerRecord(); // uses no-parameter constructor since load provides name
                temp.load(nextName); // provides power field among other things

                playerPowerRecords.add(temp);

            }

            loadReader.close();
            System.out.println("Player power records loaded.");
        } catch (FileNotFoundException e) {
            System.out.println("Error loading the player power records!");
            e.printStackTrace();
        }

        System.out.println("Player power records loaded.");
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        // mf commands
        if (label.equalsIgnoreCase("mf")) {

            // no arguments check
            if (args.length == 0) {
                HelpCommand.sendHelpMessage(sender, args);
            }

            // argument check
            if (args.length > 0) {

                // help command
                if (args[0].equalsIgnoreCase("help")) {
                    HelpCommand.sendHelpMessage(sender, args);
                }

                // create command
                if (args[0].equalsIgnoreCase("create")) {
                    CreateCommand.createFaction(sender, args, factions);
                }

                // list command
                if  (args[0].equalsIgnoreCase("list")) {
                    ListCommand.listFactions(sender, factions);
                }

                // delete command
                if (args[0].equalsIgnoreCase("delete")) {
                    DeleteCommand.deleteFaction(sender, factions, claimedChunks);
                }

                // members command
                if (args[0].equalsIgnoreCase("members")) {
                    MembersCommand.showMembers(sender, args, factions);
                }

                // info command
                if (args[0].equalsIgnoreCase("info")) {
                    InfoCommand.showInfo(sender, args, factions);
                }

                // desc command
                if (args[0].equalsIgnoreCase("desc")) {
                    DescCommand.setDescription(sender, args, factions);
                }

                // invite command
                if (args[0].equalsIgnoreCase("invite")) {
                    InviteCommand.invitePlayer(sender, args, factions);
                }

                // join command
                if (args[0].equalsIgnoreCase("join")) {
                    JoinCommand.joinFaction(sender, args, factions);
                }

                // kick command
                if (args[0].equalsIgnoreCase("kick")) {
                    KickCommand.kickPlayer(sender, args, factions);
                }

                // leave commmand
                if (args[0].equalsIgnoreCase("leave")) {
                    LeaveCommand.leaveFaction(sender, factions, claimedChunks);
                }

                // transfer command
                if (args[0].equalsIgnoreCase("transfer")) {
                    TransferCommand.transferOwnership(sender, args, factions);
                }

                // declare war command
                if (args[0].equalsIgnoreCase("declarewar")) {
                    DeclareWarCommand.declareWar(sender, args, factions);
                }

                // make peace command
                if (args[0].equalsIgnoreCase("makepeace")) {
                    MakePeaceCommand.makePeace(sender, args, factions);
                }

                // claim command
                if (args[0].equalsIgnoreCase("claim")) {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        addChunkAtPlayerLocation(player);
                    }
                }

                // unclaim command
                if (args[0].equalsIgnoreCase("unclaim")) {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        removeChunkAtPlayerLocation(player);
                    }
                }

                // unclaimall command
                if (args[0].equalsIgnoreCase("unclaimall")) {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        for (Faction faction : factions) {
                            if (faction.isOwner(player.getName())) {
                                removeAllClaimedChunks(faction.getName(), claimedChunks);
                            }
                        }
                    }
                }

                // checkclaim command
                if (args[0].equalsIgnoreCase("checkclaim")) {
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

                // autoclaim command
                if (args[0].equalsIgnoreCase("autoclaim")) {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        for (Faction faction : factions) {
                            if (faction.isOwner(player.getName())) {
                                faction.toggleAutoClaim();
                                player.sendMessage(ChatColor.AQUA + "Autoclaim toggled.");
                            }
                        }
                    }

                }

                // promote command
                if (args[0].equalsIgnoreCase("promote")) {
                    PromoteCommand.promotePlayer(sender, args, factions);
                }

                // demote command
                if (args[0].equalsIgnoreCase("demote")) {
                    DemoteCommand.demotePlayer(sender, args, factions);
                }

                // forcesave command
                if (args[0].equalsIgnoreCase("forcesave")) {
                    if (!(sender instanceof Player)) {
                        System.out.println("Medieval Factions plugin is saving...");
                        saveFactionNames();
                        saveFactions();
                        saveClaimedChunkFilenames();
                        saveClaimedChunks();
                        savePlayerPowerRecordFilenames();
                        savePlayerPowerRecords();
                    }
                }

                // forceload command
                if (args[0].equalsIgnoreCase("forceload")) {
                    if (!(sender instanceof Player)) {
                        System.out.println("Medieval Factions plugin is loading...");
                        loadFactions();
                        loadClaimedChunks();
                        loadPlayerPowerRecords();
                    }
                }

            }
        }
        return false;
    }

    public static boolean isInFaction(String playerName, ArrayList<Faction> factions) {
        // membership check
        boolean isAlreadyInFaction = false;
        for (Faction faction : factions) {
            if (faction.isMember(playerName)) {
                isAlreadyInFaction = true;
                break;
            }
        }
        return isAlreadyInFaction;
    }

    public static void sendFactionInfo(Player player, Faction faction) {
        player.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + faction.getName() + " Faction Info" + "\n----------\n");
        player.sendMessage(ChatColor.AQUA + "Name: " + faction.getName() + "\n");
        player.sendMessage(ChatColor.AQUA + "Owner: " + faction.getOwner() + "\n");
        player.sendMessage(ChatColor.AQUA + "Description: " + faction.getDescription() + "\n");
        player.sendMessage(ChatColor.AQUA + "Population: " + faction.getMemberList().size() + "\n");
        player.sendMessage(ChatColor.AQUA + "At War With: " + faction.getEnemiesSeparatedByCommas() + "\n");
        player.sendMessage(ChatColor.AQUA + "----------\n");
    }

    public static void sendFactionMembers(Player player, Faction faction) {
        ArrayList<String> members = faction.getMemberList();
        player.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + "Members of " + faction.getName() + "\n----------\n");
        for (String member : members) {
            player.sendMessage(ChatColor.AQUA + member + "\n");
        }
        player.sendMessage(ChatColor.AQUA + "----------\n");
    }

    public static String createStringFromFirstArgOnwards(String[] args) {
        StringBuilder name = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            name.append(args[i]);
            if (!(i == args.length - 1)) {
                name.append(" ");
            }
        }
        return name.toString();
    }

    @EventHandler()
    public void onDamage(EntityDamageByEntityEvent event) {
        // this method disallows PVP between members of the same faction and between factions who are not at war
        // PVP is allowed between factionless players, players who belong to a faction and the factionless, and players whose factions are at war.

        // if this was between two players
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            Player victim = (Player) event.getEntity();

            int attackersFactionIndex = 0;
            int victimsFactionIndex = 0;

            for (int i = 0; i < factions.size(); i++) {
                if (factions.get(i).isMember(attacker.getName())) {
                    attackersFactionIndex = i;
                }
                if (factions.get(i).isMember(victim.getName())) {
                    victimsFactionIndex = i;
                }
            }

            // if attacker and victim are both in a faction
            if (isInFaction(attacker.getName(), factions) && isInFaction(victim.getName(), factions)) {
                // if attacker and victim are part of the same faction
                if (attackersFactionIndex == victimsFactionIndex) {
                    event.setCancelled(true);
                    attacker.sendMessage(ChatColor.RED + "You can't attack another player if you are part of the same faction.");
                    return;
                }

                // if attacker's faction and victim's faction are not at war
                if (!(factions.get(attackersFactionIndex).isEnemy(factions.get(victimsFactionIndex).getName())) &&
                    !(factions.get(victimsFactionIndex).isEnemy(factions.get(attackersFactionIndex).getName()))) {
                    event.setCancelled(true);
                    attacker.sendMessage(ChatColor.RED + "You can't attack another player if your factions aren't at war.");
                }
            }
        }
    }

    public void addChunkAtPlayerLocation(Player player) {
        double[] playerCoords = new double[2];
        playerCoords[0] = player.getLocation().getChunk().getX();
        playerCoords[1] = player.getLocation().getChunk().getZ();
        for (Faction faction : factions) {
            if (faction.isOwner(player.getName())) {

                // check if land is already claimed
                for (ClaimedChunk chunk : claimedChunks) {
                    if (playerCoords[0] == chunk.getCoordinates()[0] && playerCoords[1] == chunk.getCoordinates()[1]) {
                        // if holder is player's faction
                        if (chunk.getHolder().equalsIgnoreCase(faction.getName())) {
                            player.sendMessage(ChatColor.RED + "This land is already claimed by your faction!");
                            return;
                        }
                        else {
                            player.sendMessage(ChatColor.RED + "This land is already claimed by " + chunk.getHolder());
                            return;
                        }
                    }
                }

                ClaimedChunk newChunk = new ClaimedChunk(player.getLocation().getChunk());
                newChunk.setHolder(faction.getName());
                newChunk.setWorld(player.getLocation().getWorld().getName());
                claimedChunks.add(newChunk);
                player.sendMessage(ChatColor.GREEN + "Land claimed!");
                return;
            }
        }
    }

    public void removeChunkAtPlayerLocation(Player player) {
        double[] playerCoords = new double[2];
        playerCoords[0] = player.getLocation().getChunk().getX();
        playerCoords[1] = player.getLocation().getChunk().getZ();
        for (Faction faction : factions) {
            if (faction.isOwner(player.getName())) {

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
                                System.out.println("An error has occurred during file deletion.");
                                e.printStackTrace();
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
                        // add new chunk to claimed chunks
                        addChunkAtPlayerLocation(event.getPlayer());
                    }
                }
            }


            // if new chunk is claimed and old chunk was not
            if (isClaimed(event.getTo().getChunk()) && !isClaimed(event.getFrom().getChunk())) {
                event.getPlayer().sendMessage(ChatColor.GREEN + "Entering the territory of " + getClaimedChunk(event.getTo().getChunk().getX(), event.getTo().getChunk().getZ()).getHolder());
                return;
            }

            // if new chunk is unclaimed and old chunk was not
            if (!isClaimed(event.getTo().getChunk()) && isClaimed(event.getFrom().getChunk())) {
                event.getPlayer().sendMessage(ChatColor.GREEN + "Entering the wilderness");
                return;
            }


            // if new chunk is claimed and old chunk was also claimed
            if (isClaimed(event.getTo().getChunk()) && isClaimed(event.getFrom().getChunk())) {
                // if chunk holders are not equal
                if (!(getClaimedChunk(event.getFrom().getChunk().getX(), event.getFrom().getChunk().getZ()).getHolder().equalsIgnoreCase(getClaimedChunk(event.getTo().getChunk().getX(), event.getTo().getChunk().getZ()).getHolder()))) {
                    event.getPlayer().sendMessage(ChatColor.GREEN + "Leaving the territory of " + getClaimedChunk(event.getFrom().getChunk().getX(), event.getFrom().getChunk().getZ()).getHolder());
                    event.getPlayer().sendMessage(ChatColor.GREEN + "Entering the territory of " + getClaimedChunk(event.getTo().getChunk().getX(), event.getTo().getChunk().getZ()).getHolder());
                }
            }

        }

    }

    boolean isClaimed(Chunk chunk) {
        for (ClaimedChunk claimedChunk : claimedChunks) {
            if (claimedChunk.getCoordinates()[0] == chunk.getX() && claimedChunk.getCoordinates()[1] == chunk.getZ()) {
                return true;
            }
        }
        return false;
    }

    ClaimedChunk getClaimedChunk(int x, int z) {
        for (ClaimedChunk claimedChunk : claimedChunks) {
            if (claimedChunk.getCoordinates()[0] == x && claimedChunk.getCoordinates()[1] == z) {
                return claimedChunk;
            }
        }
        return null;
    }

    public static void removeAllClaimedChunks(String factionName, ArrayList<ClaimedChunk> claimedChunks) {

        Iterator<ClaimedChunk> itr = claimedChunks.iterator();


        while (itr.hasNext()) {
            ClaimedChunk currentChunk = itr.next();
            if (currentChunk.getHolder().equalsIgnoreCase(factionName)) {

                String identifier = (int) currentChunk.getChunk().getX() + "_" + (int) currentChunk.getChunk().getZ();

                try {

                    // delete file associated with chunk
                    System.out.println("Attempting to delete file plugins plugins/medievalfactions/claimedchunks/" + identifier + ".txt");
                    File fileToDelete = new File("plugins/medievalfactions/claimedchunks/" + identifier + ".txt");
                    if (fileToDelete.delete()) {
                        System.out.println("Success. File deleted.");
                    } else {
                        System.out.println("There was a problem deleting the file.");
                    }

                    itr.remove();
                }
                catch(Exception e) {
                    System.out.println("An error has occurred during claimed chunk removal.");
                    e.printStackTrace();
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
        ClaimedChunk chunk = getClaimedChunk(event.getBlock().getLocation().getChunk().getX(), event.getBlock().getLocation().getChunk().getZ());

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
    public void onBlockPlace(BlockPlaceEvent event) {
        // get player
        Player player = event.getPlayer();

        // get chunk
        ClaimedChunk chunk = getClaimedChunk(event.getBlock().getLocation().getChunk().getX(), event.getBlock().getLocation().getChunk().getZ());

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
        // get player
        Player player = event.getPlayer();

        // get chunk
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock != null) {
            ClaimedChunk chunk = getClaimedChunk(event.getClickedBlock().getLocation().getChunk().getX(), event.getClickedBlock().getLocation().getChunk().getZ());

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
    }

    public static void sendAllPlayersInFactionMessage(Faction faction, String message) {
        ArrayList<String> members = faction.getMemberArrayList();
        for (String member : members) {
            try {
                Player target = Bukkit.getServer().getPlayer(member);
                target.sendMessage(message);
            }
            catch(Exception ignored) {

            }
        }
    }
}