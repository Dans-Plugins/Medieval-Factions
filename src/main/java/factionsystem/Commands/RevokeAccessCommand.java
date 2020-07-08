package factionsystem.Commands;

import factionsystem.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RevokeAccessCommand {

    Main main = null;

    public RevokeAccessCommand(Main plugin) {
        main = plugin;
    }

    public void revokeAccess(CommandSender sender, String[] args) {

        // player & perm check
        if (sender instanceof Player && ( ((Player) sender).hasPermission("mf.revokeaccess") || ((Player) sender).hasPermission("mf.default")) ) {

            Player player = (Player) sender;

            if (args.length > 1) {
                if (args[1].equalsIgnoreCase("cancel")) {
                    player.sendMessage(ChatColor.GREEN + "Cancelled!");
                    if (main.playersRevokingAccess.containsKey(player.getName())) {
                        main.playersRevokingAccess.remove(player.getName());
                        return;
                    }
                }
            }
            else {
                player.sendMessage(ChatColor.RED + "Usage: /mf revokeaccess (player-name)");
                return;
            }

            if (!main.playersCheckingAccess.contains(player.getName())) {
                main.playersRevokingAccess.put(player.getName(), args[1]);
                player.sendMessage(ChatColor.GREEN + "Right click a locked block to revoke this player's access to it! Type '/mf revokeaccess cancel' to cancel!");
            }
            else {
                player.sendMessage(ChatColor.RED + "You have already entered this command! Type '/mf revokeaccess cancel' to cancel!");
            }

        }

    }

}
