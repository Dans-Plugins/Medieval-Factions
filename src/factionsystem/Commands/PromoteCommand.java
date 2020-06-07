package factionsystem.Commands;

import factionsystem.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class PromoteCommand {
    public static void promotePlayer(CommandSender sender, String[] args, ArrayList<Faction> factions) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length > 1) {
                for (Faction faction : factions) {
                    if (faction.isMember(args[1])) {
                        if (faction.isOwner(player.getName())) {
                            faction.addOfficer(args[1]);
                            player.sendMessage(ChatColor.GREEN + "Player promoted!");
                            return;
                        }
                    }
                }
            }
            else {
                player.sendMessage(ChatColor.RED + "Usage: /mf promote (player-name)");
            }
        }
    }
}
