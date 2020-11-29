package factionsystem.Commands;

import factionsystem.MedievalFactions;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CheckAccessCommand extends Command {

    public CheckAccessCommand(MedievalFactions plugin) {
        super(plugin);
    }

    public void checkAccess(CommandSender sender, String[] args) {
        // if sender is player and if player has permission
        if (sender instanceof Player && (((Player) sender).hasPermission("mf.checkaccess") || ((Player) sender).hasPermission("mf.default"))) {

            Player player = (Player) sender;

            if (args.length > 1) {
                if (args[1].equalsIgnoreCase("cancel")) {
                    player.sendMessage(ChatColor.RED + "Cancelled!");
                    if (main.playersCheckingAccess.contains(player.getUniqueId())) {
                        main.playersCheckingAccess.remove(player.getUniqueId());
                        return;
                    }
                }
            }

            if (!main.playersCheckingAccess.contains(player.getUniqueId())) {
                main.playersCheckingAccess.add(player.getUniqueId());
                player.sendMessage(ChatColor.GREEN + "Right click a locked block to check who has access to it! Type '/mf checkaccess cancel' to cancel!");
            }
            else {
                player.sendMessage(ChatColor.RED + "You have already entered this command! Type '/mf checkaccess cancel' to cancel!");
            }

        }
    }

}
