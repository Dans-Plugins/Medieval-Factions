package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChatCommand {

    public void toggleFactionChat(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (player.hasPermission("mf.chat") || player.hasPermission("mf.default")) {
                if (PersistentData.getInstance().isInFaction(player.getUniqueId())) {
                    if (!EphemeralData.getInstance().getPlayersInFactionChat().contains(player.getUniqueId())) {
                        EphemeralData.getInstance().getPlayersInFactionChat().add(player.getUniqueId());
                        player.sendMessage(ChatColor.GREEN + "You are now speaking in faction chat!");
                    }
                    else {
                        EphemeralData.getInstance().getPlayersInFactionChat().remove(player.getUniqueId());
                        player.sendMessage(ChatColor.GREEN + "You are no longer in faction chat!");
                    }
                }
                else {
                    player.sendMessage(ChatColor.RED + "You must be in a faction to use this command!");
                }

            }
            else {
                player.sendMessage(ChatColor.RED + "Sorry! In order to use this command, you need the following permission: 'mf.chat'");
            }
        }
    }

}
