package factionsystem.Commands;

import factionsystem.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;

import static factionsystem.UtilityFunctions.createStringFromFirstArgOnwards;
import static factionsystem.UtilityFunctions.sendAllPlayersInFactionMessage;

public class BreakAllianceCommand {
    public static void breakAlliance(CommandSender sender, String[] args, ArrayList<Faction> factions) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            boolean owner = false;
            for (Faction faction : factions) {
                // if player is the owner or officer
                if (faction.isOwner(player.getName()) || faction.isOfficer(player.getName())) {
                    owner = true;
                    // if there's more than one argument
                    if (args.length > 1) {

                        // get name of faction
                        String factionName = createStringFromFirstArgOnwards(args);

                        // check if faction exists
                        for (int i = 0; i < factions.size(); i++) {
                            if (factions.get(i).getName().equalsIgnoreCase(factionName)) {

                                if (!(faction.getName().equalsIgnoreCase(factionName))) {

                                    // check that enemy is not already on list
                                    if ((faction.isAlly(factionName))) {
                                        // remove alliance
                                        faction.removeAlly(factionName);
                                        player.sendMessage(ChatColor.GREEN + "Alliance has been broken with " + factionName + "!");

                                        // add declarer's faction to new enemy's enemyList
                                        factions.get(i).removeAlly(faction.getName());
                                        for (int j = 0; j < factions.size(); j++) {
                                            if (factions.get(j).getName().equalsIgnoreCase(factionName)) {
                                                sendAllPlayersInFactionMessage(factions.get(j), ChatColor.RED + faction.getName() + " has broken their alliance your faction!");
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
    }
}
