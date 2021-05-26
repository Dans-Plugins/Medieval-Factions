package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.utils.UUIDChecker;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RevokeAccessCommand extends SubCommand {

    public RevokeAccessCommand() {
        super(new String[] {
                "ra", "revokeaccess", LOCALE_PREFIX + "CmdRevokeAccess"
        }, true);
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
        final String permission = "mf.revokeaccess";
        if (!(checkPermissions(player, permission))) return;
        if (args.length == 0) {
            player.sendMessage(translate("&c" + getText("UsageRevokeAccess")));
            return;
        }
        if (args[0].equalsIgnoreCase("cancel")) {
            ephemeral.getPlayersRevokingAccess().remove(player.getUniqueId());
            player.sendMessage(translate("&a" + getText("Cancelled")));
            return;
        }
        if (ephemeral.getPlayersRevokingAccess().containsKey(player.getUniqueId())) {
            player.sendMessage(translate("&c" + getText("AlreadyEnteredRevokeAccess")));
            return;
        }
        ephemeral.getPlayersRevokingAccess().put(
                player.getUniqueId(), UUIDChecker.getInstance().findUUIDBasedOnPlayerName(args[0])
        );
        player.sendMessage(translate("&a" + getText("RightClickRevokeAccess")));
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
