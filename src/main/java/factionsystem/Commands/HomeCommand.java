package factionsystem.Commands;

import factionsystem.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;

import static factionsystem.UtilityFunctions.getPlayersFaction;
import static factionsystem.UtilityFunctions.isInFaction;

public class HomeCommand {
    public static void teleportPlayer(CommandSender sender, ArrayList<Faction> factions) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (isInFaction(player.getName(), factions)) {
                Faction playersFaction = getPlayersFaction(player.getName(), factions);
                if (playersFaction.getFactionHome() != null) {
                    player.sendMessage(ChatColor.GREEN + "Teleporting.");
                    player.teleport(playersFaction.getFactionHome());
                }
                else {
                    player.sendMessage(ChatColor.RED + "The faction home isn't set yet.");
                }
            }
            else {
                player.sendMessage(ChatColor.RED + "You need to be in a faction to use this command.");
            }
        }
    }
}
