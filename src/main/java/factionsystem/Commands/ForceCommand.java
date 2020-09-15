package factionsystem.Commands;

import factionsystem.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class ForceCommand {

    Main main = null;

    public ForceCommand(Main plugin) {
        main = plugin;
    }

    public boolean force(CommandSender sender, String[] args) {
        if (args.length > 1) {
            if (args[1].equalsIgnoreCase("save")) {
                return forceSave(sender);
            }

            // forceload command
            if (args[1].equalsIgnoreCase(".load")) {
                return forceLoad(sender);
            }
        }
        return false;
    }

    public boolean forceSave(CommandSender sender) {
        if (sender.hasPermission("mf.force.save") || sender.hasPermission("mf.force.*") || sender.hasPermission("mf.admin")) {
            sender.sendMessage(ChatColor.GREEN + "Medieval Factions plugin is saving...");
            main.storage.save();
            return true;
        }
        else {
            sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.forcesave'");
            return false;
        }
    }

    public boolean forceLoad(CommandSender sender) {
        if (sender.hasPermission("mf.force.load") || sender.hasPermission("mf.force.*")|| sender.hasPermission("mf.admin")) {
            sender.sendMessage(ChatColor.GREEN + "Medieval Factions plugin is loading...");
            main.storage.load();
            main.reloadConfig();
            return true;
        }
        else {
            sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.forceload'");
            return false;
        }
    }

}
