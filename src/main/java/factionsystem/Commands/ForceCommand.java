package factionsystem.Commands;

import factionsystem.Main;
import factionsystem.Objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;

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

            if (args[1].equalsIgnoreCase("load")) {
                return forceLoad(sender);
            }

            if (args[1].equalsIgnoreCase("peace")) {
                return forcePeace(sender, args);
            }
            
            if (args[1].equalsIgnoreCase("demote")) {
                return forceDemote(sender, args);
            }

            if (args[1].equalsIgnoreCase("join")) {
                return forceJoin(sender, args);
            }

            if (args[1].equalsIgnoreCase("kick")) {
                return forceKick(sender, args);
            }
        }
        // show usages
        sender.sendMessage(ChatColor.RED + "Sub-commands:");
        sender.sendMessage(ChatColor.RED + "/mf force save");
        sender.sendMessage(ChatColor.RED + "/mf force load");
        sender.sendMessage(ChatColor.RED + "/mf force peace 'faction1' 'faction2'");
        return false;
    }

    private boolean forceSave(CommandSender sender) {
        if (sender.hasPermission("mf.force.save") || sender.hasPermission("mf.force.*") || sender.hasPermission("mf.admin")) {
            sender.sendMessage(ChatColor.GREEN + "Medieval Factions plugin is saving...");
            main.storage.save();
            return true;
        }
        else {
            sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.force.save'");
            return false;
        }
    }

    private boolean forceLoad(CommandSender sender) {
        if (sender.hasPermission("mf.force.load") || sender.hasPermission("mf.force.*")|| sender.hasPermission("mf.admin")) {
            sender.sendMessage(ChatColor.GREEN + "Medieval Factions plugin is loading...");
            main.storage.load();
            main.reloadConfig();
            return true;
        }
        else {
            sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.force.load'");
            return false;
        }
    }

    private boolean forcePeace(CommandSender sender, String[] args) {

        if (sender.hasPermission("mf.force.peace") || sender.hasPermission("mf.force.*")|| sender.hasPermission("mf.admin")) {

            if (args.length >= 4) {

                // get arguments designated by single quotes
                ArrayList<String> singleQuoteArgs = main.utilities.getArgumentsInsideSingleQuotes(args);

                if (singleQuoteArgs.size() < 2) {
                    sender.sendMessage(ChatColor.RED + "No factions designated. Must be designated inside single quotes!");
                    return false;
                }

                String factionName1 = singleQuoteArgs.get(0);
                String factionName2 = singleQuoteArgs.get(1);

                Faction faction1 = main.utilities.getFaction(factionName1, main.factions);
                Faction faction2 = main.utilities.getFaction(factionName2, main.factions);

                // force peace
                if (faction1 != null && faction2 != null) {
                    if (faction1.isEnemy(faction2.getName())) {
                        faction1.removeEnemy(faction2.getName());
                    }
                    if (faction2.isEnemy(faction1.getName())) {
                        faction2.removeEnemy(faction1.getName());
                    }

                    // announce peace to all players on server.
                    main.utilities.sendAllPlayersOnServerMessage(ChatColor.GREEN + faction1.getName() + " is now at peace with " + faction2.getName() + "!");
                    return true;
                }
                else {
                    sender.sendMessage(ChatColor.RED + "One of the factions designated wasn't found!");
                    return false;
                }
            }

            // send usage
            sender.sendMessage(ChatColor.RED + "Usage: /mf force peace 'faction-1' 'faction-2'");
            return false;
        }
        else {
            sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.force.peace'");
            return false;
        }

    }

    private boolean forceDemote(CommandSender sender, String[] args) {

        return false;
    }

    private boolean forceJoin(CommandSender sender, String[] args) {

        return false;
    }

    private boolean forceKick(CommandSender sender, String[] args) {

        return false;
    }

}
