package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.Utilities;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MakePeaceCommand {

    public void makePeace(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (sender.hasPermission("mf.makepeace") || sender.hasPermission("mf.default")) {
                if (PersistentData.getInstance().isInFaction(player.getUniqueId())) {
                    Faction playersFaction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());

                    if (playersFaction.isOwner(player.getUniqueId()) || playersFaction.isOfficer(player.getUniqueId())) {

                        // player is able to do this command

                        if (args.length > 1) {
                            String targetFactionName = Utilities.getInstance().createStringFromFirstArgOnwards(args);
                            Faction targetFaction = PersistentData.getInstance().getFaction(targetFactionName);

                            if (!playersFaction.getName().equalsIgnoreCase(targetFactionName)) {

                                if (targetFaction != null) {

                                    if (!playersFaction.isTruceRequested(targetFactionName)) {
                                        // if not already requested

                                        if (playersFaction.isEnemy(targetFactionName)) {

                                            playersFaction.requestTruce(targetFactionName);
                                            player.sendMessage(ChatColor.GREEN + "Attempted to make peace with " + targetFactionName);

                                            Utilities.getInstance().sendAllPlayersInFactionMessage(targetFaction,ChatColor.GREEN + "" + playersFaction.getName() + " has attempted to make peace with " + targetFactionName + "!");

                                            if (playersFaction.isTruceRequested(targetFactionName) && targetFaction.isTruceRequested(playersFaction.getName())) {
                                                // remove requests in case war breaks out again and they need to make peace aagain
                                                playersFaction.removeRequestedTruce(targetFactionName);
                                                targetFaction.removeRequestedTruce(playersFaction.getName());

                                                // make peace between factions
                                                playersFaction.removeEnemy(targetFactionName);
                                                PersistentData.getInstance().getFaction(targetFactionName).removeEnemy(playersFaction.getName());
                                                Utilities.getInstance().sendAllPlayersOnServerMessage(ChatColor.GREEN + playersFaction.getName() + " is now at peace with " + targetFactionName + "!");
                                            }
                                        }
                                        else {
                                            player.sendMessage(ChatColor.RED + "That faction is not your enemy!");
                                        }

                                    }
                                    else {
                                        player.sendMessage(ChatColor.RED + "You've already requested peace with this faction!");
                                    }

                                }
                                else {
                                    player.sendMessage(ChatColor.RED + "That faction wasn't found!");
                                }
                            }
                            else {
                                player.sendMessage(ChatColor.RED + "You can't make peace with your own faction!");
                            }

                        }
                        else {
                            player.sendMessage(ChatColor.RED + "Usage: /mf makepeace (faction-name)");
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
                sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.makepeace'");
            }
        }
    }
}
