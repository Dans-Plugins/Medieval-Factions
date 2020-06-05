package plugin.Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import plugin.Faction;

import java.util.ArrayList;

import static plugin.Main.createStringFromFirstArgOnwards;

public class CreateCommand {

    public static boolean createFaction(CommandSender sender, String[] args, ArrayList<Faction> factions) {
        // player check
        if (sender instanceof Player) {
            Player player = (Player) sender;

            // player membership check
            for (Faction faction : factions) {
                if (faction.isMember(player.getName())) {
                    player.sendMessage(ChatColor.RED + "Sorry, you're already in a faction. Leave if you want to create a different one.");
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
                        break;
                    }
                }

                if (!factionExists) {

                    // creating name from arguments 1 to the last one
                    String name = createStringFromFirstArgOnwards(args);

                    // actual faction creation
                    Faction temp = new Faction(name, player.getName());
                    factions.add(temp);
                    factions.get(factions.size() - 1).addMember(player.getName());
                    System.out.println("Faction " + args[1] + " created.");
                    player.sendMessage(ChatColor.AQUA + "Faction " + name + " created.");
                    return true;
                }
                else {
                    player.sendMessage(ChatColor.RED + "Sorry! That faction already exists.");
                    return false;
                }
            } else {

                // wrong usage
                sender.sendMessage(ChatColor.RED + "Usage: /mf create [faction-name]");
                return false;
            }
        }
        return false;
    }
}
