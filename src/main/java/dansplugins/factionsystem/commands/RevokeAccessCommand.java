package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.UUIDChecker;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.utils.Utilities;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RevokeAccessCommand {

    public void revokeAccess(CommandSender sender, String[] args) {

        // player & perm check
        if (sender instanceof Player && ( ((Player) sender).hasPermission("mf.revokeaccess") || ((Player) sender).hasPermission("mf.default")) ) {

            Player player = (Player) sender;

            if (args.length > 1) {
                if (args[1].equalsIgnoreCase("cancel")) {
                    if (EphemeralData.getInstance().getPlayersRevokingAccess().containsKey(player.getUniqueId())) {
                        EphemeralData.getInstance().getPlayersRevokingAccess().remove(player.getUniqueId());
                        player.sendMessage(ChatColor.GREEN + "Cancelled!");
                        return;
                    }
                }
            }
            else {
                player.sendMessage(ChatColor.RED + "Usage: /mf revokeaccess (player-name)");
                return;
            }

            if (!EphemeralData.getInstance().getPlayersRevokingAccess().containsKey(player.getUniqueId())) {
                EphemeralData.getInstance().getPlayersRevokingAccess().put(player.getUniqueId(), UUIDChecker.getInstance().findUUIDBasedOnPlayerName(args[1]));
                player.sendMessage(ChatColor.GREEN + "Right click a locked block to revoke this player's access to it! Type '/mf revokeaccess cancel' to cancel!");
            }
            else {
                player.sendMessage(ChatColor.RED + "You have already entered this command! Type '/mf revokeaccess cancel' to cancel!");
            }

        }

    }

}
