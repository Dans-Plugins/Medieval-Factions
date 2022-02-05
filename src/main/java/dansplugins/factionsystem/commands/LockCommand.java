/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.eventhandlers.helper.RelationChecker;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Daniel McCoy Stephenson
 * @author Callum Johnson
 */
public class LockCommand extends SubCommand {

    public LockCommand() {
        super(new String[] {
                "lock", LOCALE_PREFIX + "CmdLock"
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
        final String permission = "mf.lock";
        if (RelationChecker.getInstance().playerNotInFaction(player)) {
            return;
        }
        if (!checkPermissions(player, permission)) {
            return;
        }
        if (args.length >= 1 && safeEquals(args[0], "cancel")) {
            if (ephemeral.getLockingPlayers().remove(player.getUniqueId())) { // Remove them
                player.sendMessage(translate("&c" + getText("LockingCancelled")));
                return;
            }
        }
        ephemeral.getLockingPlayers().add(player.getUniqueId());
        ephemeral.getUnlockingPlayers().remove(player.getUniqueId());
        player.sendMessage(translate("&a" + getText("RightClickLock")));
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