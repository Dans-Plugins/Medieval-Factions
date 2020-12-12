package factionsystem.Commands;

import factionsystem.Objects.Faction;
import factionsystem.Data.PersistentData;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static factionsystem.Subsystems.UtilitySubsystem.createStringFromFirstArgOnwards;
import static factionsystem.Subsystems.UtilitySubsystem.sendFactionMembers;

public class MembersCommand {

    public void showMembers(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (sender.hasPermission("mf.members") || sender.hasPermission("mf.default")) {
                if (args.length == 1) {
                    for (Faction faction : PersistentData.getInstance().getFactions()) {
                        if (faction.isMember(player.getUniqueId())) {
                            sendFactionMembers(player, faction);
                            return;
                        }
                    }
                    player.sendMessage(ChatColor.RED + "You're not in a faction!");
                }
                else {
                    // creating name from arguments 1 to the last one
                    String name = createStringFromFirstArgOnwards(args);

                    for (Faction faction : PersistentData.getInstance().getFactions()) {
                        if (faction.getName().equalsIgnoreCase(name)) {
                            sendFactionMembers(player, faction);
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
