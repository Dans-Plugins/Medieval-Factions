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
 */
public class AddLawCommand extends SubCommand {

    /**
     * Constructor to initialise a Command.
     */
    public AddLawCommand() {
        super(new String[] {
               LOCALE_PREFIX + "CMDAddLaw", "AL", "addlaw"
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
        final String permission = "mf.addlaw";
        if (!(checkPermissions(player, permission))) {
            return;
        }

        // check if they have provided any strings beyond "addlaw"
        if (args.length == 0) {
            player.sendMessage(translate("&c" + getText("UsageAddLaw")));
            return;
        }

        // add the law and send a success message.
        faction.addLaw(String.join(" ", args));
        player.sendMessage(translate("&a" + getText("LawAdded")));
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