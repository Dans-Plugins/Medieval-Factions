package plugin.Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import plugin.Faction;

import java.util.ArrayList;

public class LeaveCommand {

    public static boolean leaveFaction(CommandSender sender, ArrayList<Faction> factions) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            for (int i = 0; i < factions.size(); i++) {
                if (factions.get(i).isMember(player.getName())) {
                    if (factions.get(i).isOwner(player.getName())) {
                        // is faction empty?
                        if (factions.get(i).getPopulation() == 1) {
                            // able to leave
                            factions.get(i).removeMember(player.getName());
                            factions.remove(i);
                            player.sendMessage(ChatColor.AQUA + "You left your faction. It was deleted since no one else was a member.");
                            return true;
                        }
                        else {
                            player.sendMessage(ChatColor.RED + "Sorry! You must transfer ownership or kick everyone in your faction to leave.");
                            return false;
                        }
                    }
                    else {
                        // able to leave
                        factions.get(i).removeMember(player.getName());
                        player.sendMessage(ChatColor.AQUA + "You left your faction.");
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
