package factionsystem.Commands;

import factionsystem.ClaimedChunk;
import factionsystem.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;

import static factionsystem.UtilityFunctions.*;

public class WhoCommand {

    public static void sendInformation(CommandSender sender, String[] args, ArrayList<Faction> factions) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length > 1) {
                String name = createStringFromFirstArgOnwards(args);
                Faction faction = getPlayersFaction(name, factions);
                sendFactionInfo(player, faction, faction.getCumulativePowerLevel());
            }
            else {
                player.sendMessage(ChatColor.RED + "Usage: /mf who (player-name)");
            }

        }
    }

}
