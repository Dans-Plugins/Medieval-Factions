package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.data.EphemeralData;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BypassCommand {

    public void toggleBypass(CommandSender sender) {

        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (player.hasPermission("mf.bypass") || player.hasPermission("mf.admin")) {

                if (!EphemeralData.getInstance().getAdminsBypassingProtections().contains(player.getUniqueId())) {
                    EphemeralData.getInstance().getAdminsBypassingProtections().add(player.getUniqueId());
                    player.sendMessage(ChatColor.GREEN + "You are now bypassing protections provided by Medieval Factions.");
                }
                else {
                    EphemeralData.getInstance().getAdminsBypassingProtections().remove(player.getUniqueId());
                    player.sendMessage(ChatColor.GREEN + "You are no longer bypassing protections provided by Medieval Factions.");
                }

            }
            else {
                player.sendMessage(ChatColor.RED + "Sorry! In order to use this command, you need the following permission: 'mf.bypass");
            }
        }

    }
}
