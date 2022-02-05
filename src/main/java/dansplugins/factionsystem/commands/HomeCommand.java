/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.objects.domain.ClaimedChunk;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Callum Johnson
 */
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
        if (!chunks.isClaimed(home_chunk = faction.getFactionHome().getChunk())) {
            player.sendMessage(translate("&c" + getText("HomeIsInUnclaimedChunk")));
            return;
        }
        ClaimedChunk chunk = chunks.getClaimedChunk(home_chunk);
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
}