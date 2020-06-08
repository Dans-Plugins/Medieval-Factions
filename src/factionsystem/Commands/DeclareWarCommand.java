package factionsystem.Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import factionsystem.Faction;

import java.util.ArrayList;

import static factionsystem.UtilityFunctions.createStringFromFirstArgOnwards;
import static factionsystem.UtilityFunctions.sendAllPlayersInFactionMessage;

public class DeclareWarCommand {
    public static void declareWar(CommandSender sender, String[] args, ArrayList<Faction> factions) {
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
                                    if (!(faction.isEnemy(factionName))) {
                                        // add enemy to declarer's faction's enemyList
                                        faction.addEnemy(factionName);
                                        player.sendMessage(ChatColor.AQUA + "War has been declared against " + factionName + "!");

                                        // add declarer's faction to new enemy's enemyList
                                        factions.get(i).addEnemy(faction.getName());
                                        try {
                                            for (int j = 0; j < factions.size(); j++) {
                                                if (factions.get(j).getName().equalsIgnoreCase(factionName)) {
                                                    sendAllPlayersInFactionMessage(factions.get(j), ChatColor.AQUA + faction.getName() + " has declared war against your faction!");
                                                }
                                            }
                                        } catch (Exception ignored) {

                                        }
                                    }
                                    else {
                                        player.sendMessage(ChatColor.RED + "Your faction is already at war with " + factionName);
                                    }

                                }
                                else {
                                    player.sendMessage(ChatColor.RED + "You can't declare war on your own faction.");
                                }
                            }
                        }

                    }
                    else {
                        player.sendMessage(ChatColor.RED + "Usage: /mf declarewar (faction-name)");
                    }
                }
            }
            if (!owner) {
                player.sendMessage(ChatColor.RED + "You have to own a faction or be an officer of a faction to use this command.");
            }
        }
    }
}
