package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.Utilities;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WhoCommand {

    public void sendInformation(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (sender.hasPermission("mf.who") || sender.hasPermission("mf.default")) {
                if (args.length > 1) {
                    String name = Utilities.createStringFromFirstArgOnwards(args);
                    Faction faction = Utilities.getPlayersFaction(Utilities.findUUIDBasedOnPlayerName(name), PersistentData.getInstance().getFactions());
                    if (faction != null) {
                        Utilities.sendFactionInfo(player, faction, Utilities.getChunksClaimedByFaction(faction.getName(), PersistentData.getInstance().getClaimedChunks()));
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
