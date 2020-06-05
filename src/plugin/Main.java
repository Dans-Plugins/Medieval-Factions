package plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import plugin.Commands.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class Main extends JavaPlugin {

    ArrayList<Faction> factions = new ArrayList<>();

    @Override
    public void onEnable() {
        System.out.println("Medieval Factions plugin enabling....");

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
            faction.save();
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
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        for (int i = 0; i < factions.size(); i++) {
                            if (factions.get(i).isMember(player.getName())) {
                                if (factions.get(i).isOwner(player.getName())) {
                                    // is faction empty?
                                    if (factions.get(i).getPopulation() == 1) {
                                        // able to leave
                                        factions.get(i).removeMember(player.getName());
                                        factions.remove(i);
                                        player.sendMessage(ChatColor.AQUA + "You left your faction. It was deleted since no one else was a member.");
                                    }
                                    else {
                                        player.sendMessage(ChatColor.RED + "Sorry! You must transfer ownership or kick everyone in your faction to leave.");
                                    }
                                }
                                else {
                                    // able to leave
                                    factions.get(i).removeMember(player.getName());
                                    player.sendMessage(ChatColor.AQUA + "You left your faction.");
                                }
                            }
                        }
                    }
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

                // transfer command
                if (args[0].equalsIgnoreCase("transfer")) {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        boolean owner = false;
                        for (Faction faction : factions) {
                            if (faction.isOwner(player.getName())) {
                                owner = true;
                                if (args.length > 1) {
                                    if (faction.isMember(args[1])) {

                                        // set owner
                                        faction.setOwner(args[1]);
                                        player.sendMessage(ChatColor.AQUA + "Ownership transferred to " + args[1]);

                                        // save
                                        saveFactionNames();
                                        saveFactions();

                                        return true;
                                    }
                                    else {
                                        player.sendMessage(ChatColor.RED + "That player isn't in your faction!");
                                        return false;
                                    }
                                }
                                else {
                                    player.sendMessage(ChatColor.RED + "Usage: /mf transfer (player-name)");
                                    return false;
                                }
                            }
                        }
                        if (!owner) {
                            player.sendMessage(ChatColor.RED + "You need to be the owner of a faction to use this command.");
                            return false;
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

}