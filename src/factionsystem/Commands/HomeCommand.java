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
                player.teleport(playersFaction.getFactionHome());
                player.sendMessage(ChatColor.GREEN + "Teleporting.");
            }
            else {
                player.sendMessage(ChatColor.RED + "You need to be in a faction to use this command.");
            }
        }
    }
}
