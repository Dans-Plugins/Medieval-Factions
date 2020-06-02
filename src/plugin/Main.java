package plugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;

public class Main extends JavaPlugin {

    ArrayList<Faction> factions = new ArrayList<Faction>();

    @Override
    public void onEnable() {
        System.out.println("Medieval Factions plugin enabling....");



        System.out.println("Medieval Factions plugin enabled.");
    }

    @Override
    public void onDisable(){

    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        // mf commands
        if (label.equalsIgnoreCase("mf")) {

            // argument check
            if (args.length > 0) {

                // create command
                if (args[0].equalsIgnoreCase("create")) {

                    // player check
                    if (sender instanceof Player) {
                        Player player = (Player) sender;

                        // argument check
                        if (args.length > 1) {

                            // actual faction creation
                            Faction temp = new Faction(args[1]);
                            factions.add(temp);
                            factions.get(factions.size() - 1).addMember(player.getName());
                            System.out.println("Faction " + args[1] + " created.");
                            return true;

                        } else {

                            // wrong usage
                            sender.sendMessage("Usage: /mf create [faction-name]");

                        }
                    }
                }
            } else {
                // TODO:
                // Show help message
            }
        }

        return false;
    }

}
