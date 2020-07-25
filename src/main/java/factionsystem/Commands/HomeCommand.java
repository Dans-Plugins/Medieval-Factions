package factionsystem.Commands;

import factionsystem.Main;
import factionsystem.Objects.ClaimedChunk;
import factionsystem.Objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static factionsystem.Subsystems.UtilitySubsystem.*;
import static org.bukkit.Bukkit.getServer;

public class HomeCommand {

    Main main = null;

    public HomeCommand(Main plugin) {
        main = plugin;
    }

    public void teleportPlayer(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (isInFaction(player.getUniqueId(), main.factions)) {
                Faction playersFaction = getPlayersFaction(player.getUniqueId(), main.factions);
                if (playersFaction.getFactionHome() != null) {

                    // Check that factionHome is in it's own factions land and not claimed by someone else.
                    Chunk homeChunk = playersFaction.getFactionHome().getBlock().getChunk();
                    if (isClaimed(homeChunk, main.claimedChunks)){
                        // Ensure is in your faction
                        ClaimedChunk claimedHomeChunk = getClaimedChunk(homeChunk.getX(), homeChunk.getZ(), main.claimedChunks);
                        if (claimedHomeChunk.getHolder() != null && !playersFaction.getName().equals(claimedHomeChunk.getHolder())) {
                            // Area is claimed by someone else and cannot be home. Cancel teleport and return;
                            player.sendMessage(ChatColor.RED + "Home was claimed by another faction, and has been lost.");
                            return;
                        }
                    } else {
                        // Area isn't claimed cannot be home. Cancel teleport and return;
                        player.sendMessage(ChatColor.RED + "Home is in an unclaimed chunk, and is unavailable");
                        return;
                    }


                    player.sendMessage(ChatColor.GREEN + "Teleporting in 3 seconds...");
                    int seconds = 3;

                    Location initialLocation = player.getLocation();

                    getServer().getScheduler().runTaskLater(main, new Runnable() {
                        @Override
                        public void run() {
                            if (initialLocation.getX() == player.getLocation().getX() &&
                                initialLocation.getY() == player.getLocation().getY() &&
                                initialLocation.getZ() == player.getLocation().getZ()) {

                                // teleport the player
                                player.teleport(playersFaction.getFactionHome());

                            }
                            else {
                                player.sendMessage(ChatColor.RED + "Movement Detected. Teleport cancelled.");
                            }

                        }
                    }, seconds * 20);

                }
                else {
                    player.sendMessage(ChatColor.RED + "The faction home isn't set yet.");
                }
            }
            else {
                player.sendMessage(ChatColor.RED + "You need to be in a faction to use this command.");
            }
        }
    }
}
