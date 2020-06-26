package factionsystem.Commands;

import factionsystem.Objects.Faction;
import factionsystem.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class ListCommand {

    Main main = null;

    public ListCommand(Main plugin) {
        main = plugin;
    }

    public boolean listFactions(CommandSender sender) {
        // if there aren't any factions
        if (main.factions.size() == 0) {
            sender.sendMessage(ChatColor.AQUA + "There are currently no factions.");
        }
        // factions exist, list them
        else {
            sender.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + "Factions" + "\n----------\n");
            for (Faction faction : main.factions) {
                sender.sendMessage(ChatColor.AQUA + faction.getName());
            }
            sender.sendMessage(ChatColor.AQUA + "----------\n");
        }
        return true;
    }
}
