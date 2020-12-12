package factionsystem.Commands;

import factionsystem.Objects.Faction;
import factionsystem.Data.PersistentData;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static factionsystem.Util.Utilities.createStringFromFirstArgOnwards;
import static factionsystem.Util.Utilities.sendAllPlayersInFactionMessage;

public class BreakAllianceCommand {

    public void breakAlliance(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (sender.hasPermission("mf.breakalliance") || sender.hasPermission("mf.default")) {
                boolean owner = false;
                for (Faction faction : PersistentData.getInstance().getFactions()) {
                    // if player is the owner or officer
                    if (faction.isOwner(player.getUniqueId()) || faction.isOfficer(player.getUniqueId())) {
                        owner = true;
                        // if there's more than one argument
                        if (args.length > 1) {

                            // get name of faction
                            String factionName = createStringFromFirstArgOnwards(args);

                            // check if faction exists
                            for (int i = 0; i < PersistentData.getInstance().getFactions().size(); i++) {
                                if (PersistentData.getInstance().getFactions().get(i).getName().equalsIgnoreCase(factionName)) {

                                    if (!(faction.getName().equalsIgnoreCase(factionName))) {

                                        // check that enemy is not already on list
                                        if ((faction.isAlly(factionName))) {
                                            // remove alliance
                                            faction.removeAlly(factionName);
                                            player.sendMessage(ChatColor.GREEN + "Alliance has been broken with " + factionName + "!");

                                            // add declarer's faction to new enemy's enemyList
                                            PersistentData.getInstance().getFactions().get(i).removeAlly(faction.getName());
                                            for (int j = 0; j < PersistentData.getInstance().getFactions().size(); j++) {
                                                if (PersistentData.getInstance().getFactions().get(j).getName().equalsIgnoreCase(factionName)) {
                                                    sendAllPlayersInFactionMessage(PersistentData.getInstance().getFactions().get(j), ChatColor.RED + faction.getName() + " has broken their alliance your faction!");
                                                }
                                            }
                                        }
                                        else {
                                            player.sendMessage(ChatColor.RED + "Your faction is not allied with " + factionName);
                                        }

                                    }
                                    else {
                                        player.sendMessage(ChatColor.RED + "You can't declare break an alliance with your own faction.");
                                    }
                                }
                            }

                        }
                        else {
                            player.sendMessage(ChatColor.RED + "Usage: /mf breakalliance (faction-name)");
                        }
                    }
                }
                if (!owner) {
                    player.sendMessage(ChatColor.RED + "You have to own a faction or be an officer of a faction to use this command.");
                }
            }
            else {
                sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.breakalliance'");
            }
        }
    }
}
