/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import preponderous.ponder.minecraft.bukkit.tools.UUIDChecker;

import java.util.UUID;

/**
 * @author Daniel McCoy Stephenson
 * @author Callum Johnson
 */
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
        UUIDChecker uuidChecker = new UUIDChecker();
        final UUID targetUUID = uuidChecker.findUUIDBasedOnPlayerName(args[0]);
        if (targetUUID == null) {
            player.sendMessage(translate("&c" + getText("PlayerNotFound")));
            return;
        }
        if (targetUUID == player.getUniqueId()) {
            player.sendMessage(translate("&c" + getText("CannotRevokeAccessFromSelf")));
            return;
        }
        ephemeral.getPlayersRevokingAccess().put(
                player.getUniqueId(), targetUUID
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