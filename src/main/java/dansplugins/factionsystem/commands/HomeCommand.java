package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.ChunkManager;
import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.ClaimedChunk;
import dansplugins.factionsystem.objects.Faction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static org.bukkit.Bukkit.getServer;

public class HomeCommand extends SubCommand {

    public HomeCommand() {
        super(new String[]{
                "home", LOCALE_PREFIX + "CmdHome"
        }, true, true);
    }

    /**
     * Method to execute the command for a player.
     *
     * @param player who sent the command.
     * @param args   of the command.
     * @param key    of the sub-command (e.g. Ally).
     */
    @Override
    public void execute(Player player, String[] args, String key) {
        if (!(checkPermissions(player, "mf.home"))) return;
        if (faction.getFactionHome() == null) {
            player.sendMessage(translate("&c" + getText("FactionHomeNotSetYet")));
            return;
        }
        final Chunk home_chunk;
        if (!chunks.isClaimed(home_chunk = faction.getFactionHome().getChunk(), data.getClaimedChunks())) {
            player.sendMessage(translate("&c" + getText("HomeIsInUnclaimedChunk")));
            return;
        }
        ClaimedChunk chunk = chunks.getClaimedChunk(home_chunk, data.getClaimedChunks());
        if (chunk == null || chunk.getHolder() == null) {
            player.sendMessage(translate("&c" + getText("HomeIsInUnclaimedChunk")));
            return;
        }
        if (!chunk.getHolder().equalsIgnoreCase(faction.getName())) {
            player.sendMessage(translate("&c" + getText("HomeClaimedByAnotherFaction")));
            return;
        }
        final int teleport_delay = 3;
        player.sendMessage(translate("&a" + getText("TeleportingAlert")));
        final Location initialLocation = player.getLocation();
        Bukkit.getScheduler().runTaskLater(MedievalFactions.getInstance(), () -> {
            if (    initialLocation.getX() == player.getLocation().getX()   &&
                    initialLocation.getY() == player.getLocation().getY()   &&
                    initialLocation.getZ() == player.getLocation().getZ()   ) {
                // teleport the player
                player.teleport(faction.getFactionHome());
            } else {
                player.sendMessage(translate("&c" + getText("MovementDetectedTeleportCancelled")));
            }

        }, teleport_delay * 20);
    }

    /**
     * Method to execute the command.
     *
     * @param sender who sent the command.
     * @param args   of the command.
     * @param key    of the command.
     */
    @Override
    public void execute(CommandSender sender, String[] args, String key) {

    }

    @Deprecated
    public void teleportPlayer(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (sender.hasPermission("mf.home")) {
                if (PersistentData.getInstance().isInFaction(player.getUniqueId())) {
                    Faction playersFaction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());
                    if (playersFaction.getFactionHome() != null) {

                        // Check that factionHome is in it's own factions land and not claimed by someone else.
                        Chunk homeChunk = playersFaction.getFactionHome().getBlock().getChunk();
                        if (ChunkManager.getInstance().isClaimed(homeChunk, PersistentData.getInstance().getClaimedChunks())) {
                            // Ensure is in your faction
                            ClaimedChunk claimedHomeChunk = ChunkManager.getInstance().getClaimedChunk(homeChunk.getX(), homeChunk.getZ(), homeChunk.getWorld().getName(), PersistentData.getInstance().getClaimedChunks());
                            if (claimedHomeChunk.getHolder() != null && !playersFaction.getName().equals(claimedHomeChunk.getHolder())) {
                                // Area is claimed by someone else and cannot be home. Cancel teleport and return;
                                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HomeClaimedByAnotherFaction"));
                                return;
                            }
                        } else {
                            // Area isn't claimed cannot be home. Cancel teleport and return;
                            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HomeIsInUnclaimedChunk"));
                            return;
                        }


                        player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("TeleportingAlert"));
                        int seconds = 3;

                        Location initialLocation = player.getLocation();

                        getServer().getScheduler().runTaskLater(MedievalFactions.getInstance(), new Runnable() {
                            @Override
                            public void run() {
                                if (initialLocation.getX() == player.getLocation().getX() && initialLocation.getY() == player.getLocation().getY() && initialLocation.getZ() == player.getLocation().getZ()) {

                                    // teleport the player
                                    player.teleport(playersFaction.getFactionHome());

                                } else {
                                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("MovementDetectedTeleportCancelled"));
                                }

                            }
                        }, seconds * 20);

                    } else {
                        player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("FactionHomeNotSetYet"));
                    }
                } else {
                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("MustBeInFaction"));
                }
            } else {
                sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.home"));
            }
        }
    }
}
