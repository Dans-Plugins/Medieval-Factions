package plugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

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
            File saveFile = new File("faction-names.txt");
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
        for (int i = 0; i < factions.size(); i++) {
            factions.get(i).save();
        }
    }

    public void loadFactions() {
        try {
            System.out.println("Attempting to load factions...");
            File loadFile = new File("faction-names.txt");
            Scanner loadReader = new Scanner(loadFile);

            // actual loading
            while (loadReader.hasNextLine()) {
                String nextName = loadReader.nextLine();
                Faction temp = new Faction(nextName); // uses server constructor, only temporary
                temp.load(nextName + ".txt"); // provides owner field among other things
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
                    sender.sendMessage("/mf help - Show list of useful commands." + "\n");
                    sender.sendMessage("/mf create - Create a new faction." + "\n");
                    sender.sendMessage("/mf list - List all factions on the server." + "\n");
                    sender.sendMessage("/mf delete - Delete your faction (must be owner)." + "\n");
                    sender.sendMessage("/mf members - List the members of your faction." + "\n");
                    sender.sendMessage("/mf info - See your faction information." + "\n");
                    sender.sendMessage("/mf desc - Set your faction description." + "\n");
                }

                // create command
                if (args[0].equalsIgnoreCase("create")) {

                    // player check
                    if (sender instanceof Player) {
                        Player player = (Player) sender;

                        // player membership check
                        for (Faction faction : factions) {
                            if (faction.isMember(player.getName())) {
                                player.sendMessage("Sorry, you're already in a faction. Leave if you want to create a different one.");
                                return false;
                            }
                        }

                        // argument check
                        if (args.length > 1) {

                            // faction existence check
                            boolean factionExists = false;
                            for (Faction faction : factions) {
                                if (faction.getName().equalsIgnoreCase(args[1])) {
                                    factionExists = true;
                                }
                            }

                            if (!factionExists) {
                                // actual faction creation
                                Faction temp = new Faction(args[1], player.getName());
                                factions.add(temp);
                                factions.get(factions.size() - 1).addMember(player.getName());
                                System.out.println("Faction " + args[1] + " created.");
                                return true;
                            }
                            else {
                                player.sendMessage("Sorry! That faction already exists.");
                                return false;
                            }
                        } else {

                            // wrong usage
                            sender.sendMessage("Usage: /mf create [faction-name]");
                            return false;
                        }
                    }
                }

                // list command
                if  (args[0].equalsIgnoreCase("list")) {
                    // if there aren't any factions
                    if (factions.size() == 0) {
                        sender.sendMessage("There are currently no factions.");
                    }
                    // factions exist, list them
                    else {
                        for (Faction faction : factions) {
                            sender.sendMessage(faction.getName());
                        }
                    }
                }

                // delete command
                if (args[0].equalsIgnoreCase("delete")) {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        for (int i = 0; i < factions.size(); i++) {
                            if (factions.get(i).isOwner(player.getName())) {
                                if (factions.get(i).getPopulation() == 1) {
                                    factions.remove(i);
                                    player.sendMessage("Faction successfully deleted.");
                                }
                                else {
                                    player.sendMessage("You need to kick all players before you can delete your faction.");
                                }
                            }
                        }

                    }

                }

                // members command
                if (args[0].equalsIgnoreCase("members")) {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        for (Faction faction : factions) {
                            if (faction.isMember(player.getName())) {
                                ArrayList<String> members = faction.getMemberList();
                                for (String member : members) {
                                    player.sendMessage(member + "\n");
                                }
                            }
                        }
                    }
                }

                // info command
                if (args[0].equalsIgnoreCase("info")) {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        for (Faction faction : factions) {
                            if (faction.isMember(player.getName())) {
                                player.sendMessage("Name: " + faction.getName() + "\n");
                                player.sendMessage("Owner: " + faction.getOwner() + "\n");
                                player.sendMessage("Description: " + faction.getDescription() + "\n");
                                player.sendMessage("Population: " + faction.getMemberList().size() + "\n");
                            }
                        }
                    }

                    // desc command
                    if (args[0].equalsIgnoreCase("desc")) {
                        if (sender instanceof Player) {
                            Player player = (Player) sender;
                            for (Faction faction : factions) {
                                if (faction.isOwner(player.getName())) {
                                    if (args.length > 1) {
                                        faction.setDescription(args[1]);
                                        player.sendMessage("Description set!");
                                        return true;
                                    }
                                    else {
                                        player.sendMessage("Usage: /mf desc \"this is the description\" [quotes required]");
                                        return false;
                                    }
                                }
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
                                        faction.invite(args[1]);
                                        player.sendMessage("Invitation sent!");
                                        return true;
                                    }
                                    else {
                                        player.sendMessage("Usage: /mf invite (player-name)");
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
                                for (Faction faction : factions) {
                                    if (faction.getName().equalsIgnoreCase(args[1])) {
                                        if (faction.isInvited(player.getName())) {
                                            faction.addMember(player.getName());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            }
        }
        return false;
    }

}
