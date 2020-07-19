package factionsystem.Commands;

import factionsystem.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BypassCommand {

    Main main = null;

    public BypassCommand(Main plugin) {
        main = plugin;
    }

    public void toggleBypass(CommandSender sender) {

        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (player.hasPermission("mf.bypass") || player.hasPermission("mf.admin")) {

                if (!main.adminsBypassingProtections.contains(player.getName())) {
                    main.adminsBypassingProtections.add(player.getName());
                    player.sendMessage(ChatColor.GREEN + "You are now bypassing protections provided by Medieval Factions.");
                }
                else {
                    main.adminsBypassingProtections.remove(player.getName());
                    player.sendMessage(ChatColor.GREEN + "You are no longer bypassing protections provided by Medieval Factions.");
                }

            }
            else {
                player.sendMessage(ChatColor.RED + "Sorry! In order to use this command, you need the following permission: 'mf.bypass");
            }
        }

    }
}
