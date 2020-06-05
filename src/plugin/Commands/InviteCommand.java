package plugin.Commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import plugin.Faction;

import java.util.ArrayList;

import static plugin.Main.isInFaction;

public class InviteCommand {

    public static boolean invitePlayer(CommandSender sender, String[] args, ArrayList<Faction> factions) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            for (Faction faction : factions) {
                if (faction.isOwner(player.getName())) {
                    if (args.length > 1) {

                        // invite if player isn't in a faction already
                        if (!(isInFaction(args[1], factions))) {
                            faction.invite(args[1]);
                            try {
                                Player target = Bukkit.getServer().getPlayer(args[1]);
                                target.sendMessage(ChatColor.GREEN + "You've been invited to " + faction.getName() + "! Type /mf join " + faction.getName() + " to join.");
                            } catch (Exception e) {

                            }
                            player.sendMessage(ChatColor.GREEN + "Invitation sent!");
                            return true;
                        }
                        else {
                            player.sendMessage(ChatColor.RED + "That player is already in a faction, sorry!");
                            return false;
                        }


                    }
                    else {
                        player.sendMessage(ChatColor.RED + "Usage: /mf invite (player-name)");
                        return false;
                    }
                }
            }
        }
        return false;
    }

}
