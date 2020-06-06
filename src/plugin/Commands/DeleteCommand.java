package plugin.Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import plugin.ClaimedChunk;
import plugin.Faction;

import java.util.ArrayList;

import static plugin.Main.removeAllClaimedChunks;

public class DeleteCommand {

    public static boolean deleteFaction(CommandSender sender, ArrayList<Faction> factions, ArrayList<ClaimedChunk> chunks) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            boolean owner = false;
            for (int i = 0; i < factions.size(); i++) {
                if (factions.get(i).isOwner(player.getName())) {
                    owner = true;
                    if (factions.get(i).getPopulation() == 1) {
                        factions.remove(i);
                        player.sendMessage(ChatColor.AQUA + "Faction successfully deleted.");

                        // remove claimed land objects associated with this faction
                        removeAllClaimedChunks(factions.get(i).getName(), chunks);

                        return true;
                    }
                    else {
                        player.sendMessage(ChatColor.RED + "You need to kick all players before you can delete your faction.");
                        return false;
                    }
                }
            }

            if (!owner) {
                player.sendMessage(ChatColor.RED + "You need to be the owner of a faction to use this command.");
                return false;
            }
        }
        return false;
    }
}
