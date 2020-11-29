package factionsystem.Commands;

import factionsystem.MedievalFactions;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BypassCommand extends Command {

    public BypassCommand(MedievalFactions plugin) {
        super(plugin);
    }

    public void toggleBypass(CommandSender sender) {

        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (player.hasPermission("mf.bypass") || player.hasPermission("mf.admin")) {

                if (!main.adminsBypassingProtections.contains(player.getUniqueId())) {
                    main.adminsBypassingProtections.add(player.getUniqueId());
                    player.sendMessage(ChatColor.GREEN + "You are now bypassing protections provided by Medieval Factions.");
                }
                else {
                    main.adminsBypassingProtections.remove(player.getUniqueId());
                    player.sendMessage(ChatColor.GREEN + "You are no longer bypassing protections provided by Medieval Factions.");
                }

            }
            else {
                player.sendMessage(ChatColor.RED + "Sorry! In order to use this command, you need the following permission: 'mf.bypass");
            }
        }

    }
}
