package factionsystem.Commands;

import factionsystem.ClaimedChunk;
import factionsystem.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;

import static factionsystem.UtilityFunctions.*;

public class InfoCommand {

    public static void showInfo(CommandSender sender, String[] args, ArrayList<Faction> factions, ArrayList<ClaimedChunk> chunks) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length == 1) {
                for (Faction faction : factions) {
                    if (faction.isMember(player.getName())) {
                        sendFactionInfo(player, faction, getChunksClaimedByFaction(faction.getName(), chunks));
                    }
                }
            }
            else {
                // creating name from arguments 1 to the last one
                String name = createStringFromFirstArgOnwards(args);

                boolean exists = false;
                for (Faction faction : factions) {
                    if (faction.getName().equals(name)) {
                        exists = true;
                        sendFactionInfo(player, faction, getChunksClaimedByFaction(faction.getName(), chunks));
                    }
                }
                if (!exists) {
                    player.sendMessage(ChatColor.RED + "That faction wasn't found!");
                }
            }
        }
    }

}
