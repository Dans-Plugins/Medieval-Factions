package factionsystem.Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import factionsystem.Faction;

import java.util.ArrayList;

import static factionsystem.UtilityFunctions.createStringFromFirstArgOnwards;
import static factionsystem.UtilityFunctions.sendAllPlayersInFactionMessage;

public class MakePeaceCommand {
        public static void makePeace(CommandSender sender, String[] args, ArrayList<Faction> factions) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            boolean owner = false;
            for (Faction faction : factions) {
                // if player is the owner or an officer
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

                                    // check that enemy exists
                                    if (faction.isEnemy(factionName)) {
                                        // add enemy to declarer's faction's enemyList
                                        faction.removeEnemy(factionName);
                                        player.sendMessage(ChatColor.AQUA + "You have tried to make peace with " + factionName + "!");
                                        try {
                                            for (int j = 0; j < factions.size(); j++) {
                                                if (factions.get(j).getName().equalsIgnoreCase(factionName)) {
                                                    sendAllPlayersInFactionMessage(factions.get(j), ChatColor.AQUA + faction.getName() + " has tried to make peace with your faction!");
                                                }
                                            }
                                        } catch (Exception ignored) {

                                        }
                                    }
                                    else {
                                        player.sendMessage(ChatColor.RED + "Your faction is not currently at war with " + factionName + ".");
                                    }


                                }
                                else {
                                    player.sendMessage(ChatColor.RED + "You can't make peace with your own faction.");
                                }
                            }
                        }

                    }
                    else {
                        player.sendMessage(ChatColor.RED + "Usage: /mf makepeace (faction-name)");
                    }
                }
            }
            if (!owner) {
                player.sendMessage(ChatColor.RED + "You have to own a faction to use this command.");
            }
        }
    }
}
