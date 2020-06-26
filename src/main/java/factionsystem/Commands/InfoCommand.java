package factionsystem.Commands;

import factionsystem.Objects.Faction;
import factionsystem.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static factionsystem.Utility.UtilityFunctions.*;

public class InfoCommand {

    Main main = null;

    public InfoCommand(Main plugin) {
        main = plugin;
    }

    public void showInfo(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (isInFaction(player.getName(), main.factions)) {
                if (args.length == 1) {
                    for (Faction faction : main.factions) {
                        if (faction.isMember(player.getName())) {
                            sendFactionInfo(player, faction, getChunksClaimedByFaction(faction.getName(), main.claimedChunks));
                        }
                    }
                }
                else {
                    // creating name from arguments 1 to the last one
                    String name = createStringFromFirstArgOnwards(args);

                    boolean exists = false;
                    for (Faction faction : main.factions) {
                        if (faction.getName().equals(name)) {
                            exists = true;
                            sendFactionInfo(player, faction, getChunksClaimedByFaction(faction.getName(), main.claimedChunks));
                        }
                    }
                    if (!exists) {
                        player.sendMessage(ChatColor.RED + "That faction wasn't found!");
                    }
                }
            }
            else {
                player.sendMessage(ChatColor.RED + "You need to be in a faction to use /mf info. To view an existing faction's info page, type /mf info (player-name).");
            }
        }

    }

}
