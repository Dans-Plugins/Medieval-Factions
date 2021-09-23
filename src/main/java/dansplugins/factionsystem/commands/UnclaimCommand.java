package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UnclaimCommand extends SubCommand {

    public UnclaimCommand() {
        super(new String[] {
                "unclaim", LOCALE_PREFIX + "CmdUnclaim"
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
        final String permission = "mf.unclaim";
        if (!(checkPermissions(player, permission))) return;
        final boolean isPlayerBypassing = EphemeralData.getInstance().getAdminsBypassingProtections().contains(player.getUniqueId());
        if ((boolean) faction.getFlags().getFlag("mustBeOfficerToManageLand")) {
            // officer or owner rank required
            if (!faction.isOfficer(player.getUniqueId()) && !faction.isOwner(player.getUniqueId()) && !isPlayerBypassing) {
                player.sendMessage(translate("&c" + getText("AlertMustBeOfficerOrOwnerToClaimLand")));
                return;
            }
        }
        chunks.removeChunkAtPlayerLocation(player, faction);
        dynmap.updateClaims();
        // TODO: add locale message
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
