package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.Messenger;
import dansplugins.factionsystem.StorageManager;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.objects.PlayerPowerRecord;
import dansplugins.factionsystem.utils.ArgumentParser;
import dansplugins.factionsystem.utils.UUIDChecker;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.UUID;

import static org.bukkit.Bukkit.getServer;

public class ForceCommand {

    public boolean force(CommandSender sender, String[] args) {
        if (args.length > 1) {
            if (args[1].equalsIgnoreCase("save") || args[1].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdForceSave"))) {
                return forceSave(sender);
            }

            if (args[1].equalsIgnoreCase("load") || args[1].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdForceLoad"))) {
                return forceLoad(sender);
            }

            if (args[1].equalsIgnoreCase("peace") || args[1].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdForcePeace"))) {
                return forcePeace(sender, args);
            }
            
            if (args[1].equalsIgnoreCase("demote") || args[1].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdForceDemote"))) {
                return forceDemote(sender, args);
            }

            if (args[1].equalsIgnoreCase("join") || args[1].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdForceJoin"))) {
                return forceJoin(sender, args);
            }

            if (args[1].equalsIgnoreCase("kick") || args[1].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdForceKick"))) {
                return forceKick(sender, args);
            }
            if (args[1].equalsIgnoreCase("power") || args[1].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdForcePower"))) {
                return forcePower(sender, args);
            }
            if (args[1].equalsIgnoreCase("renounce") || args[1].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdForceRenounce"))) {
                return renounceVassalAndLiegeRelationships(sender, args);
            }
            if (args[1].equalsIgnoreCase("transfer") || args[1].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdForceTransfer"))) {
                return forceTransfer(sender, args);
            }
        }
        // show usages
        sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("SubCommands"));
        sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HelpForceSave"));
        sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HelpForceLoad"));
        sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HelpForcePeace"));
        sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HelpForceDemote"));
        sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HelpForceJoin"));
        sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HelpForceKick"));
        sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HelpForcePower"));
        sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HelpForceRenounce"));
        sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HelpForceTransfer"));
        return false;
    }

    private boolean forceSave(CommandSender sender) {
        if (sender.hasPermission("mf.force.save") || sender.hasPermission("mf.force.*") || sender.hasPermission("mf.admin")) {
            sender.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("AlertForcedSave"));
            StorageManager.getInstance().save();
            return true;
        }
        else {
            sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.force.save"));
            return false;
        }
    }

    private boolean forceLoad(CommandSender sender) {
        if (sender.hasPermission("mf.force.load") || sender.hasPermission("mf.force.*")|| sender.hasPermission("mf.admin")) {
            sender.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("AlertForcedLoad"));
            StorageManager.getInstance().load();
            MedievalFactions.getInstance().reloadConfig();
            return true;
        }
        else {
            sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.force.load"));
            return false;
        }
    }

    private boolean forcePeace(CommandSender sender, String[] args) {

        if (sender.hasPermission("mf.force.peace") || sender.hasPermission("mf.force.*")|| sender.hasPermission("mf.admin")) {

            if (args.length >= 4) {

                // get arguments designated by single quotes
                ArrayList<String> singleQuoteArgs = ArgumentParser.getInstance().getArgumentsInsideSingleQuotes(args);

                if (singleQuoteArgs.size() < 2) {
                    sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("NoFactionsDesignatedSingleQuotesRequired"));
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
                    Messenger.getInstance().sendAllPlayersOnServerMessage(ChatColor.GREEN + faction1.getName() + LocaleManager.getInstance().getText("AlertNowAtPeaceWith") + faction2.getName() + "!");
                    return true;
                }
                else {
                    sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("DesignatedFactionNotFound"));
                    return false;
                }
            }

            // send usage
            sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageForcePeace"));
            return false;
        }
        else {
            sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.force.peace"));
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
                                    Bukkit.getPlayer(player.getUniqueId()).sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("AlertForcedDemotion"));
                                }
                            }
                        }
                    }
                }

                sender.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("SuccessOfficerRemoval"));
                return true;
            }
            else {
                sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageForceDemote"));
                return false;
            }
        }
        else {
            sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.force.demote"));
            return false;
        }
    }

    private boolean forceJoin(CommandSender sender, String[] args) { // 2 arguments
        if (sender.hasPermission("mf.force.join") || sender.hasPermission("mf.force.*")|| sender.hasPermission("mf.admin")) {

            if (args.length >= 4) {

                // get arguments designated by single quotes
                ArrayList<String> singleQuoteArgs = ArgumentParser.getInstance().getArgumentsInsideSingleQuotes(args);

                if (singleQuoteArgs.size() < 2) {
                    sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("NotEnoughArgumentsDesignatedSingleQuotesRequired"));
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
                                    Messenger.getInstance().sendAllPlayersInFactionMessage(faction, ChatColor.GREEN + player.getName() + LocaleManager.getInstance().getText("HasJoined") + faction.getName());
                                } catch (Exception ignored) {

                                }
                                if (player.isOnline()) {
                                    Bukkit.getPlayer(player.getUniqueId()).sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("AlertForcedToJoinFaction"));
                                }
                                sender.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("SuccessForceJoin"));
                                return true;
                            }
                            else {
                                sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("PlayerAlreadyInFaction"));
                                return false;
                            }
                        }
                        else {
                            sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("FactionNotFound"));
                            return false;
                        }
                    }
                }
                sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("PlayerNotFound"));
                return false;
            }

            // send usage
            sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageForceJoin"));
            return false;
        }
        else {
            sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.force.join"));
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
                                sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CannotForciblyKickOwner"));
                                return false;
                            }

                            if (faction.isMember(player.getUniqueId())) {
                                faction.removeMember(player.getUniqueId(), PersistentData.getInstance().getPlayersPowerRecord(player.getUniqueId()).getPowerLevel());

                                if (player.isOnline()) {
                                    Bukkit.getPlayer(player.getUniqueId()).sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("AlertForcedKick"));
                                }

                                if (faction.isOfficer(player.getUniqueId())) {
                                    faction.removeOfficer(player.getUniqueId());
                                }
                            }
                        }
                    }
                }

                sender.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("SuccessFactionMemberRemoval"));
                return true;
            }
            else {
                sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageForceKick"));
                return false;
            }
        }
        else {
            sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.force.kick"));
            return false;
        }
    }

    public boolean forcePower(CommandSender sender, String[] args) {
        if (sender.hasPermission("mf.force.power") || sender.hasPermission("mf.force.*") || sender.hasPermission("mf.admin")) {

            if (args.length >= 4) {

                // get arguments designated by single quotes
                ArrayList<String> singleQuoteArgs = ArgumentParser.getInstance().getArgumentsInsideSingleQuotes(args);

                if (singleQuoteArgs.size() < 2) {
                    sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("PlayerAndDesiredPowerSingleQuotesRequirement"));
                    return false;
                }

                String player = singleQuoteArgs.get(0);
                int desiredPower = -1;

                try {
                    desiredPower = Integer.parseInt(singleQuoteArgs.get(1));
                } catch (Exception e) {
                    sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("DesiredPowerMustBeANumber"));
                    return false;
                }

                PlayerPowerRecord record = PersistentData.getInstance().getPlayersPowerRecord(UUIDChecker.getInstance().findUUIDBasedOnPlayerName(player));

                record.setPowerLevel(desiredPower);
                sender.sendMessage(ChatColor.GREEN + String.format(LocaleManager.getInstance().getText("PowerLevelHasBeenSetTo"), desiredPower));
                return true;
            }

            // send usage
            sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageForcePower"));
            return false;
        } else {
            sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.force.power"));
            return false;
        }
    }

    private boolean renounceVassalAndLiegeRelationships(CommandSender sender, String[] args) {

        if (sender.hasPermission("mf.force.renounce") || sender.hasPermission("mf.force.*") || sender.hasPermission("mf.admin")) {

            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageForceRenounce"));
                return false;
            }

            ArrayList<String> singleQuoteArgs = ArgumentParser.getInstance().getArgumentsInsideSingleQuotes(args);

            // single quote args length check
            if (singleQuoteArgs.size() != 1) {
                sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("FactionMustBeDesignatedInsideSingleQuotes"));
                return false;
            }

            String factionName = singleQuoteArgs.get(0);

            int numReferences = 0;

            for (Faction f : PersistentData.getInstance().getFactions()) {
                // remove liege and vassal references associated with this faction
                if (f.isLiege(factionName)) {
                    f.setLiege("none");
                    numReferences++;
                }
                if (f.isVassal(factionName)) {
                    f.removeVassal(factionName);
                    numReferences++;
                }
            }

            Faction faction = PersistentData.getInstance().getFaction(factionName);

            if (faction != null) {
                if (!faction.getLiege().equalsIgnoreCase("none")) {
                    faction.setLiege("none");
                    numReferences++;
                }
                if (faction.getNumVassals() != 0) {
                    numReferences = numReferences + faction.getNumVassals();
                    faction.clearVassals();
                }

            }

            if (numReferences != 0) {
                sender.sendMessage(ChatColor.GREEN + "" + numReferences + LocaleManager.getInstance().getText("SuccessReferencesRemoved"));
                return true;
            }
            else {
                sender.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("NoVassalOrLiegeReferences"));
                return false;
            }


        }
        else {
            sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.force.renounce"));
            return false;
        }

    }

    public boolean forceTransfer(CommandSender sender, String[] args) {

        if (sender.hasPermission("mf.force.transfer") || sender.hasPermission("mf.force.*") || sender.hasPermission("mf.admin")) {

            if (args.length >= 4) {

                // get arguments designated by single quotes
                ArrayList<String> singleQuoteArgs = ArgumentParser.getInstance().getArgumentsInsideSingleQuotes(args);

                if (singleQuoteArgs.size() < 2) {
                    sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("FactionAndPlayerSingleQuotesRequirement"));
                    return false;
                }

                String factionName = singleQuoteArgs.get(0);
                String playerName = singleQuoteArgs.get(1);

                Faction faction = PersistentData.getInstance().getFaction(factionName);

                if (faction == null) {
                    sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("FactionNotFound"));
                    return false;
                }

                UUID uuid = UUIDChecker.getInstance().findUUIDBasedOnPlayerName(playerName);

                if (uuid == null) {
                    sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("PlayerNotFound"));
                    return false;
                }

                if (faction.isOwner(uuid)) {
                    sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertPlayerAlreadyOwner"));
                    return false;
                }

                if (!faction.isMember(uuid)) {
                    sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertPlayerNotInFaction"));
                    return false;
                }

                if (faction.isOfficer(uuid)) {
                    faction.removeOfficer(uuid);
                }

                faction.setOwner(uuid);

                try {
                    Player target = getServer().getPlayer(playerName);
                    target.sendMessage(ChatColor.GREEN + String.format(LocaleManager.getInstance().getText("OwnershipTransferred"), faction.getName()));
                }
                catch(Exception ignored) {

                }

                sender.sendMessage(ChatColor.GREEN + String.format(LocaleManager.getInstance().getText("OwnerShipTransferredTo"), playerName));
                return true;
            }

            // send usage
            sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageForceTransfer"));
            return false;
        } else {
            sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.force.transfer"));
            return false;
        }

    }

}
