package factionsystem.Commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import factionsystem.Faction;

import java.util.ArrayList;

import static factionsystem.Main.*;

public class JoinCommand {

    public static boolean joinFaction(CommandSender sender, String[] args, ArrayList<Faction> factions) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length > 1) {

                // creating name from arguments 1 to the last one
                String factionName = createStringFromFirstArgOnwards(args);

                for (Faction faction : factions) {
                    if (faction.getName().equalsIgnoreCase(factionName)) {
                        if (faction.isInvited(player.getName())) {

                            // join if player isn't in a faction already
                            if (!(isInFaction(player.getName(), factions))) {
                                faction.addMember(player.getName());
                                faction.uninvite(player.getName());
                                try {
                                    sendAllPlayersInFactionMessage(faction, ChatColor.GREEN + player.getName() + " has joined " + faction.getName());
                                } catch (Exception ignored) {

                                }
                                player.sendMessage(ChatColor.GREEN + "You joined the faction!");
                                return true;
                            }
                            else {
                                player.sendMessage(ChatColor.RED + "You're already in a faction, sorry!");
                                return false;
                            }

                        } else {
                            player.sendMessage(ChatColor.RED + "You're not invited to this faction!");
                            return false;
                        }
                    }
                }
            } else {
                player.sendMessage(ChatColor.RED + "Usage: /mf join (faction-name)");
                return false;
            }
        }
        return false;
    }

}
