package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.fiefs.utils.UUIDChecker;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * @author Daniel McCoy Stephenson
 * @author Callum Johnson
 */
public class GrantAccessCommand extends SubCommand {

    public GrantAccessCommand() {
        super(new String[] {
                "grantaccess", "ga", LOCALE_PREFIX + "CmdGrantAccess"
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
        if (args.length == 0) {
            player.sendMessage(translate("&c" + getText("UsageGrantAccess")));
            return;
        }
        if (args[0].equalsIgnoreCase("cancel")) {
            player.sendMessage(translate("&c" + getText("CommandCancelled")));
            return;
        }
        if (ephemeral.getPlayersGrantingAccess().containsKey(player.getUniqueId())) {
            player.sendMessage(translate("&c" + getText("AlertAlreadyGrantingAccess")));
            return;
        }
        UUIDChecker uuidChecker = new UUIDChecker();
        final UUID targetUUID = uuidChecker.findUUIDBasedOnPlayerName(args[0]);
        if (targetUUID == null) {
            player.sendMessage(translate("&c" + getText("PlayerNotFound")));
            return;
        }
        if (targetUUID == player.getUniqueId()) {
            player.sendMessage(translate("&c" + getText("CannotGrantAccessToSelf")));
            return;
        }
        ephemeral.getPlayersGrantingAccess().put(player.getUniqueId(), targetUUID);
        player.sendMessage(translate("&a" + getText("RightClickGrantAccess", args[0])));
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