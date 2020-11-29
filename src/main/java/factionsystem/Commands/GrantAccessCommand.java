package factionsystem.Commands;

import factionsystem.MedievalFactions;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static factionsystem.Subsystems.UtilitySubsystem.findUUIDBasedOnPlayerName;

public class GrantAccessCommand {

    MedievalFactions main = null;

    public GrantAccessCommand(MedievalFactions plugin) {
        main = plugin;
    }

    public void grantAccess(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (args.length > 1) {

                // if args[1] is cancel, cancel this
                if (args[1].equalsIgnoreCase("cancel")) {
                    player.sendMessage(ChatColor.GREEN + "Command cancelled!");
                    return;
                }

                // if not already granting access
                if (!main.playersGrantingAccess.containsKey(player.getUniqueId())) {
                    // save target name and player name in hashmap in main
                    main.playersGrantingAccess.put(player.getUniqueId(), findUUIDBasedOnPlayerName(args[1]));
                    player.sendMessage(ChatColor.GREEN + "Right click a chest or door to grant " + args[1] + " access. Type /mf grantaccess cancel to cancel this.");
                }
                else {
                    player.sendMessage(ChatColor.RED + "You are already granting access to someone! Type /mf grantaccess cancel to cancel this.");
                }
            }
            else {
                player.sendMessage(ChatColor.RED + "Usage: /grantaccess (player-name)");
            }

        }
    }

}
