package factionsystem.Commands;

import factionsystem.Objects.Faction;
import factionsystem.Data.PersistentData;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static factionsystem.Util.Utilities.*;

public class InfoCommand {

    public void showInfo(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
           Player player = (Player) sender;

            if (sender.hasPermission("mf.info") || sender.hasPermission("mf.default")) {
                if (args.length == 1) {
                    for (Faction faction : PersistentData.getInstance().getFactions()) {
                        if (faction.isMember(player.getUniqueId())) {
                            sendFactionInfo(player, faction, getChunksClaimedByFaction(faction.getName(), PersistentData.getInstance().getClaimedChunks()));
                            return;
                        }
                    }
                    player.sendMessage(ChatColor.RED + "You're not in a faction!");
                }
                else {
                    // creating name from arguments 1 to the last one
                    String name = createStringFromFirstArgOnwards(args);

                    boolean exists = false;
                    for (Faction faction : PersistentData.getInstance().getFactions()) {
                        if (faction.getName().equalsIgnoreCase(name)) {
                            exists = true;
                            sendFactionInfo(player, faction, getChunksClaimedByFaction(faction.getName(), PersistentData.getInstance().getClaimedChunks()));
                        }
                    }
                    if (!exists) {
                        player.sendMessage(ChatColor.RED + "That faction wasn't found!");
                    }
                }
            }
            else {
                sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.info'");
            }
        }

    }

}
