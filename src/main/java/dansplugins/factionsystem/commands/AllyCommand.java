package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.Utilities;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AllyCommand {

    public void requestAlliance(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (sender.hasPermission("mf.ally") || sender.hasPermission("mf.default")) {
                if (PersistentData.getInstance().isInFaction(player.getUniqueId())) {
                    Faction playersFaction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());

                    if (playersFaction.isOwner(player.getUniqueId()) || playersFaction.isOfficer(player.getUniqueId())) {

                        // player is able to do this command

                        if (args.length > 1) {
                            String targetFactionName = Utilities.getInstance().createStringFromFirstArgOnwards(args);
                            Faction targetFaction = PersistentData.getInstance().getFaction(targetFactionName);

                            if (!playersFaction.getName().equalsIgnoreCase(targetFactionName)) {

                                if (targetFaction != null) {

                                    if (!playersFaction.isAlly(targetFactionName)) {
                                        // if not already ally

                                        if (!playersFaction.isRequestedAlly(targetFactionName)) {
                                            // if not already requested

                                            if (!playersFaction.isEnemy(targetFactionName)) {

                                                playersFaction.requestAlly(targetFactionName);
                                                player.sendMessage(ChatColor.GREEN + "Attempted to ally with " + targetFactionName);

                                                Utilities.getInstance().sendAllPlayersInFactionMessage(targetFaction,ChatColor.GREEN + "" + playersFaction.getName() + " has attempted to ally with " + targetFactionName + "!");

                                                if (playersFaction.isRequestedAlly(targetFactionName) && targetFaction.isRequestedAlly(playersFaction.getName())) {
                                                    // ally factions
                                                    playersFaction.addAlly(targetFactionName);
                                                    PersistentData.getInstance().getFaction(targetFactionName).addAlly(playersFaction.getName());
                                                    player.sendMessage(ChatColor.GREEN + "Your faction is now allied with " + targetFactionName + "!");
                                                    Utilities.getInstance().sendAllPlayersInFactionMessage(targetFaction, ChatColor.GREEN + "Your faction is now allied with " + playersFaction.getName() + "!");
                                                }
                                            }
                                            else {
                                                player.sendMessage(ChatColor.RED + "That faction is currently your enemy! Make peace before trying to ally with them.");
                                            }

                                        }
                                        else {
                                            player.sendMessage(ChatColor.RED + "You've already requested an alliance with this faction!");
                                        }

                                    }
                                    else {
                                        player.sendMessage(ChatColor.RED + "That faction is already your ally!");
                                    }
                                }
                                else {
                                    player.sendMessage(ChatColor.RED + "That faction wasn't found!");
                                }
                            }
                            else {
                                player.sendMessage(ChatColor.RED + "You can't ally with your own faction?");
                            }

                        }
                        else {
                            player.sendMessage(ChatColor.RED + "Usage: /mf ally (faction-name)");
                        }

                    }
                    else {
                        player.sendMessage(ChatColor.RED + "You need to be the owner of a faction or an officer of a faction to use this command.");
                    }
                }
                else {
                    player.sendMessage(ChatColor.RED + "You need to be in a faction to use this command.");
                }
            }
            else {
                sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.ally'");
            }
        }
    }
}
