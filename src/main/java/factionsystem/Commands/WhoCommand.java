package factionsystem.Commands;

import factionsystem.MedievalFactions;
import factionsystem.Objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static factionsystem.Subsystems.UtilitySubsystem.*;

public class WhoCommand extends Command {

    public WhoCommand() {
        super();
    }

    public void sendInformation(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (sender.hasPermission("mf.who") || sender.hasPermission("mf.default")) {
                if (args.length > 1) {
                    String name = createStringFromFirstArgOnwards(args);
                    Faction faction = getPlayersFaction(findUUIDBasedOnPlayerName(name), MedievalFactions.getInstance().factions);
                    if (faction != null) {
                        sendFactionInfo(player, faction, getChunksClaimedByFaction(faction.getName(), MedievalFactions.getInstance().claimedChunks));
                    }
                    else {
                        player.sendMessage(ChatColor.RED + "That player isn't in a faction.");
                    }

                }
                else {
                    player.sendMessage(ChatColor.RED + "Usage: /mf who (player-name)");
                }
            }
            else {
                sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.who'");
            }
        }
    }

}
