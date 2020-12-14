package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.ChunkManager;
import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.ClaimedChunk;
import dansplugins.factionsystem.objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static org.bukkit.Bukkit.getServer;

public class HomeCommand {

    public void teleportPlayer(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (sender.hasPermission("mf.home") || sender.hasPermission("mf.default")) {
                if (PersistentData.getInstance().isInFaction(player.getUniqueId())) {
                    Faction playersFaction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());
                    if (playersFaction.getFactionHome() != null) {

                        // Check that factionHome is in it's own factions land and not claimed by someone else.
                        Chunk homeChunk = playersFaction.getFactionHome().getBlock().getChunk();
                        if (ChunkManager.getInstance().isClaimed(homeChunk, PersistentData.getInstance().getClaimedChunks())){
                            // Ensure is in your faction
                            ClaimedChunk claimedHomeChunk = ChunkManager.getInstance().getClaimedChunk(homeChunk.getX(), homeChunk.getZ(), homeChunk.getWorld().getName(), PersistentData.getInstance().getClaimedChunks());
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

                        getServer().getScheduler().runTaskLater(MedievalFactions.getInstance(), new Runnable() {
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
            else {
                sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.home'");
            }
        }
    }
}
