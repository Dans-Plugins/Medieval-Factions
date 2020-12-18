package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.Messenger;
import dansplugins.factionsystem.StorageManager;
import dansplugins.factionsystem.UUIDChecker;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.objects.PlayerPowerRecord;
import dansplugins.factionsystem.utils.ArgumentParser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;

public class ForceCommand {

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
            if (args[1].equalsIgnoreCase("renounce")) {
                return renounceVassalAndLiegeRelationships(sender, args);
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
        sender.sendMessage(ChatColor.RED + "/mf force renounce (faction)");
        return false;
    }

    private boolean forceSave(CommandSender sender) {
        if (sender.hasPermission("mf.force.save") || sender.hasPermission("mf.force.*") || sender.hasPermission("mf.admin")) {
            sender.sendMessage(ChatColor.GREEN + "Medieval Factions plugin is saving...");
            StorageManager.getInstance().save();
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
            StorageManager.getInstance().load();
            MedievalFactions.getInstance().reloadConfig();
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
                ArrayList<String> singleQuoteArgs = ArgumentParser.getInstance().getArgumentsInsideSingleQuotes(args);

                if (singleQuoteArgs.size() < 2) {
                    sender.sendMessage(ChatColor.RED + "No factions designated. Must be designated inside single quotes!");
                    return false;
                }

                String factionName1 = singleQuoteArgs.get(0);
                String factionName2 = singleQuoteArgs.get(1);

                Faction faction1 = PersistentData.getInstance().getFaction(factionName1);
                Faction faction2 = PersistentData.getInstance().getFaction(factionName2);

                // force peace
                if (faction1 != null && faction2 != null) {
                    if (faction1.isEnemy(faction2.getName())) {
                        faction1.removeEnemy(faction2.getName());
                    }
                    if (faction2.isEnemy(faction1.getName())) {
                        faction2.removeEnemy(faction1.getName());
                    }

                    // announce peace to all players on server.
                    Messenger.getInstance().sendAllPlayersOnServerMessage(ChatColor.GREEN + faction1.getName() + " is now at peace with " + faction2.getName() + "!");
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
                        for (Faction faction : PersistentData.getInstance().getFactions()) {
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
                ArrayList<String> singleQuoteArgs = ArgumentParser.getInstance().getArgumentsInsideSingleQuotes(args);

                if (singleQuoteArgs.size() < 2) {
                    sender.sendMessage(ChatColor.RED + "Not enough arguments designated. Must be designated inside single quotes!");
                    return false;
                }

                String playerName = singleQuoteArgs.get(0);
                String factionName = singleQuoteArgs.get(1);

                Faction faction = PersistentData.getInstance().getFaction(factionName);

                for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                    if (player.getName().equalsIgnoreCase(playerName)) {

                        if (faction != null) {
                            if (!(PersistentData.getInstance().isInFaction(player.getUniqueId()))) {
                                faction.addMember(player.getUniqueId(), PersistentData.getInstance().getPlayersPowerRecord(player.getUniqueId()).getPowerLevel());
                                try {
                                    Messenger.getInstance().sendAllPlayersInFactionMessage(faction, ChatColor.GREEN + player.getName() + " has joined " + faction.getName());
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
                        for (Faction faction : PersistentData.getInstance().getFactions()) {
                            if (faction.isOwner(player.getUniqueId())) {
                                sender.sendMessage(ChatColor.RED + "Cannot forcibly kick an owner from their faction! Try disbanding the faction!");
                                return false;
                            }

                            if (faction.isMember(player.getUniqueId())) {
                                faction.removeMember(player.getUniqueId(), PersistentData.getInstance().getPlayersPowerRecord(player.getUniqueId()).getPowerLevel());

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
                ArrayList<String> singleQuoteArgs = ArgumentParser.getInstance().getArgumentsInsideSingleQuotes(args);

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

                PlayerPowerRecord record = PersistentData.getInstance().getPlayersPowerRecord(UUIDChecker.getInstance().findUUIDBasedOnPlayerName(player));

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

    private boolean renounceVassalAndLiegeRelationships(CommandSender sender, String[] args) {

        if (sender.hasPermission("mf.force.renounce") || sender.hasPermission("mf.force.*") || sender.hasPermission("mf.admin")) {

            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /mf force renounce (faction)");
                return false;
            }

            String factionName = ArgumentParser.getInstance().createStringFromFirstArgOnwards(args);
            System.out.println("DEBUG: FACTION NAME IS " + factionName);

            int numReferences = 0;

            System.out.println("DEBUG: FOUND " + PersistentData.getInstance().getFactions().size() + " FACTIONS");

            for (Faction f : PersistentData.getInstance().getFactions()) {
                System.out.println("DEBUG: REMOVING LIEGE AND VASSAL REFERENCES ASSOCIATED WITH " + factionName + " for " + f.getName());
                // remove liege and vassal references associated with this faction
                System.out.println("DEBUG: LIEGE OF " + f.getName() + " IS " + f.getLiege());
                if (f.isLiege(factionName)) {
                    f.setLiege("none");
                    numReferences++;
                }
                System.out.println("DEBUG: " + f.getName() + " has " + f.getNumVassals() + " vassals");
                if (f.isVassal(factionName)) {
                    f.removeVassal(factionName);
                    numReferences++;
                }
            }

            Faction faction = PersistentData.getInstance().getFaction(factionName);

            if (faction != null) {
                System.out.println("DEBUG: CHECKING LIEGE OF " + factionName);
                System.out.println("DEBUG: LIEGE IS " + faction.getLiege());
                if (faction.getLiege().equalsIgnoreCase("none")) {
                    faction.setLiege("none");
                    numReferences++;
                }
                System.out.println("DEBUG: CHECKING VASSALS OF " + factionName);
                System.out.println("VASSALS FOUND: " + faction.getNumVassals());
                if (faction.getNumVassals() != 0) {
                    numReferences = numReferences + faction.getNumVassals();
                    faction.clearVassals();
                }

            }
            else {
                System.out.println("DEBUG: FACTION IS NULL");
            }

            if (numReferences != 0) {
                sender.sendMessage(ChatColor.GREEN + "Success! " + numReferences + " references removed!");
                return true;
            }
            else {
                sender.sendMessage(ChatColor.GREEN + "There were no vassal or liege references associated with " + factionName);
                return false;
            }


        }
        else {
            sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.force.renounce'");
            return false;
        }

    }

}
