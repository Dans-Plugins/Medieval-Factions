/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Daniel McCoy Stephenson
 * @author Callum Johnson
 */
public class CheckClaimCommand extends SubCommand {

    public CheckClaimCommand() {
        super(new String[]{"checkclaim", "cc", LOCALE_PREFIX + "CmdCheckClaim"}, true);
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
        final String permission = "mf.checkclaim";
        if (!(checkPermissions(player, permission))) {
            return;
        }

        final String result = chunks.checkOwnershipAtPlayerLocation(player);

        if (result.equals("unclaimed")) {
            player.sendMessage(translate("&a" + getText("LandIsUnclaimed")));
        }
        else {
            player.sendMessage(translate("&c" + getText("LandClaimedBy", result)));
        }
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