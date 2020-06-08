package factionsystem.Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import factionsystem.ClaimedChunk;
import factionsystem.Faction;

import java.io.File;
import java.util.ArrayList;

import static factionsystem.UtilityFunctions.removeAllClaimedChunks;

public class DeleteCommand {

    public static boolean deleteFaction(CommandSender sender, ArrayList<Faction> factions, ArrayList<ClaimedChunk> chunks) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            boolean owner = false;
            for (int i = 0; i < factions.size(); i++) {
                if (factions.get(i).isOwner(player.getName())) {
                    owner = true;
                    if (factions.get(i).getPopulation() == 1) {

                        // delete file associated with faction
                        System.out.println("Attempting to delete file plugins/medievalfactions/" + factions.get(i).getName() + ".txt");
                        try {
                            File fileToDelete = new File("plugins/medievalfactions/" + factions.get(i).getName() + ".txt");
                            if (fileToDelete.delete()) {
                                System.out.println("Success. File deleted.");
                            }
                            else {
                                System.out.println("There was a problem deleting the file.");
                            }
                        } catch(Exception e) {
                            System.out.println("An error has occurred during file deletion.");
                            e.printStackTrace();
                        }

                        // remove claimed land objects associated with this faction
                        removeAllClaimedChunks(factions.get(i).getName(), chunks);

                        factions.remove(i);
                        player.sendMessage(ChatColor.AQUA + "Faction successfully deleted.");

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
