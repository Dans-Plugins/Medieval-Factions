package factionsystem.Commands;

import factionsystem.Main;
import factionsystem.Objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static factionsystem.Utility.UtilityFunctions.getPlayersFaction;

public class LawsCommand {

    Main main = null;

    public LawsCommand(Main plugin) {
        main = plugin;
    }

    public void showLawsToPlayer(CommandSender sender) {
        // player & perm check
        if (sender instanceof Player && ( ((Player) sender).hasPermission("mf.laws") || ((Player) sender).hasPermission("mf.default")) ) {

            Player player = (Player) sender;

            Faction playersFaction = getPlayersFaction(player.getName(), main.factions);

            if (playersFaction != null) {

                if (playersFaction.getNumLaws() != 0) {

                    player.sendMessage(ChatColor.RED + " == Laws of " + playersFaction.getName() + " == ");

                    // list laws
                    int counter = 1;
                    for (String law : playersFaction.getLaws()) {
                        player.sendMessage(ChatColor.AQUA + "" + counter + ": " + playersFaction.getLaws().get(counter - 1));
                        counter++;
                    }

                }
                else {
                    player.sendMessage(ChatColor.RED + "Your faction hasn't set any laws yet.");
                }

            }
            else {
                player.sendMessage(ChatColor.RED + "You need to be in a faction to use this command!");
            }

        }
        else {
            sender.sendMessage(ChatColor.RED + "Sorry! In order to use this command, you need the following permission: 'mf.laws'");
        }
    }

}
