package factionsystem.Commands;

import factionsystem.Faction;
import factionsystem.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;

import static factionsystem.UtilityFunctions.isInFaction;

public class InviteCommand {

    Main main = null;

    public InviteCommand(Main plugin) {
        main = plugin;
    }

    public boolean invitePlayer(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (isInFaction(player.getName(), main.factions)) {
                for (Faction faction : main.factions) {
                    if (faction.isOwner(player.getName()) || faction.isOfficer(player.getName())) {
                        if (args.length > 1) {

                            // invite if player isn't in a faction already
                            if (!(isInFaction(args[1], main.factions))) {
                                faction.invite(args[1]);
                                try {
                                    Player target = Bukkit.getServer().getPlayer(args[1]);
                                    target.sendMessage(ChatColor.GREEN + "You've been invited to " + faction.getName() + "! Type /mf join " + faction.getName() + " to join.");
                                } catch (Exception ignored) {

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
            else {
                player.sendMessage(ChatColor.RED + "You need to be in a faction to use this command.");
            }
        }
        return false;
    }

}
