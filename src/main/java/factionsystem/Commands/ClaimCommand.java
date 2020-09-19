package factionsystem.Commands;

import factionsystem.Main;
import factionsystem.Objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static factionsystem.Subsystems.UtilitySubsystem.*;

public class ClaimCommand {

    Main main = null;

    public ClaimCommand(Main plugin) {
        main = plugin;
    }

    public boolean claimChunk(CommandSender sender, String[] args) {
        if (sender.hasPermission("mf.claim") || sender.hasPermission("mf.default")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;

                // if not at demesne limit
                if (isInFaction(player.getUniqueId(), main.factions)) {
                    Faction playersFaction = getPlayersFaction(player.getUniqueId(), main.factions);
                    if (getChunksClaimedByFaction(playersFaction.getName(), main.claimedChunks) < playersFaction.getCumulativePowerLevel()) {
                        main.utilities.addChunkAtPlayerLocation(player);
                        return true;
                    }
                    else {
                        player.sendMessage(ChatColor.RED + "You have reached your demesne limit! Invite more players to increase this.");
                        return false;
                    }
                }
                else {
                    player.sendMessage(ChatColor.RED + "You must be in a faction to use this command.");
                    return false;
                }
            }
        }
        else {
            sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.claim'");
            return false;
        }
        return false;
    }

}
