package plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import plugin.Commands.CreateCommand;
import plugin.Commands.HelpCommand;
import plugin.Commands.ListCommand;

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
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        boolean owner = false;
                        for (int i = 0; i < factions.size(); i++) {
                            if (factions.get(i).isOwner(player.getName())) {
                                owner = true;
                                if (factions.get(i).getPopulation() == 1) {
                                    factions.remove(i);
                                    player.sendMessage(ChatColor.AQUA + "Faction successfully deleted.");
                                    return true;
                                }
                                else {
                                    player.sendMessage(ChatColor.RED + "You need to kick all players before you can delete your faction.");
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

                // members command
                if (args[0].equalsIgnoreCase("members")) {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        if (args.length == 1) {
                            for (Faction faction : factions) {
                                if (faction.isMember(player.getName())) {
                                    sendFactionMembers(player, faction);
                                }
                            }
                        }
                        else {
                            // creating name from arguments 1 to the last one
                            String name = createStringFromFirstArgOnwards(args);

                            for (Faction faction : factions) {
                                if (faction.getName().equalsIgnoreCase(name)) {
                                    sendFactionMembers(player, faction);
                                }
                            }
                        }
                    }
                }

                // info command
                if (args[0].equalsIgnoreCase("info")) {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        if (args.length == 1) {
                            for (Faction faction : factions) {
                                if (faction.isMember(player.getName())) {
                                    sendFactionInfo(player, faction);
                                }
                            }
                        }
                        else {
                            // creating name from arguments 1 to the last one
                            String name = createStringFromFirstArgOnwards(args);

                            boolean exists = false;
                            for (Faction faction : factions) {
                                if (faction.getName().equals(name)) {
                                    exists = true;
                                    sendFactionInfo(player, faction);
                                }
                            }
                            if (!exists) {
                                player.sendMessage(ChatColor.RED + "That faction wasn't found!");
                            }
                        }
                    }
                }

                // desc command
                if (args[0].equalsIgnoreCase("desc")) {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        boolean owner = false;
                        for (Faction faction : factions) {
                            if (faction.isOwner(player.getName())) {
                                owner = true;
                                if (args.length > 1) {

                                    // set arg[1] - args[args.length-1] to be the description with spaces put in between
                                    String newDesc = "";
                                    for (int i = 1; i < args.length; i++) {
                                        newDesc = newDesc + args[i];
                                        if (!(i == args.length - 1)) {
                                            newDesc = newDesc + " ";
                                        }
                                    }

                                    faction.setDescription(newDesc);
                                    player.sendMessage(ChatColor.AQUA + "Description set!");
                                    return true;
                                }
                                else {
                                    player.sendMessage(ChatColor.RED + "Usage: /mf desc (description)");
                                    return false;
                                }
                            }
                        }
                        if (!owner) {
                            player.sendMessage(ChatColor.RED + "You need to be the owner of a faction to use this command.");
                        }
                    }
                }

                // invite command
                if (args[0].equalsIgnoreCase("invite")) {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        for (Faction faction : factions) {
                            if (faction.isOwner(player.getName())) {
                                if (args.length > 1) {

                                    // invite if player isn't in a faction already
                                    if (!(isInFaction(args[1]))) {
                                        faction.invite(args[1]);
                                        try {
                                            Player target = Bukkit.getServer().getPlayer(args[1]);
                                            target.sendMessage(ChatColor.GREEN + "You've been invited to " + faction.getName() + "! Type /mf join " + faction.getName() + " to join.");
                                        } catch (Exception e) {

                                        }
                                        player.sendMessage(ChatColor.GREEN + "Invitation sent!");
                                        return true;
                                    }
                                    else {
                                        player.sendMessage(ChatColor.RED + "That player is already in a faction, sorry!");
                                        return false;
                                    }


                                }
                                else {
                                    player.sendMessage(ChatColor.RED + "Usage: /mf invite (player-name)");
                                    return false;
                                }
                            }
                        }
                    }
                }

                // join command
                if (args[0].equalsIgnoreCase("join")) {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        if (args.length > 1) {

                            // creating name from arguments 1 to the last one
                            String factionName = createStringFromFirstArgOnwards(args);

                            for (Faction faction : factions) {
                                if (faction.getName().equalsIgnoreCase(factionName)) {
                                    if (faction.isInvited(player.getName())) {

                                        // join if player isn't in a faction already
                                        if (!(isInFaction(player.getName()))) {
                                            faction.addMember(player.getName());
                                            faction.uninvite(player.getName());
                                            try {
                                                Player target = Bukkit.getServer().getPlayer(faction.getOwner());
                                                target.sendMessage(ChatColor.GREEN + player.getName() + " has joined your faction.");
                                            } catch (Exception e) {

                                            }
                                            player.sendMessage(ChatColor.GREEN + "You joined the faction!");
                                            return true;
                                        }
                                        else {
                                            player.sendMessage(ChatColor.RED + "You're already in a faction, sorry!");
                                            return false;
                                        }

                                    } else {
                                        player.sendMessage(ChatColor.RED + "You're not invited to this faction!");
                                    }
                                }
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "Usage: /mf join (faction-name)");
                        }
                    }
                }

                // kick command
                if (args[0].equalsIgnoreCase("kick")) {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        if (args.length > 1) {
                            boolean owner = false;
                            for (Faction faction : factions) {
                                if (faction.isOwner(player.getName())) {
                                    owner = true;
                                    if (faction.isMember(args[1])) {
                                        if (!(args[1].equalsIgnoreCase(player.getName()))) {
                                            faction.removeMember(args[1]);
                                            player.sendMessage(ChatColor.AQUA + args[1] + " kicked.");
                                            try {
                                                Player target = Bukkit.getServer().getPlayer(args[1]);
                                                target.sendMessage(ChatColor.RED + "You have been kicked from your faction by " + player.getName() + ".");
                                            } catch (Exception ignored) {

                                            }
                                        }
                                        else {
                                            player.sendMessage(ChatColor.RED + "You can't kick yourself.");
                                        }

                                    }
                                }
                            }

                            if (!owner) {
                                player.sendMessage(ChatColor.RED + "You need to be the owner of a faction to use this command.");
                                return false;
                            }

                        } else {
                            player.sendMessage(ChatColor.RED + "Usage: /mf kick (player-name)");
                            return false;
                        }
                    }
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

    boolean isInFaction(String playerName) {
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

    void sendFactionInfo(Player player, Faction faction) {
        player.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + faction.getName() + " Faction Info" + "\n----------\n");
        player.sendMessage(ChatColor.AQUA + "Name: " + faction.getName() + "\n");
        player.sendMessage(ChatColor.AQUA + "Owner: " + faction.getOwner() + "\n");
        player.sendMessage(ChatColor.AQUA + "Description: " + faction.getDescription() + "\n");
        player.sendMessage(ChatColor.AQUA + "Population: " + faction.getMemberList().size() + "\n");
        player.sendMessage(ChatColor.AQUA + "----------\n");
    }

    void sendFactionMembers(Player player, Faction faction) {
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