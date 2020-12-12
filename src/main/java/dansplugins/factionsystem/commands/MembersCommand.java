package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.domainobjects.Faction;
import dansplugins.factionsystem.utils.Utilities;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MembersCommand {

    public void showMembers(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (sender.hasPermission("mf.members") || sender.hasPermission("mf.default")) {
                if (args.length == 1) {
                    for (Faction faction : PersistentData.getInstance().getFactions()) {
                        if (faction.isMember(player.getUniqueId())) {
                            Utilities.sendFactionMembers(player, faction);
                            return;
                        }
                    }
                    player.sendMessage(ChatColor.RED + "You're not in a faction!");
                }
                else {
                    // creating name from arguments 1 to the last one
                    String name = Utilities.createStringFromFirstArgOnwards(args);

                    for (Faction faction : PersistentData.getInstance().getFactions()) {
                        if (faction.getName().equalsIgnoreCase(name)) {
                            Utilities.sendFactionMembers(player, faction);
                            return;
                        }
                    }
                    player.sendMessage(ChatColor.RED + "That faction name isn't recognized!");
                }
            }
            else {
                sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.members'");
            }
        }
    }

}
