package factionsystem.Commands;

import factionsystem.ClaimedChunk;
import factionsystem.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;

import static factionsystem.UtilityFunctions.*;

public class SetHomeCommand {
    public static void setHome(CommandSender sender, ArrayList<Faction> factions, ArrayList<ClaimedChunk> claimedChunks) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (isInFaction(player.getName(), factions)) {
                Faction playersFaction = getPlayersFaction(player.getName(), factions);
                if (playersFaction.isOwner(player.getName()) || playersFaction.isOfficer(player.getName())) {

                    if (isClaimed(player.getLocation().getChunk(), claimedChunks)) {
                        ClaimedChunk chunk = getClaimedChunk(player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ(), claimedChunks);
                        if (chunk.getHolder().equalsIgnoreCase(playersFaction.getName())) {
                            playersFaction.setFactionHome(player.getLocation());
                            player.sendMessage(ChatColor.GREEN + "Faction home set!");
                        }
                        else {
                            player.sendMessage(ChatColor.RED + "You can't set your faction home on land your faction hasn't claimed!");
                        }
                    }
                    else {
                        player.sendMessage(ChatColor.RED + "This land isn't claimed!");
                    }

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
