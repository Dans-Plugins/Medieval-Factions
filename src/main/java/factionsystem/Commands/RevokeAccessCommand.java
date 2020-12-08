package factionsystem.Commands;

import factionsystem.MedievalFactions;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static factionsystem.Subsystems.UtilitySubsystem.findUUIDBasedOnPlayerName;

public class RevokeAccessCommand extends Command {

    public RevokeAccessCommand() {
        super();
    }

    public void revokeAccess(CommandSender sender, String[] args) {

        // player & perm check
        if (sender instanceof Player && ( ((Player) sender).hasPermission("mf.revokeaccess") || ((Player) sender).hasPermission("mf.default")) ) {

            Player player = (Player) sender;

            if (args.length > 1) {
                if (args[1].equalsIgnoreCase("cancel")) {
                    if (MedievalFactions.getInstance().playersRevokingAccess.containsKey(player.getUniqueId())) {
                        MedievalFactions.getInstance().playersRevokingAccess.remove(player.getUniqueId());
                        player.sendMessage(ChatColor.GREEN + "Cancelled!");
                        return;
                    }
                }
            }
            else {
                player.sendMessage(ChatColor.RED + "Usage: /mf revokeaccess (player-name)");
                return;
            }

            if (!MedievalFactions.getInstance().playersRevokingAccess.containsKey(player.getUniqueId())) {
                MedievalFactions.getInstance().playersRevokingAccess.put(player.getUniqueId(), findUUIDBasedOnPlayerName(args[1]));
                player.sendMessage(ChatColor.GREEN + "Right click a locked block to revoke this player's access to it! Type '/mf revokeaccess cancel' to cancel!");
            }
            else {
                player.sendMessage(ChatColor.RED + "You have already entered this command! Type '/mf revokeaccess cancel' to cancel!");
            }

        }

    }

}
