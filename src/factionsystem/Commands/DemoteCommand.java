package factionsystem.Commands;

import factionsystem.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class DemoteCommand {
    public static void demotePlayer(CommandSender sender, String[] args, ArrayList<Faction> factions) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length > 1) {
                for (Faction faction : factions) {
                    if (faction.isOfficer(args[1])) {
                        if (faction.isOwner(player.getName())) {
                            faction.removeOfficer(args[1]);
                            player.sendMessage(ChatColor.GREEN + "Player demoted!");
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
