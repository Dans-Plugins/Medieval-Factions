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
public class AutoClaimCommand extends SubCommand {

    /**
     * Constructor to initialise a Command.
     */
    public AutoClaimCommand() {
        super(new String[] {
                "AC", "AUTOCLAIM", LOCALE_PREFIX + "CmdAutoClaim"
        }, true, true, false, true);
    }

    /**
     * Method to execute the command.
     *
     * @param player who sent the command.
     * @param args   of the command.
     * @param key    of the command.
     */
    @Override
    public void execute(Player player, String[] args, String key) {
        final String permission = "mf.autoclaim";
        if (!checkPermissions(player, permission)) {
            return;
        }

        faction.toggleAutoClaim();
        player.sendMessage(translate("&b" + getText("AutoclaimToggled")));
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