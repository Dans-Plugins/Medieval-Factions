package factionsystem.Commands;

import factionsystem.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static factionsystem.Subsystems.UtilitySubsystem.isInFaction;

public class ChatCommand {

    Main main = null;

    public ChatCommand(Main plugin) {
        main = plugin;
    }

    public void toggleFactionChat(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (player.hasPermission("mf.chat") || player.hasPermission("mf.default")) {
                if (isInFaction(player.getName(), main.factions)) {
                    if (!main.playersInFactionChat.contains(player.getName())) {
                        main.playersInFactionChat.add(player.getName());
                        player.sendMessage(ChatColor.GREEN + "You are now speaking in faction chat!");
                    }
                    else {
                        main.playersInFactionChat.remove(player.getName());
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
