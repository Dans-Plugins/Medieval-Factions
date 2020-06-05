package plugin.Commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import plugin.Faction;

import java.util.ArrayList;

import static plugin.Main.createStringFromFirstArgOnwards;

public class DeclareWarCommand {
    public static void declareWar(CommandSender sender, String[] args, ArrayList<Faction> factions) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            for (Faction faction : factions) {

                // if player is the owner
                if (faction.isOwner(player.getName())) {

                    // if there's more than one argument
                    if (args.length > 1) {

                        // get name of faction
                        String factionName = createStringFromFirstArgOnwards(args);

                        // check if faction exists
                        for (int i = 0; i < factions.size(); i++) {
                            if (factions.get(i).getName().equalsIgnoreCase(factionName)) {

                                // add enemy
                                faction.addEnemy(factionName);

                            }
                        }

                    }
                }
            }
        }
    }
}
