package factionsystem.Commands;

import factionsystem.Main;
import factionsystem.Objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VassalizeCommand {

    Main main = null;

    public VassalizeCommand(Main plugin) {
        main = plugin;
    }

    public void sendVassalizationOffer(CommandSender sender, String[] args) {

        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (player.hasPermission("mf.vassalize") || player.hasPermission("mf.default")) {

                if (args.length > 1) {

                    String targetFactionName = main.utilities.createStringFromFirstArgOnwards(args);

                    Faction playersFaction = main.utilities.getPlayersFaction(player.getUniqueId(), main.factions);
                    Faction targetFaction = main.utilities.getFaction(targetFactionName, main.factions);

                    if (targetFaction != null) {

                        if (playersFaction != null) {

                            if (playersFaction.isOwner(player.getUniqueId())) {
                                // add faction to attemptedVassalizations
                                playersFaction.addAttemptedVassalization(targetFactionName);

                                // inform all players in that faction that they are trying to be vassalized
                                main.utilities.sendAllPlayersInFactionMessage(targetFaction, ChatColor.GREEN + "" + playersFaction.getName() + " has attempted to vassalize your faction! If you are the owner, type '/mf swearfealty " + playersFaction.getName() + "' to accept.");

                                // inform all players in players faction that a vassalization offer was sent
                                main.utilities.sendAllPlayersInFactionMessage(playersFaction, ChatColor.GREEN + "Your faction has attempted to vassalize " + targetFactionName + "!");

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
