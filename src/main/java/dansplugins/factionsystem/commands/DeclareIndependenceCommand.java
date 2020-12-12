package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.domainobjects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DeclareIndependenceCommand {

    public void declareIndependence(CommandSender sender) {

        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (player.hasPermission("mf.declareIndependence") || player.hasPermission("mf.default")) {

                Faction playersFaction = MedievalFactions.getInstance().utilities.getPlayersFaction(player.getUniqueId(), PersistentData.getInstance().getFactions());

                    if (playersFaction != null) {
                        // if faction has liege
                        if (playersFaction.hasLiege()) {

                            Faction targetFaction = MedievalFactions.getInstance().utilities.getFaction(playersFaction.getLiege(), PersistentData.getInstance().getFactions());

                            // if owner of faction
                            if (playersFaction.isOwner(player.getUniqueId())) {

                                // break vassal agreement
                                targetFaction.removeVassal(playersFaction.getName());
                                playersFaction.setLiege("none");

                                // add enemy to declarer's faction's enemyList and the enemyLists of its allies
                                playersFaction.addEnemy(targetFaction.getName());

                                // add declarer's faction to new enemy's enemyList
                                targetFaction.addEnemy(playersFaction.getName());

                                MedievalFactions.getInstance().utilities.sendAllPlayersOnServerMessage(ChatColor.RED + playersFaction.getName() + " has declared independence from " + targetFaction.getName() + "!");
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
