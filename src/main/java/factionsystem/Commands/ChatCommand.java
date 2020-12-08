package factionsystem.Commands;

import factionsystem.MedievalFactions;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static factionsystem.Subsystems.UtilitySubsystem.isInFaction;

public class ChatCommand extends Command {

    public ChatCommand() {
        super();
    }

    public void toggleFactionChat(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (player.hasPermission("mf.chat") || player.hasPermission("mf.default")) {
                if (isInFaction(player.getUniqueId(), MedievalFactions.getInstance().factions)) {
                    if (!MedievalFactions.getInstance().playersInFactionChat.contains(player.getUniqueId())) {
                        MedievalFactions.getInstance().playersInFactionChat.add(player.getUniqueId());
                        player.sendMessage(ChatColor.GREEN + "You are now speaking in faction chat!");
                    }
                    else {
                        MedievalFactions.getInstance().playersInFactionChat.remove(player.getUniqueId());
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
