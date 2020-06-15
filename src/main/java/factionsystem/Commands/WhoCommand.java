package factionsystem.Commands;

import factionsystem.ClaimedChunk;
import factionsystem.Faction;
import factionsystem.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;

import static factionsystem.UtilityFunctions.*;

public class WhoCommand {

    Main main = null;

    public WhoCommand(Main plugin) {
        main = plugin;
    }

    public void sendInformation(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length > 1) {
                String name = createStringFromFirstArgOnwards(args);
                Faction faction = getPlayersFaction(name, main.factions);
                if (faction != null) {
                    sendFactionInfo(player, faction, faction.getCumulativePowerLevel());
                }
                else {
                    player.sendMessage(ChatColor.RED + "That player isn't in a faction.");
                }

            }
            else {
                player.sendMessage(ChatColor.RED + "Usage: /mf who (player-name)");
            }

        }
    }

}
