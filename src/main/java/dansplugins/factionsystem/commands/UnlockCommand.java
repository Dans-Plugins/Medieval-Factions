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
public class UnlockCommand extends SubCommand {

    public UnlockCommand() {
        super(new String[]{"unlock", LOCALE_PREFIX + "CmdUnlock"}, true);
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
        final String permission = "mf.unlock";
        if (RelationChecker.getInstance().playerNotInFaction(player)) {
            return;
        }
        if (!checkPermissions(player, permission)) {
            return;
        }
        if (args.length != 0 && args[0].equalsIgnoreCase("cancel")) {
            ephemeral.getUnlockingPlayers().remove(player.getUniqueId());
            ephemeral.getForcefullyUnlockingPlayers().remove(player.getUniqueId()); // just in case the player tries to cancel a forceful unlock without using the force command
            player.sendMessage(translate("&c" + getText("AlertUnlockingCancelled")));
            return;
        }
        if (!ephemeral.getUnlockingPlayers().contains(player.getUniqueId())) {
            ephemeral.getUnlockingPlayers().add(player.getUniqueId());
        }
        ephemeral.getLockingPlayers().remove(player.getUniqueId());

        // inform them they need to right click the block that they want to lock or type /mf lock cancel to cancel it
        player.sendMessage(translate("&a" + getText("RightClickUnlock")));
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