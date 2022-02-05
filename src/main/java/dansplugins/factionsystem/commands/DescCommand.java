/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Callum Johnson
 */
public class DescCommand extends SubCommand {

    public DescCommand() {
        super(new String[]{
                "Desc", "Description", LOCALE_PREFIX + "CmdDesc"
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
        final String permission = "mf.desc";
        if (!(checkPermissions(player, permission))) {
            return;
        }

        if (args.length == 0) {
            player.sendMessage(translate("&c" + getText("UsageDesc")));
            return;
        }

        faction.setDescription(String.join(" ", args));

        player.sendMessage(translate("&b" + getText("DescriptionSet")));
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