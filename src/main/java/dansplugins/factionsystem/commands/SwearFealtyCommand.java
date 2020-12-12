package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.domainobjects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SwearFealtyCommand {

    public void swearFealty(CommandSender sender, String[] args) {

        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (player.hasPermission("mf.swearfealty") || player.hasPermission("mf.default")) {

                if (args.length > 1) {

                    String targetFactionName = MedievalFactions.getInstance().utilities.createStringFromFirstArgOnwards(args);

                    Faction playersFaction = MedievalFactions.getInstance().utilities.getPlayersFaction(player.getUniqueId(), PersistentData.getInstance().getFactions());
                    Faction targetFaction = MedievalFactions.getInstance().utilities.getFaction(targetFactionName, PersistentData.getInstance().getFactions());

                    if (targetFaction != null) {

                        if (playersFaction != null) {
                            // if offered vassalization
                            if (targetFaction.hasBeenOfferedVassalization(playersFaction.getName())) {

                                // if owner of faction
                                if (playersFaction.isOwner(player.getUniqueId())) {

                                    // set vassal
                                    targetFaction.addVassal(playersFaction.getName());
                                    targetFaction.removeAttemptedVassalization(playersFaction.getName());

                                    // inform target faction that they have a new vassal
                                    MedievalFactions.getInstance().utilities.sendAllPlayersInFactionMessage(targetFaction, ChatColor.GREEN + "Your faction has a new vassal: " + playersFaction.getName());

                                    // set liege
                                    playersFaction.setLiege(targetFactionName);

                                    // inform players faction that they have a new liege
                                    MedievalFactions.getInstance().utilities.sendAllPlayersInFactionMessage(playersFaction, ChatColor.GREEN + "Your faction has been vassalized and has a new liege: " + targetFactionName);
                                }
                                else {
                                    // tell player they must be owner
                                    player.sendMessage(ChatColor.RED + "Sorry, you must be the owner of a faction to use this command!");
                                }

                            }
                            else {
                                // tell player they haven't offered vassalization to their faction
                                player.sendMessage(ChatColor.RED + "You haven't been offered vassalization by this faction!");
                            }
                        }
                        else {
                            player.sendMessage(ChatColor.RED + "Sorry! You must be in a faction to use this command!");
                        }
                    }
                    else {
                        // faction doesn't exist, send message
                        player.sendMessage(ChatColor.RED + "Sorry! That faction doesn't exist!");
                    }

                }
                else {
                    player.sendMessage(ChatColor.RED + "Usage: /mf swearfealty (faction-name)");
                }

            }
            else {
                // send perm message
                player.sendMessage(ChatColor.RED + "Sorry! In order to use this command, you need the following permission: 'mf.swearfealty'");
            }
        }

    }

}
