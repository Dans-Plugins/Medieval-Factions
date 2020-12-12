package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.Utilities;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GrantIndependenceCommand {

    public void grantIndependence(CommandSender sender, String[] args) {

        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (player.hasPermission("mf.grantindependence") || player.hasPermission("mf.default")) {

                if (args.length > 1) {

                    String targetFactionName = Utilities.getInstance().createStringFromFirstArgOnwards(args);

                    Faction playersFaction = Utilities.getInstance().getPlayersFaction(player.getUniqueId(), PersistentData.getInstance().getFactions());
                    Faction targetFaction = Utilities.getInstance().getFaction(targetFactionName, PersistentData.getInstance().getFactions());

                    if (targetFaction != null) {

                        if (playersFaction != null) {

                            if (playersFaction.isOwner(player.getUniqueId())) {
                                // if target faction is a vassal
                                if (targetFaction.isLiege(playersFaction.getName())) {
                                    targetFaction.setLiege("none");
                                    playersFaction.removeVassal(targetFaction.getName());

                                    // inform all players in that faction that they are now independent
                                    Utilities.getInstance().sendAllPlayersInFactionMessage(targetFaction, ChatColor.GREEN + "" + targetFactionName + " has granted your faction independence!");

                                    // inform all players in players faction that a vassal was granted independence
                                    Utilities.getInstance().sendAllPlayersInFactionMessage(playersFaction, ChatColor.GREEN + "" + targetFactionName + " is no longer a vassal faction!");
                                }
                                else {
                                    player.sendMessage(ChatColor.RED + "That faction isn't a vassal of yours!");
                                }

                            }
                            else {
                                player.sendMessage(ChatColor.RED + "You must be the owner of your faction to use this command!");
                            }
                        }
                        else {
                            player.sendMessage(ChatColor.RED + "You must be in a faction to use this command!");
                        }
                    }
                    else {
                        // faction doesn't exist, send message
                        player.sendMessage(ChatColor.RED + "Sorry! That faction doesn't exist!");
                    }

                }
                else {
                    player.sendMessage(ChatColor.RED + "Usage: /mf grantindependence (faction-name)");
                }

            }
            else {
                // send perm message
                player.sendMessage(ChatColor.RED + "Sorry! In order to use this command, you need the following permission: 'mf.grantindependence'");
            }
        }

    }

}
