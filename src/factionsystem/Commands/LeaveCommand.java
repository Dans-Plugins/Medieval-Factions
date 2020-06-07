package factionsystem.Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import factionsystem.ClaimedChunk;
import factionsystem.Faction;

import java.io.File;
import java.util.ArrayList;

import static factionsystem.Main.removeAllClaimedChunks;
import static factionsystem.Main.sendAllPlayersInFactionMessage;

public class LeaveCommand {

    public static boolean leaveFaction(CommandSender sender, ArrayList<Faction> factions, ArrayList<ClaimedChunk> chunks) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            for (int i = 0; i < factions.size(); i++) {
                if (factions.get(i).isMember(player.getName())) {
                    if (factions.get(i).isOwner(player.getName())) {
                        // is faction empty?
                        if (factions.get(i).getPopulation() == 1) {
                            // able to leave

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

                            factions.get(i).removeMember(player.getName());
                            factions.remove(i);
                            player.sendMessage(ChatColor.AQUA + "You left your faction. It was deleted since no one else was a member.");

                            return true;
                        }
                        else {
                            player.sendMessage(ChatColor.RED + "Sorry! You must transfer ownership or kick everyone in your faction to leave.");
                            return false;
                        }
                    }
                    else {
                        // able to leave
                        factions.get(i).removeMember(player.getName());
                        player.sendMessage(ChatColor.AQUA + "You left your faction.");
                        try {
                            sendAllPlayersInFactionMessage(factions.get(i), ChatColor.GREEN + player.getName() + " has left " + factions.get(i).getName());
                        } catch (Exception ignored) {

                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
