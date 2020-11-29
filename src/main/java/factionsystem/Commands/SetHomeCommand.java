package factionsystem.Commands;

import factionsystem.MedievalFactions;
import factionsystem.Objects.ClaimedChunk;
import factionsystem.Objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static factionsystem.Subsystems.UtilitySubsystem.*;

public class SetHomeCommand extends Command {

    public SetHomeCommand(MedievalFactions plugin) {
        super(plugin);
    }

    public void setHome(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (isInFaction(player.getUniqueId(), main.factions)) {
                Faction playersFaction = getPlayersFaction(player.getUniqueId(), main.factions);
                if (playersFaction.isOwner(player.getUniqueId()) || playersFaction.isOfficer(player.getUniqueId())) {

                    if (isClaimed(player.getLocation().getChunk(), main.claimedChunks)) {
                        ClaimedChunk chunk = getClaimedChunk(player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ(), player.getWorld().getName(), main.claimedChunks);
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
