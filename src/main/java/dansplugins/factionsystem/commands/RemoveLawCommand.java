/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Daniel McCoy Stephenson
 * @author Callum Johnson
 */public class RemoveLawCommand extends SubCommand {

    public RemoveLawCommand() {
        super(new String[] {
                "removelaw", LOCALE_PREFIX + "CmdRemoveLaw"
        }, true, true, false, true);
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
        final String permission = "mf.removelaw";
        if (!(checkPermissions(player, permission))) return;
        if (args.length == 0) {
            player.sendMessage(translate("&c" + getText("UsageRemoveLaw")));
            return;
        }
        final int lawToRemove = getIntSafe(args[0], 0) - 1;
        if (lawToRemove < 0) {
            player.sendMessage(translate("&c" + getText("UsageRemoveLaw")));
            return;
        }
        if (faction.removeLaw(lawToRemove)) player.sendMessage(translate("&a" + getText("LawRemoved")));
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