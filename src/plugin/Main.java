package plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import plugin.Commands.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class Main extends JavaPlugin implements Listener {

    ArrayList<Faction> factions = new ArrayList<>();
    ArrayList<ClaimedChunk> claimedChunks = new ArrayList<>();

    @Override
    public void onEnable() {
        System.out.println("Medieval Factions plugin enabling....");

        this.getServer().getPluginManager().registerEvents(this, this);

        loadFactions();

        System.out.println("Medieval Factions plugin enabled.");
    }

    @Override
    public void onDisable(){
        System.out.println("Medieval Factions plugin disabling....");

        saveFactionNames();
        saveFactions();

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

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        // mf commands
        if (label.equalsIgnoreCase("mf")) {

            // no arguments check
            if (args.length == 0) {
                HelpCommand.sendHelpMessage(sender);
            }

            // argument check
            if (args.length > 0) {

                // help command
                if (args[0].equalsIgnoreCase("help")) {
                    HelpCommand.sendHelpMessage(sender);
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
                    DeleteCommand.deleteFaction(sender, factions);
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
                    LeaveCommand.leaveFaction(sender, factions);
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

                // forcesave command
                if (args[0].equalsIgnoreCase("forcesave")) {
                    if (!(sender instanceof Player)) {
                        System.out.println("Medieval Factions plugin is saving...");
                        saveFactionNames();
                        saveFactions();
                    }
                }

                // forceload command
                if (args[0].equalsIgnoreCase("forceload")) {
                    if (!(sender instanceof Player)) {
                        System.out.println("Medieval Factions plugin is loading...");
                        loadFactions();
                    }
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

            }
        }
        return false;
    }

    public static boolean isInFaction(String playerName, ArrayList<Faction> factions) {
        // membership check
        boolean isAlreadyInFaction = false;
        for (int i = 0; i < factions.size(); i++) {
            if (factions.get(i).isMember(playerName)) {
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
        String name = "";
        for (int i = 1; i < args.length; i++) {
            name = name + args[i];
            if (!(i == args.length - 1)) {
                name = name + " ";
            }
        }
        return name;
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
                    return;
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
        // if player enters a new chunk
        if (event.getFrom().getChunk() != event.getTo().getChunk()) {
            Player player = event.getPlayer();

            double[] playerCoords = new double[2];
            playerCoords[0] = player.getLocation().getChunk().getX();
            playerCoords[1] = player.getLocation().getChunk().getZ();

            for (ClaimedChunk chunk : claimedChunks) {

                // if new chunk is claimed
                if (playerCoords[0] == chunk.getCoordinates()[0] && playerCoords[1] == chunk.getCoordinates()[1]) {

                    // if old chunk was claimed
                    for (ClaimedChunk chunk2 : claimedChunks) {
                        if (event.getFrom().getChunk().getX() == chunk2.getCoordinates()[0] && event.getFrom().getChunk().getZ() == chunk2.getCoordinates()[1]) {

                            // if new chunk and old chunk were claimed by different factions
                            if (chunk.getHolder() != chunk2.getHolder()) {
                                player.sendMessage(ChatColor.GREEN + "Leaving the territory of " + chunk2.getHolder());
                                player.sendMessage(ChatColor.GREEN + "Entering the territory of " + chunk.getHolder());
                                return;
                            }

                        }
                    }

                    // otherwise if old chunk was unclaimed
                    player.sendMessage(ChatColor.GREEN + "Entering the territory of " + chunk.getHolder());
                    return;

                }

            }
            // otherwise new chunk is unclaimed
            player.sendMessage(ChatColor.GREEN + "Entering the wilderness.");
            return;
        }
    }

}