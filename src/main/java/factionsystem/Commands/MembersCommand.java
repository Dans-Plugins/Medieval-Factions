package factionsystem.Commands;

import factionsystem.Main;
import factionsystem.Objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static factionsystem.Subsystems.UtilitySubsystem.*;

public class MembersCommand {

    Main main = null;

    public MembersCommand(Main plugin) {
        main = plugin;
    }

    public void showMembers(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (isInFaction(player.getName(), main.factions)) {
                if (args.length == 1) {
                    for (Faction faction : main.factions) {
                        if (faction.isMember(player.getName())) {
                            sendFactionMembers(player, faction);
                        }
                    }
                }
                else {
                    // creating name from arguments 1 to the last one
                    String name = createStringFromFirstArgOnwards(args);

                    for (Faction faction : main.factions) {
                        if (faction.getName().equalsIgnoreCase(name)) {
                            sendFactionMembers(player, faction);
                        }
                    }
                }
            }
            else {
                player.sendMessage(ChatColor.RED + "You need to be in a faction to use this command.");
            }
        }
    }

}
