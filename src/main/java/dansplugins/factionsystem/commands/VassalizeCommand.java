package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.Messenger;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.StringBuilder;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VassalizeCommand {

    public void sendVassalizationOffer(CommandSender sender, String[] args) {

        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (player.hasPermission("mf.vassalize") || player.hasPermission("mf.default")) {

                if (args.length > 1) {

                    String targetFactionName = StringBuilder.getInstance().createStringFromFirstArgOnwards(args);

                    Faction playersFaction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());
                    Faction targetFaction = PersistentData.getInstance().getFaction(targetFactionName);

                    if (targetFaction != null) {

                        if (playersFaction != null) {

                            if (playersFaction.isOwner(player.getUniqueId())) {

                                // make sure player isn't trying to vassalize their own faction
                                if (playersFaction.getName().equalsIgnoreCase(targetFaction.getName())) {
                                    player.sendMessage(ChatColor.RED + "You can't vassalize your own faction!");
                                    return;
                                }

                                // make sure player isn't trying to vassalize their liege
                                if (targetFaction.getName().equalsIgnoreCase(playersFaction.getLiege())) {
                                    player.sendMessage(ChatColor.RED + "You can't vassalize your liege!");
                                    return;
                                }

                                // make sure player isn't trying to vassalize a vassal
                                if (targetFaction.hasLiege()) {
                                    player.sendMessage(ChatColor.RED + "You can't vassalize a faction who already has a liege!");
                                    return;
                                }

                                // add faction to attemptedVassalizations
                                playersFaction.addAttemptedVassalization(targetFactionName);

                                // inform all players in that faction that they are trying to be vassalized
                                Messenger.getInstance().sendAllPlayersInFactionMessage(targetFaction, ChatColor.GREEN + "" + playersFaction.getName() + " has attempted to vassalize your faction! If you are the owner, type '/mf swearfealty " + playersFaction.getName() + "' to accept.");

                                // inform all players in players faction that a vassalization offer was sent
                                Messenger.getInstance().sendAllPlayersInFactionMessage(playersFaction, ChatColor.GREEN + "Your faction has attempted to vassalize " + targetFactionName + "!");

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
                    player.sendMessage(ChatColor.RED + "Usage: /mf vassalize (faction-name)");
                }

            }
            else {
                // send perm message
                player.sendMessage(ChatColor.RED + "Sorry! In order to use this command, you need the following permission: 'mf.vassalize'");
            }
        }

    }

}
