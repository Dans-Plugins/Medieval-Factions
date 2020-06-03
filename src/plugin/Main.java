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

    ArrayList<Faction> factions = new ArrayList<Faction>();

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

    public boolean saveFactionNames() {
        try {
            File saveFile = new File("faction-names.txt");
            if (saveFile.createNewFile()) {
                System.out.println("Save file for faction names created.");
            } else {
                System.out.println("Save file for faction names already exists. Overwriting.");
            }

            FileWriter saveWriter = new FileWriter(saveFile);

            // actual saving takes place here
            for (int i = 0; i < factions.size(); i++) {
                saveWriter.write(factions.get(i).getName() + "\n");
            }

            saveWriter.close();
            return true;

        } catch (IOException e) {
            System.out.println("An error occurred while saving faction names.");
            return false;
        }
    }

    public void saveFactions() {
        System.out.println("Saving factions...");
        for (int i = 0; i < factions.size(); i++) {
            factions.get(i).save();
        }
    }

    public boolean loadFactions() {
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
            return true;
        } catch (FileNotFoundException e) {
            System.out.println("Error loading the factions!");
            return false;
        }
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        // mf commands
        if (label.equalsIgnoreCase("mf")) {

            // argument check
            if (args.length > 0) {

                // help command
                if (args[0].equalsIgnoreCase("help")) {
                    sender.sendMessage("/help" + "\n");
                    sender.sendMessage("/create" + "\n");
                    sender.sendMessage("/list" + "\n");
                    sender.sendMessage("/delete" + "\n");
                    sender.sendMessage("/members" + "\n");
                }

                // create command
                if (args[0].equalsIgnoreCase("create")) {

                    // player check
                    if (sender instanceof Player) {
                        Player player = (Player) sender;

                        // player membership check
                        for (int i = 0; i < factions.size(); i++) {
                            if (factions.get(i).isMember(player.getName())) {
                                player.sendMessage("Sorry, you're already in a faction. Leave if you want to create a different one.");
                                return false;
                            }
                        }

                        // argument check
                        if (args.length > 1) {

                            // faction existence check
                            boolean factionExists = false;
                            for (int i = 0; i < factions.size(); i++) {
                                if (factions.get(i).getName().equalsIgnoreCase(args[1])) {
                                    factionExists = true;
                                }
                            }

                            if (factionExists == false) {
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
                        for (int i = 0; i < factions.size(); i++) {
                            sender.sendMessage(factions.get(i).getName());
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
                        for (int i = 0; i < factions.size(); i++) {
                            if (factions.get(i).isMember(player.getName())) {
                                ArrayList<String> members = factions.get(i).getMemberList();
                                for (int j = 0; j < members.size(); j++) {
                                    player.sendMessage(members.get(j));
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
