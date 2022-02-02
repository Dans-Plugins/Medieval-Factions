package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LineClaimCommand extends SubCommand {
    /**
     * Constructor to initialise a Command.
     */
    public LineClaimCommand() {
        super(new String[] {
                "LC", "LINECLAIM", LOCALE_PREFIX + "CmdLineClaim"
        }, true, true, true, true);
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
        final String permission = "mf.lineclaim";
        if (!checkPermissions(player, permission)) {
            return;
        }
        if ((boolean) faction.getFlags().getFlag("mustBeOfficerToManageLand")) {
            // officer or owner rank required
            if (!faction.isOfficer(player.getUniqueId()) && !faction.isOwner(player.getUniqueId())) {
                player.sendMessage(translate("&c" + getText("AlertMustBeOfficerOrOwnerToClaimLand")));
                return;
            }
        }

        BlockFace blockFace;


        if (args.length != 0) {
            int length = getIntSafe(args[0], -1);

            if (length <= 0) {
                player.sendMessage(translate("&c" + getText("UsageClaimLength")));
            }
            else {
                chunks.lineClaimAtLocation(length, player, player.getLocation(), faction);
            }
        }
        else {
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
