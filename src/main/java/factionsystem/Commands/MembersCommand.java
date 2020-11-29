package factionsystem.Commands;

import factionsystem.MedievalFactions;
import factionsystem.Objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static factionsystem.Subsystems.UtilitySubsystem.*;

public class MembersCommand {

    MedievalFactions main = null;

    public MembersCommand(MedievalFactions plugin) {
        main = plugin;
    }

    public void showMembers(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length == 1) {
                for (Faction faction : main.factions) {
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

                for (Faction faction : main.factions) {
                    if (faction.getName().equalsIgnoreCase(name)) {
                        sendFactionMembers(player, faction);
                        return;
                    }
                }
                player.sendMessage(ChatColor.RED + "That faction name isn't recognized!");
            }
        }
    }

}
