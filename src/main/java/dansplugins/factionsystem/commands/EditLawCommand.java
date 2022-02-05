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
public class EditLawCommand extends SubCommand {

    public EditLawCommand() {
        super(new String[]{
                "EditLaw", "EL", LOCALE_PREFIX + "CmdEditLaw"
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
        final String permission = "mf.editlaw";
        if (!(checkPermissions(player, permission))) return;
        final int lawToEdit = getIntSafe(args[0], 0) - 1;
        if (lawToEdit < 0 || lawToEdit >= faction.getLaws().size()) {
            player.sendMessage(translate("&c" + getText("UsageEditLaw")));
            return;
        }
        String[] arguments = new String[args.length - 1];
        System.arraycopy(args, 1, arguments, 0, arguments.length);
        final String editedLaw = String.join(" ", arguments);
        if (faction.editLaw(lawToEdit, editedLaw)) {
            player.sendMessage(translate("&a" + getText("LawEdited")));
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