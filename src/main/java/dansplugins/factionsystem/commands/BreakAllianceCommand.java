package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.Messenger;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.ArgumentParser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BreakAllianceCommand {

    public void breakAlliance(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (sender.hasPermission("mf.breakalliance")) {
                boolean owner = false;
                for (Faction faction : PersistentData.getInstance().getFactions()) {
                    // if player is the owner or officer
                    if (faction.isOwner(player.getUniqueId()) || faction.isOfficer(player.getUniqueId())) {
                        owner = true;
                        // if there's more than one argument
                        if (args.length > 1) {

                            // get name of faction
                            String factionName = ArgumentParser.getInstance().createStringFromFirstArgOnwards(args);

                            // check if faction exists
                            for (int i = 0; i < PersistentData.getInstance().getFactions().size(); i++) {
                                if (PersistentData.getInstance().getFactions().get(i).getName().equalsIgnoreCase(factionName)) {

                                    if (!(faction.getName().equalsIgnoreCase(factionName))) {

                                        // check that enemy is not already on list
                                        if ((faction.isAlly(factionName))) {
                                            // remove alliance
                                            faction.removeAlly(factionName);
                                            player.sendMessage(ChatColor.GREEN + String.format(LocaleManager.getInstance().getText("AllianceBrokenWith"), factionName));

                                            // add declarer's faction to new enemy's enemyList
                                            PersistentData.getInstance().getFactions().get(i).removeAlly(faction.getName());
                                            for (int j = 0; j < PersistentData.getInstance().getFactions().size(); j++) {
                                                if (PersistentData.getInstance().getFactions().get(j).getName().equalsIgnoreCase(factionName)) {
                                                    Messenger.getInstance().sendAllPlayersInFactionMessage(PersistentData.getInstance().getFactions().get(j), ChatColor.RED + "" + String.format(LocaleManager.getInstance().getText("AlertAllianceHasBeenBroken"), faction.getName()));
                                                }
                                            }
                                        }
                                        else {
                                            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertNotAllied") + factionName);
                                        }

                                    }
                                    else {
                                        player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CannotBreakAllianceWithSelf"));
                                    }
                                }
                            }

                        }
                        else {
                            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageBreakAlliance"));
                        }
                    }
                }
                if (!owner) {
                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeOwnerOrOfficerToUseCommand"));
                }
            }
            else {
                sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("PermissionBreakAlliance"));
            }
        }
    }
}
