package plugin.Commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import plugin.Faction;

import java.util.ArrayList;

public class KickCommand {

    public static boolean kickPlayer(CommandSender sender, String[] args, ArrayList<Faction> factions) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length > 1) {
                boolean owner = false;
                for (Faction faction : factions) {
                    if (faction.isOwner(player.getName())) {
                        owner = true;
                        if (faction.isMember(args[1])) {
                            if (!(args[1].equalsIgnoreCase(player.getName()))) {
                                faction.removeMember(args[1]);
                                player.sendMessage(ChatColor.AQUA + args[1] + " kicked.");
                                try {
                                    Player target = Bukkit.getServer().getPlayer(args[1]);
                                    target.sendMessage(ChatColor.RED + "You have been kicked from your faction by " + player.getName() + ".");
                                } catch (Exception ignored) {

                                }
                                return true;
                            }
                            else {
                                player.sendMessage(ChatColor.RED + "You can't kick yourself.");
                                return false;
                            }

                        }
                    }
                }

                if (!owner) {
                    player.sendMessage(ChatColor.RED + "You need to be the owner of a faction to use this command.");
                    return false;
                }

            } else {
                player.sendMessage(ChatColor.RED + "Usage: /mf kick (player-name)");
                return false;
            }
        }
        return false;
    }

}
