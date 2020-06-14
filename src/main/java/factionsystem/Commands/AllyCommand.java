package factionsystem.Commands;

import factionsystem.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;

import static factionsystem.UtilityFunctions.*;

public class AllyCommand {

    public static void requestAlliance(CommandSender sender, String[] args, ArrayList<Faction> factions) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (isInFaction(player.getName(), factions)) {
                Faction playersFaction = getPlayersFaction(player.getName(), factions);

                if (playersFaction.isOwner(player.getName()) || playersFaction.isOfficer(player.getName())) {

                    // player is able to do this command

                    if (args.length > 1) {
                        String targetFactionName = createStringFromFirstArgOnwards(args);
                        Faction targetFaction = getFaction(targetFactionName, factions);

                        if (targetFaction != null) {
                            if (!playersFaction.isAlly(targetFactionName)) {
                                // if not already ally

                                if (!playersFaction.isRequestedAlly(targetFactionName)) {
                                    // if not already requested

                                    if (!playersFaction.isEnemy(targetFactionName)) {

                                        playersFaction.requestAlly(targetFactionName);
                                        player.sendMessage(ChatColor.GREEN + "Attempted to ally with " + targetFactionName);

                                        sendAllPlayersInFactionMessage(targetFaction,ChatColor.GREEN + "" + playersFaction.getName() + " has attempted to ally with " + targetFactionName + "!");

                                        if (playersFaction.isRequestedAlly(targetFactionName) && targetFaction.isRequestedAlly(playersFaction.getName())) {
                                            System.out.println("DEBUG: Both factions want to ally! Allying!");
                                            // ally factions
                                            playersFaction.addAlly(targetFactionName);
                                            getFaction(targetFactionName, factions).addAlly(playersFaction.getName());
                                            player.sendMessage("Your faction is now allied with " + targetFactionName + "!");
                                            sendAllPlayersInFactionMessage(targetFaction, ChatColor.GREEN + "Your faction is now allied with " + playersFaction.getName() + "!");
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
    }
}
