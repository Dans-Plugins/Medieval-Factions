package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.objects.Faction;
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
        final Faction playersFaction = getPlayerFaction(player.getUniqueId());
        if (playersFaction.getFlags().getFlag("officerRankRequiredToManageLand")) {
            // officer or owner rank required
            if (!playersFaction.isOfficer(player.getUniqueId()) && !playersFaction.isOwner(player.getUniqueId()) && !isPlayerBypassing) {
                player.sendMessage(translate("&c" + getText("AlertMustBeOfficerOrOwnerToClaimLand")));
                return;
            }
        }
        chunks.removeChunkAtPlayerLocation(player, playersFaction);
        dynmap.updateClaims();
        // TODO: 12/05/2021 Locale Message.
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
