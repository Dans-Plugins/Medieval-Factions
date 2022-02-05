/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.objects.domain.ClaimedChunk;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Callum Johnson
 */
public class SetHomeCommand extends SubCommand {

    public SetHomeCommand() {
        super(new String[]{
                "sethome", "sh", LOCALE_PREFIX + "CmdSetHome"
        }, true, true, true, false);
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
        final String permission = "mf.sethome";
        if (!(checkPermissions(player, permission))) return;
        if (!chunks.isClaimed(player.getLocation().getChunk())) {
            player.sendMessage(translate("&c" + getText("LandIsNotClaimed")));
            return;
        }
        ClaimedChunk chunk = chunks.getClaimedChunk(player.getLocation().getChunk());
        if (chunk == null || !chunk.getHolder().equalsIgnoreCase(faction.getName())) {
            player.sendMessage(translate("&c" + getText("CannotSetFactionHomeInWilderness")));
            return;
        }
        faction.setFactionHome(player.getLocation());
        player.sendMessage(translate("&a" + getText("FactionHomeSet")));
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