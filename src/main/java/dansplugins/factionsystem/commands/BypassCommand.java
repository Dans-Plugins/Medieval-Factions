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
public class BypassCommand extends SubCommand {

    /**
     * Constructor to initialise a Command.
     */
    public BypassCommand() {
        super(new String[]{
                "bypass", "Locale_CmdBypass"
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
        if (!checkPermissions(player, "mf.bypass", "mf.admin")) {
            return;
        }

        final boolean contains = ephemeral.getAdminsBypassingProtections().contains(player.getUniqueId());

        final String path = (contains ? "NoLonger" : "Now") + "BypassingProtections";

        if (contains) {
            ephemeral.getAdminsBypassingProtections().remove(player.getUniqueId());
        } else {
            ephemeral.getAdminsBypassingProtections().add(player.getUniqueId());
        }

        player.sendMessage(translate("&a" + getText(path)));
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