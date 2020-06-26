package factionsystem.Commands;

import factionsystem.Objects.ClaimedChunk;
import factionsystem.Objects.Faction;
import factionsystem.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static factionsystem.Utility.UtilityFunctions.*;

public class SetHomeCommand {

    Main main = null;

    public SetHomeCommand(Main plugin) {
        main = plugin;
    }

    public void setHome(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (isInFaction(player.getName(), main.factions)) {
                Faction playersFaction = getPlayersFaction(player.getName(), main.factions);
                if (playersFaction.isOwner(player.getName()) || playersFaction.isOfficer(player.getName())) {

                    if (isClaimed(player.getLocation().getChunk(), main.claimedChunks)) {
                        ClaimedChunk chunk = getClaimedChunk(player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ(), main.claimedChunks);
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
