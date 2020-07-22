package factionsystem.Commands;

import factionsystem.Main;
import factionsystem.Objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static factionsystem.Subsystems.UtilitySubsystem.*;

public class InfoCommand {

    Main main = null;

    public InfoCommand(Main plugin) {
        main = plugin;
    }

    public void showInfo(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
           Player player = (Player) sender;
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
                    if (faction.getName().equalsIgnoreCase(name)) {
                        exists = true;
                        sendFactionInfo(player, faction, getChunksClaimedByFaction(faction.getName(), main.claimedChunks));
                    }
                }
                if (!exists) {
                    player.sendMessage(ChatColor.RED + "That faction wasn't found!");
                }
            }
        }

    }

}
