package factionsystem.Commands;

import factionsystem.MedievalFactions;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static factionsystem.Subsystems.UtilitySubsystem.findUUIDBasedOnPlayerName;

public class RevokeAccessCommand {

    MedievalFactions main = null;

    public RevokeAccessCommand(MedievalFactions plugin) {
        main = plugin;
    }

    public void revokeAccess(CommandSender sender, String[] args) {

        // player & perm check
        if (sender instanceof Player && ( ((Player) sender).hasPermission("mf.revokeaccess") || ((Player) sender).hasPermission("mf.default")) ) {

            Player player = (Player) sender;

            if (args.length > 1) {
                if (args[1].equalsIgnoreCase("cancel")) {
                    if (main.playersRevokingAccess.containsKey(player.getUniqueId())) {
                        main.playersRevokingAccess.remove(player.getUniqueId());
                        player.sendMessage(ChatColor.GREEN + "Cancelled!");
                        return;
                    }
                }
            }
            else {
                player.sendMessage(ChatColor.RED + "Usage: /mf revokeaccess (player-name)");
                return;
            }

            if (!main.playersRevokingAccess.containsKey(player.getUniqueId())) {
                main.playersRevokingAccess.put(player.getUniqueId(), findUUIDBasedOnPlayerName(args[1]));
                player.sendMessage(ChatColor.GREEN + "Right click a locked block to revoke this player's access to it! Type '/mf revokeaccess cancel' to cancel!");
            }
            else {
                player.sendMessage(ChatColor.RED + "You have already entered this command! Type '/mf revokeaccess cancel' to cancel!");
            }

        }

    }

}
