package plugin.Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import plugin.Faction;

import java.util.ArrayList;

public class ListCommand {

    public static boolean listFactions(CommandSender sender, ArrayList<Faction> factions) {
        // if there aren't any factions
        if (factions.size() == 0) {
            sender.sendMessage(ChatColor.AQUA + "There are currently no factions.");
        }
        // factions exist, list them
        else {
            sender.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + "Factions" + "\n----------\n");
            for (Faction faction : factions) {
                sender.sendMessage(ChatColor.AQUA + faction.getName());
            }
            sender.sendMessage(ChatColor.AQUA + "----------\n");
        }
        return true;
    }
}
