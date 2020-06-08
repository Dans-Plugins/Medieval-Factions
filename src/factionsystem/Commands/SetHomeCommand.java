package factionsystem.Commands;

import factionsystem.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;

import static factionsystem.UtilityFunctions.getPlayersFaction;
import static factionsystem.UtilityFunctions.isInFaction;

public class SetHomeCommand {
    public static void setHome(CommandSender sender, ArrayList<Faction> factions) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (isInFaction(player.getName(), factions)) {
                Faction playersFaction = getPlayersFaction(player.getName(), factions);
                if (playersFaction.isOwner(player.getName()) || playersFaction.isOfficer(player.getName())) {
                    playersFaction.setFactionHome(player.getLocation());
                    player.sendMessage(ChatColor.GREEN + "Faction home set!");
                }
                else {
                    player.sendMessage(ChatColor.RED + "You need to be the owner of your faction or an officer of your faction to use this command.");
                }
            }
            else {
                player.sendMessage(ChatColor.RED + "You need to be in a faction to use this command.");
            }
        }
    }
}
