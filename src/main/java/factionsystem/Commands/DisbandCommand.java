package factionsystem.Commands;

import factionsystem.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;

import static factionsystem.Utility.UtilityFunctions.removeAllClaimedChunks;
import static factionsystem.Utility.UtilityFunctions.removeAllLocks;

public class DisbandCommand {

    Main main = null;

    public DisbandCommand(Main plugin) {
        main = plugin;
    }

    public boolean deleteFaction(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            boolean owner = false;
            for (int i = 0; i < main.factions.size(); i++) {
                if (main.factions.get(i).isOwner(player.getName())) {
                    owner = true;
                    if (main.factions.get(i).getPopulation() == 1) {

                        // delete file associated with faction
                        System.out.println("Attempting to delete file plugins/medievalfactions/" + main.factions.get(i).getName() + ".txt");
                        try {
                            File fileToDelete = new File("plugins/medievalfactions/" + main.factions.get(i).getName() + ".txt");
                            if (fileToDelete.delete()) {
                                System.out.println("Success. File deleted.");
                            }
                            else {
                                System.out.println("There was a problem deleting the file.");
                            }
                        } catch(Exception e) {
                            System.out.println("An error has occurred during file deletion.");
                        }

                        // remove claimed land objects associated with this faction
                        removeAllClaimedChunks(main.factions.get(i).getName(), main.claimedChunks);

                        // remove locks associated with this faction
                        removeAllLocks(main.factions.get(i).getName(), main.lockedBlocks);

                        main.factions.remove(i);
                        player.sendMessage(ChatColor.GREEN + "Faction successfully disbanded.");

                        return true;
                    }
                    else {
                        player.sendMessage(ChatColor.RED + "You need to kick all players before you can disband your faction.");
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
