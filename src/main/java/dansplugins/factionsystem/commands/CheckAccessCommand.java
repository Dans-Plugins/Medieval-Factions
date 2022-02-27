/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dansplugins.factionsystem.commands.abs.SubCommand;

/**
 * @author Callum Johnson
 */
public class CheckAccessCommand extends SubCommand {

    public CheckAccessCommand() {
        super(new String[]{
                "ca", "checkaccess", LOCALE_PREFIX + "CmdCheckAccess"
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
        final String permission = "mf.checkaccess";
        if (!(checkPermissions(player, permission))) {
            return;
        }

        boolean cancel = false, contains = ephemeral.getPlayersCheckingAccess().contains(player.getUniqueId());

        if (args.length >= 1) {
            cancel = args[0].equalsIgnoreCase("cancel");
        }

        if (cancel && contains) {
            ephemeral.getPlayersCheckingAccess().remove(player.getUniqueId());
            player.sendMessage(translate("&c" + getText("Cancelled")));
        } else {
            if (contains) {
                player.sendMessage(translate("&c" + getText("AlreadyEnteredCheckAccess")));
            } else {
                ephemeral.getPlayersCheckingAccess().add(player.getUniqueId());
                player.sendMessage(translate("&a" + getText("RightClickCheckAccess")));
            }
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