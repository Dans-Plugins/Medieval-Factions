/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Callum Johnson
 */
public class ClaimCommand extends SubCommand {

    public ClaimCommand() {
        super(new String[]{
                "Claim", LOCALE_PREFIX + "CmdClaim"
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
        if ((boolean) faction.getFlags().getFlag("mustBeOfficerToManageLand")) {
            // officer or owner rank required
            if (!faction.isOfficer(player.getUniqueId()) && !faction.isOwner(player.getUniqueId())) {
                player.sendMessage(translate("&c" + getText("AlertMustBeOfficerOrOwnerToClaimLand")));
                return;
            }
        }

        if (args.length != 0) {
            int depth = getIntSafe(args[0], -1);

            if (depth <= 0) {
                player.sendMessage(translate("&c" + getText("UsageClaimRadius")));
            } else {
                chunks.radiusClaimAtLocation(depth, player, player.getLocation(), faction);
            }
        } else {
            chunks.claimChunkAtLocation(player, player.getLocation(), faction);
        }
        dynmap.updateClaims();
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