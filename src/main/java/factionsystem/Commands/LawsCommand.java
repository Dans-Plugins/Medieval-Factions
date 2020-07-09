package factionsystem.Commands;

import factionsystem.Main;
import factionsystem.Objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static factionsystem.Subsystems.UtilitySubsystem.getPlayersFaction;

public class LawsCommand {

    Main main = null;

    public LawsCommand(Main plugin) {
        main = plugin;
    }

    public void showLawsToPlayer(CommandSender sender, String[] args) {
        // player & perm check
        if (sender instanceof Player && ( ((Player) sender).hasPermission("mf.laws") || ((Player) sender).hasPermission("mf.default")) ) {

            Player player = (Player) sender;

            Faction faction = null;

            if (args.length == 1) {
                faction = getPlayersFaction(player.getName(), main.factions);
            }
            else {
                faction = getPlayersFaction(args[1], main.factions);
            }


            if (faction != null) {

                if (faction.getNumLaws() != 0) {

                    player.sendMessage(ChatColor.AQUA + "\n == Laws of " + faction.getName() + " == ");

                    // list laws
                    int counter = 1;
                    for (String law : faction.getLaws()) {
                        player.sendMessage(ChatColor.AQUA + "" + counter + ". " + faction.getLaws().get(counter - 1));
                        counter++;
                    }

                }
                else {
                    if (args.length == 1) {
                        player.sendMessage(ChatColor.RED + "Your faction doesn't have any laws.");
                    }
                    else {
                        player.sendMessage(ChatColor.RED + "That faction doesn't have any laws.");
                    }

                }

            }
            else {
                if (args.length == 1) {
                    player.sendMessage(ChatColor.RED + "You need to be in a faction to use this command!");
                }
                else {
                    player.sendMessage(ChatColor.RED + "That faction doesn't exist!");
                }

            }

        }
        else {
            sender.sendMessage(ChatColor.RED + "Sorry! In order to use this command, you need the following permission: 'mf.laws'");
        }
    }

}
