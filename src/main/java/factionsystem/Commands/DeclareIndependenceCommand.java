package factionsystem.Commands;

import factionsystem.Main;
import factionsystem.Objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DeclareIndependenceCommand {

    Main main = null;

    public DeclareIndependenceCommand(Main plugin) {
        main = plugin;
    }

    public void declareIndependence(CommandSender sender) {

        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (player.hasPermission("mf.declareIndependence") || player.hasPermission("mf.default")) {

                Faction playersFaction = main.utilities.getPlayersFaction(player.getUniqueId(), main.factions);

                    if (playersFaction != null) {
                        // if faction has liege
                        if (playersFaction.hasLiege()) {

                            Faction targetFaction = main.utilities.getFaction(playersFaction.getLiege(), main.factions);

                            // if owner of faction
                            if (playersFaction.isOwner(player.getUniqueId())) {

                                // break vassal agreement
                                targetFaction.removeVassal(playersFaction.getName());
                                playersFaction.setLiege("none");

                                // add enemy to declarer's faction's enemyList and the enemyLists of its allies
                                playersFaction.addEnemy(targetFaction.getName());

                                // add declarer's faction to new enemy's enemyList
                                targetFaction.addEnemy(playersFaction.getName());

                                main.utilities.sendAllPlayersOnServerMessage(ChatColor.RED + playersFaction.getName() + " has declared independence from " + targetFaction.getName() + "!");
                           }
                            else {
                                // tell player they must be owner
                                player.sendMessage(ChatColor.RED + "Sorry, you must be the owner of a faction to use this command!");
                            }

                        }
                        else {
                            player.sendMessage(ChatColor.RED + "You aren't a vassal of a faction!");
                        }
                    }
                    else {
                        player.sendMessage(ChatColor.RED + "Sorry! You must be in a faction to use this command!");
                    }

            }
            else {
                player.sendMessage(ChatColor.RED + "Sorry! In order to use this command, you must have the following permission: 'mf.declareindependence'");
            }
        }

    }

}
