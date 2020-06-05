package plugin.Commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import plugin.Faction;

import java.util.ArrayList;

import static plugin.Main.createStringFromFirstArgOnwards;
import static plugin.Main.sendFactionMembers;

public class MembersCommand {

    public static void showMembers(CommandSender sender, String[] args, ArrayList<Faction> factions) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length == 1) {
                for (Faction faction : factions) {
                    if (faction.isMember(player.getName())) {
                        sendFactionMembers(player, faction);
                    }
                }
            }
            else {
                // creating name from arguments 1 to the last one
                String name = createStringFromFirstArgOnwards(args);

                for (Faction faction : factions) {
                    if (faction.getName().equalsIgnoreCase(name)) {
                        sendFactionMembers(player, faction);
                    }
                }
            }
        }
    }

}
