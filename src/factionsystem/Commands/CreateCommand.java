package factionsystem.Commands;

import factionsystem.PlayerPowerRecord;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import factionsystem.Faction;

import java.util.ArrayList;

import static factionsystem.UtilityFunctions.createStringFromFirstArgOnwards;
import static factionsystem.UtilityFunctions.getPlayersPowerRecord;

public class CreateCommand {

    public static boolean createFaction(CommandSender sender, String[] args, ArrayList<Faction> factions, ArrayList<PlayerPowerRecord> powerRecords) {
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

                // creating name from arguments 1 to the last one
                String name = createStringFromFirstArgOnwards(args);

                // faction existence check
                boolean factionExists = false;
                for (Faction faction : factions) {
                    if (faction.getName().equalsIgnoreCase(name)) {
                        factionExists = true;
                        break;
                    }
                }

                if (!factionExists) {

                    // actual faction creation
                    Faction temp = new Faction(name, player.getName());
                    factions.add(temp);
                    factions.get(factions.size() - 1).addMember(player.getName(), getPlayersPowerRecord(player.getName(), powerRecords).getPowerLevel());
                    System.out.println("Faction " + name + " created.");
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
