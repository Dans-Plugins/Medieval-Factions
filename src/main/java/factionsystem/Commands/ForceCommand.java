package factionsystem.Commands;

import factionsystem.Main;
import factionsystem.Objects.Faction;
import factionsystem.Objects.PlayerPowerRecord;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;

import static factionsystem.Subsystems.UtilitySubsystem.*;

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
            if (args[1].equalsIgnoreCase("power")) {
                return forcePower(sender, args);
            }
        }
        // show usages
        sender.sendMessage(ChatColor.RED + "Sub-commands:");
        sender.sendMessage(ChatColor.RED + "/mf force save");
        sender.sendMessage(ChatColor.RED + "/mf force load");
        sender.sendMessage(ChatColor.RED + "/mf force peace 'faction1' 'faction2'");
        sender.sendMessage(ChatColor.RED + "/mf force demote (player)");
        sender.sendMessage(ChatColor.RED + "/mf force join 'player' 'faction2'");
        sender.sendMessage(ChatColor.RED + "/mf force kick (player)");
        sender.sendMessage(ChatColor.RED + "/mf force power 'player' 'number'");
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

    private boolean forceDemote(CommandSender sender, String[] args) { // 1 argument
        if (sender.hasPermission("mf.force.demote") || sender.hasPermission("mf.force.*")|| sender.hasPermission("mf.admin")) {
            if (args.length > 2) {
                String playerName = args[2];
                for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                    if (player.getName().equalsIgnoreCase(playerName)) {
                        for (Faction faction : main.factions) {
                            if (faction.isOfficer(player.getUniqueId())) {
                                faction.removeOfficer(player.getUniqueId());

                                if (player.isOnline()) {
                                    Bukkit.getPlayer(player.getUniqueId()).sendMessage(ChatColor.AQUA + "You were forcibly demoted from officer status in the faction " + faction.getName() + "!");
                                }
                            }
                        }
                    }
                }

                sender.sendMessage(ChatColor.GREEN + "Success! If player was considered an officer in any faction, they are no longer.");
                return true;
            }
            else {
                sender.sendMessage(ChatColor.RED + "Usage: /mf force demote (player)");
                return false;
            }
        }
        else {
            sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.force.demote'");
            return false;
        }
    }

    private boolean forceJoin(CommandSender sender, String[] args) { // 2 arguments
        if (sender.hasPermission("mf.force.join") || sender.hasPermission("mf.force.*")|| sender.hasPermission("mf.admin")) {

            if (args.length >= 4) {

                // get arguments designated by single quotes
                ArrayList<String> singleQuoteArgs = main.utilities.getArgumentsInsideSingleQuotes(args);

                if (singleQuoteArgs.size() < 2) {
                    sender.sendMessage(ChatColor.RED + "Not enough arguments designated. Must be designated inside single quotes!");
                    return false;
                }

                String playerName = singleQuoteArgs.get(0);
                String factionName = singleQuoteArgs.get(1);

                Faction faction = main.utilities.getFaction(factionName, main.factions);

                for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                    if (player.getName().equalsIgnoreCase(playerName)) {

                        if (faction != null) {
                            if (!(isInFaction(player.getUniqueId(), main.factions))) {
                                faction.addMember(player.getUniqueId(), getPlayersPowerRecord(player.getUniqueId(), main.playerPowerRecords).getPowerLevel());
                                try {
                                    sendAllPlayersInFactionMessage(faction, ChatColor.GREEN + player.getName() + " has joined " + faction.getName());
                                } catch (Exception ignored) {

                                }
                                if (player.isOnline()) {
                                    Bukkit.getPlayer(player.getUniqueId()).sendMessage(ChatColor.AQUA + "You were forced to join the faction " + faction.getName() + "!");
                                }
                                sender.sendMessage(ChatColor.GREEN + "Success! Player was forced to join faction.");
                                return true;
                            }
                            else {
                                sender.sendMessage(ChatColor.RED + "That player is already in a faction, sorry!");
                                return false;
                            }
                        }
                        else {
                            sender.sendMessage(ChatColor.RED + "That faction wasn't found!");
                            return false;
                        }
                    }
                }
                sender.sendMessage(ChatColor.RED + "Player not found!");
                return false;
            }

            // send usage
            sender.sendMessage(ChatColor.RED + "Usage: /mf force join 'player' 'faction'");
            return false;
        }
        else {
            sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.force.join'");
            return false;
        }
    }

    private boolean forceKick(CommandSender sender, String[] args) { // 1 argument
        if (sender.hasPermission("mf.force.kick") || sender.hasPermission("mf.force.*")|| sender.hasPermission("mf.admin")) {
            if (args.length > 2) {
                String playerName = args[2];
                for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                    if (player.getName().equalsIgnoreCase(playerName)) {
                        for (Faction faction : main.factions) {
                            if (faction.isOwner(player.getUniqueId())) {
                                sender.sendMessage(ChatColor.RED + "Cannot forcibly kick an owner from their faction! Try disbanding the faction!");
                                return false;
                            }

                            if (faction.isMember(player.getUniqueId())) {
                                faction.removeMember(player.getUniqueId(), getPlayersPowerRecord(player.getUniqueId(), main.playerPowerRecords).getPowerLevel());

                                if (player.isOnline()) {
                                    Bukkit.getPlayer(player.getUniqueId()).sendMessage(ChatColor.AQUA + "You were forcibly kicked from the faction " + faction.getName() + "!");
                                }

                                if (faction.isOfficer(player.getUniqueId())) {
                                    faction.removeOfficer(player.getUniqueId());
                                }
                            }
                        }
                    }
                }

                sender.sendMessage(ChatColor.GREEN + "Success! If the player was in a faction, they are no longer a member.");
                return true;
            }
            else {
                sender.sendMessage(ChatColor.RED + "Usage: /mf force kick (player)");
                return false;
            }
        }
        else {
            sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.force.kick'");
            return false;
        }
    }

    public boolean forcePower(CommandSender sender, String[] args) {
        if (sender.hasPermission("mf.force.power") || sender.hasPermission("mf.force.*") || sender.hasPermission("mf.admin")) {

            if (args.length >= 4) {

                // get arguments designated by single quotes
                ArrayList<String> singleQuoteArgs = main.utilities.getArgumentsInsideSingleQuotes(args);

                if (singleQuoteArgs.size() < 2) {
                    sender.sendMessage(ChatColor.RED + "Player and desired power must be designated inside single quotes!");
                    return false;
                }

                String player = singleQuoteArgs.get(0);
                int desiredPower = -1;

                try {
                    desiredPower = Integer.parseInt(singleQuoteArgs.get(1));
                } catch (Exception e) {
                    sender.sendMessage(ChatColor.RED + "Desired power must be a number!");
                    return false;
                }

                PlayerPowerRecord record = getPlayersPowerRecord(findUUIDBasedOnPlayerName(player), main.playerPowerRecords);

                record.setPowerLevel(desiredPower);
                sender.sendMessage(ChatColor.GREEN + "The power level of '" + player + "' has been set to " + desiredPower);
                return true;
            }

            // send usage
            sender.sendMessage(ChatColor.RED + "Usage: /mf force power 'player' 'number'");
            return false;
        } else {
            sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.force.power'");
            return false;
        }
    }

}
