package factionsystem.Commands;

import factionsystem.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;

import static factionsystem.UtilityFunctions.isInFaction;
import static org.bukkit.Bukkit.getServer;

public class PromoteCommand {
    public static void promotePlayer(CommandSender sender, String[] args, ArrayList<Faction> factions) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (isInFaction(player.getName(), factions)) {
                if (args.length > 1) {
                    for (Faction faction : factions) {
                        if (faction.isMember(args[1])) {
                            if (faction.isOwner(player.getName())) {
                                if (faction.isMember(args[1])) {
                                    faction.addOfficer(args[1]);
                                    player.sendMessage(ChatColor.GREEN + "Player promoted!");

                                    try {
                                        Player target = getServer().getPlayer(args[1]);
                                        target.sendMessage(ChatColor.GREEN + "You have been promoted to officer status in your faction!");
                                    }
                                    catch(Exception ignored) {

                                    }
                                }
                                else {
                                    player.sendMessage(ChatColor.RED + "That player isn't a member of your faction!");
                                }

                                return;
                            }
                        }
                    }
                }
                else {
                    player.sendMessage(ChatColor.RED + "Usage: /mf promote (player-name)");
                }
            }
            else {
                player.sendMessage(ChatColor.RED + "You need to be in a faction to use this command.");
            }
        }
    }
}
